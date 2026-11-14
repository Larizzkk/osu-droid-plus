package com.osudroid.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.osudroid.mods.LegacyModConverter
import com.osudroid.mods.ModDifficultyAdjust
import com.osudroid.mods.ModReplayV6
import com.osudroid.scoring.LegacyScoreMultiplierCalculator
import com.osudroid.utils.ModHashMap
import com.osudroid.utils.mainThread
import com.reco1l.osu.ui.MessageDialog
import com.reco1l.framework.iterator
import org.apache.commons.io.FilenameUtils
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.helper.sql.DBOpenHelper
import ru.nsu.ccfit.zuev.osuplusplus.BuildConfig
import java.io.File
import java.io.IOException
import java.io.ObjectInputStream
import kotlin.math.roundToLong
import ru.nsu.ccfit.zuev.osu.scoring.Replay


// Ported from rimu! project

/**
 * The current schema version of [DroidDatabase].
 */
const val DATABASE_VERSION = 5

/**
 * The osu!droid database manager.
 */
object DatabaseManager {

    /**
     * Get the beatmaps table DAO.
     */
    @JvmStatic
    val beatmapInfoTable
        get() = database.getBeatmapInfoTable()

    /**
     * Get the beatmap options table DAO.
     */
    @JvmStatic
    val beatmapOptionsTable
        get() = database.getBeatmapOptionsTable()

    /**
     * Get the beatmap collections table DAO.
     */
    @JvmStatic
    val beatmapCollectionsTable
        get() = database.getBeatmapCollectionsTable()

    /**
     * Get the score table DAO.
     */
    @JvmStatic
    val scoreInfoTable
        get() = database.getScoreInfoTable()

    /**
     * Get the block area table DAO.
     */
    @JvmStatic
    val blockAreaTable
        get() = database.getBlockAreaTable()

    /**
     * Get the mod preset table DAO.
     */
    @JvmStatic
    val modPresetTable
        get() = database.getModPresetTable()

    /**
     * The path to the database file.
     */
    @JvmStatic
    val databasePath: String
        get() = "${Config.getCorePath()}databases/room-${BuildConfig.BUILD_TYPE}.db"

    private lateinit var database: DroidDatabase

    /**
     * Room databases written by the legacy osu!droid+ client. Its file name was
     * `room-plus-<build type>.db` (release/debug/pre-release) or
     * `plus-release.db`; the `-wal`/`-shm` sidecars belong to the same file.
     */
    private val upstreamDatabaseNames = arrayOf(
        "room-plus-${BuildConfig.BUILD_TYPE}.db",
        "room-plus-release.db",
        "room-plus-debug.db",
        "room-plus-pre-release.db",
        "room-plus-prerelease.db",
        "room-plus.db",
        "plus-release.db",
        "plus-debug.db",
        "plus-pre-release.db",
        "client.db",
    )

    /**
     * `true` if the database was created by a newer, incompatible version of the app and had to be reset.
     *
     * When this happens, the previous database file is kept as a backup alongside the new one.
     */
    @JvmStatic
    var wasResetDueToDowngrade = false
        private set


    @JvmStatic
    fun load(context: Context) {

        wasResetDueToDowngrade = backUpDatabaseIfDowngraded()

        // Be careful when changing the database name, it may cause data loss.
        database = Room.databaseBuilder(context, DroidDatabase::class.java, databasePath)
            .addMigrations(*ALL_MIGRATIONS)
            // No downgrade path is maintained for the database, so if it was created by a newer version of
            // the game, it will be reset. backUpDatabaseIfDowngraded() keeps a copy of it beforehand.
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .allowMainThreadQueries()
            .build()

        if (!BuildConfig.DEBUG) {
            loadLegacyMigrations(context)
        }

        // Legacy osu!droid+ stored its library in room-plus-*.db. A fresh
        // install re-imports everything; otherwise only changed files are read.
        try {
            val hasLibrary = try {
                beatmapInfoTable.getBeatmapSetList().isNotEmpty()
            } catch (_: Exception) {
                false
            }
            importFromUpstreamDatabase(loadLibrary = false, force = !hasLibrary)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Automatic upstream import failed", e)
        }
    }

    /**
     * Imports beatmaps, options, collections and scores from a database left by
     * the legacy osu!droid client or an older osu!droid+ build.
     *
     * Tables are copied by the intersection of their columns, so databases from
     * different schema versions can be read without a destructive migration.
     * The upstream file is never modified.
     *
     * @param loadLibrary whether to reload the in-memory library after importing.
     * @param force import even when the legacy files were already imported before.
     * @return the number of beatmap sets in this database, or -1 when no legacy file was imported.
     */
    @JvmStatic
    @JvmOverloads
    fun importFromUpstreamDatabase(loadLibrary: Boolean = true, force: Boolean = false): Int {
        val dbDir = File(Config.getCorePath(), "databases")
        if (!dbDir.isDirectory) {
            return -1
        }

        val current = try {
            File(databasePath).canonicalFile
        } catch (_: IOException) {
            File(databasePath).absoluteFile
        }

        val candidates = (upstreamDatabaseNames.map { File(dbDir, it) } + dbDir.listFiles { file ->
            file.isFile && file.name.endsWith(".db") &&
                (file.name.startsWith("room-plus") || file.name.startsWith("plus-"))
        }.orEmpty().sortedBy { it.name })
            .filter { it.isFile && it.length() > 0 }
            .distinctBy {
                try {
                    it.canonicalPath
                } catch (_: IOException) {
                    it.absolutePath
                }
            }
            .filter {
                try {
                    it.canonicalFile != current
                } catch (_: IOException) {
                    it.absoluteFile != current
                }
            }

        val marker = File(dbDir, ".room-plus-imported")
        val previous = try {
            marker.readLines().filter { it.isNotBlank() }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
        val pending = if (force) candidates else candidates.filter { legacySignature(it) !in previous }
        if (pending.isEmpty()) {
            return -1
        }

        val writableDb = database.openHelper.writableDatabase
        var importedAny = false
        val importedSignatures = mutableSetOf<String>()

        for (candidate in pending) {
            // Legacy room-plus databases are in WAL mode. Snapshot the database
            // together with its -wal/-shm/-journal files so committed rows that
            // are still in the WAL are visible to ATTACH.
            val snapshot = if (hasSidecars(candidate)) snapshotDatabase(candidate) else null
            val source = snapshot ?: candidate
            val escapedPath = source.absolutePath.replace("'", "''")

            try {
                try {
                    // Inline the path because ATTACH accepts a filename, not a bind
                    // value on every Android SQLite build.
                    writableDb.execSQL("ATTACH DATABASE '$escapedPath' AS upstream")
                } catch (e: Exception) {
                    Log.w("DatabaseManager", "Cannot attach legacy database $candidate", e)
                    continue
                }

                try {
                    if (!hasTable(writableDb, "BeatmapInfo")) {
                        continue
                    }
                    copySharedColumns(writableDb, "BeatmapInfo")
                    copySharedColumns(writableDb, "BeatmapOptions")
                    copySharedColumns(writableDb, "BeatmapSetCollection")
                    copySharedColumns(writableDb, "BeatmapSetCollection_BeatmapSetInfo")
                    copySharedColumns(writableDb, "BlockArea")
                    copySharedColumns(writableDb, "ModPreset")
                    copySharedColumns(writableDb, "ScoreInfo")
                    importedAny = true
                    importedSignatures += legacySignature(candidate)
                    Log.i("DatabaseManager", "Imported legacy library from ${candidate.absolutePath}")
                } catch (e: Exception) {
                    Log.e("DatabaseManager", "Failed to import legacy database $candidate", e)
                } finally {
                    try {
                        writableDb.execSQL("DETACH DATABASE upstream")
                    } catch (e: Exception) {
                        Log.w("DatabaseManager", "Failed to detach legacy database", e)
                    }
                }
            } finally {
                snapshot?.let(::deleteSnapshot)
            }
        }

        if (!importedAny) {
            return -1
        }

        try {
            marker.writeText((previous + importedSignatures).joinToString("\n"))
        } catch (e: Exception) {
            Log.w("DatabaseManager", "Could not write legacy import marker", e)
        }

        if (loadLibrary) {
            LibraryManager.loadLibrary()
        }
        return beatmapInfoTable.getBeatmapSetList().size
    }

    private fun legacySignature(file: File): String =
        arrayOf("", "-wal", "-shm", "-journal").joinToString("|") { suffix ->
            val part = if (suffix.isEmpty()) file else File(file.parentFile, file.name + suffix)
            "${part.name}:${part.length()}:${part.lastModified()}"
        }

    private fun hasSidecars(file: File): Boolean =
        sequenceOf("-wal", "-shm", "-journal")
            .map { File(file.parentFile, file.name + it) }
            .any { it.isFile }

    /** Copies a WAL database and its sidecars to a temporary, consistent snapshot. */
    private fun snapshotDatabase(source: File): File? {
        var snapshot: File? = null
        try {
            snapshot = File.createTempFile("legacy-import-", "-${source.name}", source.parentFile)
            source.copyTo(snapshot, overwrite = true)

            for (suffix in arrayOf("-wal", "-shm", "-journal")) {
                val sidecar = File(source.parentFile, source.name + suffix)
                if (sidecar.isFile) {
                    sidecar.copyTo(File(snapshot.parentFile, snapshot.name + suffix), overwrite = true)
                }
            }
            return snapshot
        } catch (e: Exception) {
            Log.w("DatabaseManager", "Could not snapshot legacy database $source", e)
            snapshot?.let(::deleteSnapshot)
            return null
        }
    }

    private fun deleteSnapshot(snapshot: File) {
        for (suffix in arrayOf("", "-wal", "-shm", "-journal")) {
            try {
                File(snapshot.parentFile, snapshot.name + suffix).delete()
            } catch (_: Exception) {
                // Best-effort cleanup.
            }
        }
    }

    private data class ColumnInfo(
        val name: String,
        val type: String,
        val notNull: Boolean,
        val defaultValue: String?,
        val primaryKey: Int,
    )

    /** Column names and constraints of a table in the main or attached database. */
    private fun tableColumns(
        db: SupportSQLiteDatabase,
        schema: String,
        table: String,
    ): Map<String, ColumnInfo> {
        val columns = linkedMapOf<String, ColumnInfo>()
        db.query("PRAGMA $schema.table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            val notNullIndex = cursor.getColumnIndexOrThrow("notnull")
            val defaultIndex = cursor.getColumnIndexOrThrow("dflt_value")
            val primaryKeyIndex = cursor.getColumnIndexOrThrow("pk")

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex)
                columns[name] = ColumnInfo(
                    name = name,
                    type = cursor.getString(typeIndex).orEmpty(),
                    notNull = cursor.getInt(notNullIndex) != 0,
                    defaultValue = cursor.getString(defaultIndex),
                    primaryKey = cursor.getInt(primaryKeyIndex),
                )
            }
        }
        return columns
    }

    private fun hasTable(db: SupportSQLiteDatabase, table: String): Boolean =
        db.query(
            "SELECT 1 FROM upstream.sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
            arrayOf(table),
        ).use { it.moveToFirst() }

    /**
     * Copies only columns present in both schemas. Newly added NOT NULL columns
     * without a SQL default receive a sensible value (older Room scores are
     * flagged for score migration, new beatmap flags default to false).
     */
    private fun copySharedColumns(db: SupportSQLiteDatabase, table: String) {
        val destination = tableColumns(db, "main", table)
        val source = tableColumns(db, "upstream", table)
        if (destination.isEmpty() || source.isEmpty()) {
            return
        }

        // A primary key cannot be generated for rows copied from an old table.
        if (destination.values.any { it.primaryKey > 0 && it.name !in source }) {
            Log.w("DatabaseManager", "Skipping $table: source has no primary key")
            return
        }

        val targetColumns = mutableListOf<String>()
        val expressions = mutableListOf<String>()

        for (column in destination.values) {
            if (column.name in source) {
                targetColumns += "`${column.name}`"
                expressions += "`${column.name}`"
            } else if (column.notNull && column.defaultValue == null) {
                targetColumns += "`${column.name}`"
                expressions += missingColumnValue(table, column)
            }
        }

        if (targetColumns.isEmpty()) {
            return
        }

        db.execSQL(
            "INSERT OR IGNORE INTO `$table` (${targetColumns.joinToString(", ")}) " +
                "SELECT ${expressions.joinToString(", ")} FROM upstream.`$table`"
        )
    }

    private fun missingColumnValue(table: String, column: ColumnInfo): String {
        if (table == "ScoreInfo" && column.name == "needsScoreMigration") {
            // Scores from schemas before v5 still contain the multiplied score.
            return "1"
        }
        return when {
            column.type.contains("INT", ignoreCase = true) -> "0"
            column.type.contains("REAL", ignoreCase = true) ||
                column.type.contains("FLOA", ignoreCase = true) ||
                column.type.contains("DOUB", ignoreCase = true) -> "0.0"
            column.type.contains("BLOB", ignoreCase = true) -> "X''"
            else -> "''"
        }
    }

    /**
     * Shows a dialog notifying the user that their local database was reset, if applicable.
     *
     * Must be called after the main scene has finished loading.
     */
    @JvmStatic
    fun notifyIfResetDueToDowngrade() {
        if (!wasResetDueToDowngrade) {
            return
        }

        mainThread {
            MessageDialog()
                .setTitle("Database reset")
                .setMessage(
                    "Your local database was created by a newer version of the game and could not be read, " +
                        "so it was reset. This means locally stored scores, favorites, and collections were " +
                        "lost.\n\nA backup of the previous database was saved."
                )
                .addButton("OK", clickListener = MessageDialog::dismiss)
                .show()
        }
    }

    /**
     * Backs up the current database file if it was created by a version of the game newer than this one,
     * since Room cannot open it and will destructively reset it.
     *
     * @return `true` if the database was backed up, `false` otherwise.
     */
    private fun backUpDatabaseIfDowngraded(): Boolean {
        val dbFile = File(databasePath)

        if (!dbFile.exists()) {
            return false
        }

        val fileVersion = try {
            SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Failed to read database version prior to opening", e)
            return false
        }

        if (fileVersion <= DATABASE_VERSION) {
            return false
        }

        Log.w(
            "DatabaseManager",
            "Database was created by a newer version of the game (file version $fileVersion, game version " +
                "$DATABASE_VERSION) and will be reset. The previous database will be backed up."
        )

        try {
            // The WAL file may hold committed data that hasn't been written back to the main database file
            // yet, so it needs to be checkpointed before the file is copied.
            SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE).use {
                it.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { cursor -> cursor.moveToFirst() }
            }

            dbFile.copyTo(File(dbFile.parentFile, "${dbFile.name}.v$fileVersion.bak"), true)
        } catch (e: IOException) {
            Log.e("DatabaseManager", "Failed to back up database before reset", e)
        }

        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadLegacyMigrations(context: Context) {

        // BeatmapOptions
        try {
            val oldPropertiesFile = File(context.filesDir, "properties")

            if (oldPropertiesFile.exists()) {
                GlobalManager.getInstance().info = "Migrating beatmap properties..."

                oldPropertiesFile.inputStream().use { fis ->

                    ObjectInputStream(fis).use { ois ->

                        val beatmapOptions = mutableListOf<BeatmapOptions>()

                        // Ignoring first object which is intended to be the version.
                        ois.readObject()

                        for ((path, properties) in ois.readObject() as Map<String, BeatmapProperties>) {
                            beatmapOptions += BeatmapOptions(
                                setDirectory = FilenameUtils.getName(FilenameUtils.normalizeNoEndSeparator(path)),
                                isFavorite = properties.favorite,
                                // Offset is flipped in version 1.8, so we need to negate it.
                                offset = -properties.offset
                            )
                        }

                        beatmapOptionsTable.insertAll(beatmapOptions)
                    }
                }

                oldPropertiesFile.renameTo(File(context.filesDir, "properties_old"))
            }

        } catch (e: IOException) {
            Log.e("DatabaseManager", "Failed to migrate legacy beatmap properties", e)
        }

        // BeatmapCollections
        try {
            val oldFavoritesFile = File(Config.getCorePath(), "json/favorite.json")

            if (oldFavoritesFile.exists()) {
                GlobalManager.getInstance().info = "Migrating beatmap collections..."

                val json = JSONObject(oldFavoritesFile.readText())

                for (collectionName in json.keys()) {
                    beatmapCollectionsTable.insertCollection(collectionName)

                    for (beatmapPath in json.getJSONArray(collectionName)) {

                        beatmapCollectionsTable.addBeatmap(
                            collectionName = collectionName,
                            setDirectory = FilenameUtils.getName(FilenameUtils.normalizeNoEndSeparator(beatmapPath.toString()))
                        )
                    }
                }

                oldFavoritesFile.renameTo(File(Config.getCorePath(), "json/favorite_old.json"))
            }

        } catch (e: Exception) {
            Log.e("DatabaseManager", "Failed to migrate legacy beatmap collections", e)
        }

        // ScoreInfo
        try {
            val oldDatabaseFile = File(Config.getCorePath(), "databases/osudroid_test.db")

            if (oldDatabaseFile.exists()) {
                GlobalManager.getInstance().info = "Migrating score table..."

                DBOpenHelper.getOrCreate(context).writableDatabase.use { db ->

                    db.rawQuery("SELECT * FROM scores", null).use {

                        var pendingScores = it.count
                        val scoreMultiplierCalculator = LegacyScoreMultiplierCalculator()

                        val scoreInfos = mutableListOf<ScoreInfo>()
                        while (it.moveToNext()) {

                            try {
                                val id = it.getInt(it.getColumnIndexOrThrow("id")).toLong()

                                if (scoreInfoTable.scoreExists(id)) {
                                    pendingScores--
                                    continue
                                }

                                val replayFilePath = it.getString(it.getColumnIndexOrThrow("replayfile"))
                                val replay = Replay()

                                if (!replay.load(replayFilePath, false)) {
                                    Log.e("ScoreLibrary", "Failed to import score from old database. Replay file not found.")
                                    pendingScores--
                                    continue
                                }

                                val legacyModString = it.getString(it.getColumnIndexOrThrow("mode"))

                                val legacyMods = try {
                                    LegacyModConverter.convert(legacyModString)
                                } catch (_: Exception) {
                                    ModHashMap()
                                }.apply { put(ModReplayV6()) }

                                val legacyScore = it.getInt(it.getColumnIndexOrThrow("score")).toLong()
                                val needsMigration = legacyMods.values.any { mod -> mod is ModDifficultyAdjust }

                                val rawScore =
                                    if (needsMigration) legacyScore
                                    else (legacyScore / scoreMultiplierCalculator.calculateFor(legacyMods.values)).roundToLong()

                                scoreInfos += ScoreInfo(
                                    id = id,
                                    beatmapMD5 = replay.md5,
                                    playerName = it.getString(it.getColumnIndexOrThrow("playername")),
                                    replayFilename = FilenameUtils.getName(replayFilePath),
                                    mods = legacyMods.serializeMods(),
                                    score = rawScore,
                                    maxCombo = it.getInt(it.getColumnIndexOrThrow("combo")),
                                    mark = it.getString(it.getColumnIndexOrThrow("mark")),
                                    hit300k = it.getInt(it.getColumnIndexOrThrow("h300k")),
                                    hit300 = it.getInt(it.getColumnIndexOrThrow("h300")),
                                    hit100k = it.getInt(it.getColumnIndexOrThrow("h100k")),
                                    hit100 = it.getInt(it.getColumnIndexOrThrow("h100")),
                                    hit50 = it.getInt(it.getColumnIndexOrThrow("h50")),
                                    misses = it.getInt(it.getColumnIndexOrThrow("misses")),
                                    time = it.getLong(it.getColumnIndexOrThrow("time")),
                                    sliderHeadHits = null,
                                    sliderTickHits = null,
                                    sliderRepeatHits = null,
                                    sliderEndHits = null,
                                    needsScoreMigration = needsMigration
                                )

                                pendingScores--

                            } catch (e: Exception) {
                                Log.e("ScoreLibrary", "Failed to import score from old database.", e)
                            }
                        }

                        scoreInfoTable.insertScores(scoreInfos)

                        if (pendingScores <= 0) {
                            oldDatabaseFile.renameTo(File(Config.getCorePath(), "databases/osudroid_old.db"))
                        }
                    }

                }

            }

        } catch (e: IOException) {
            Log.e("DatabaseManager", "Failed to migrate legacy score table", e)
        }

    }

}

@Database(
    version = DATABASE_VERSION,
    entities = [
        BeatmapInfo::class,
        BeatmapOptions::class,
        ScoreInfo::class,
        BeatmapSetCollection::class,
        BeatmapSetCollection_BeatmapSetInfo::class,
        BlockArea::class,
        ModPreset::class
    ]
)
abstract class DroidDatabase : RoomDatabase() {

    abstract fun getBeatmapInfoTable(): IBeatmapInfoDAO

    abstract fun getBeatmapOptionsTable(): IBeatmapOptionsDAO

    abstract fun getBeatmapCollectionsTable(): IBeatmapCollectionsDAO

    abstract fun getScoreInfoTable(): IScoreInfoDAO

    abstract fun getBlockAreaTable(): IBlockAreaDAO

    abstract fun getModPresetTable(): IModPresetDAO
}
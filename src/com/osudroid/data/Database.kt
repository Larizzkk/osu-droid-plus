package com.osudroid.data

import android.content.Context
import android.util.Log
import androidx.room.*
import com.reco1l.toolkt.data.iterator
import org.apache.commons.io.FilenameUtils
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.helper.sql.DBOpenHelper
import ru.nsu.ccfit.zuev.osuplusplus.BuildConfig
import ru.nsu.ccfit.zuev.osuplusplus.GlobalManager
import java.io.File
import java.io.IOException
import java.io.ObjectInputStream
import ru.nsu.ccfit.zuev.osu.scoring.Replay


// Ported from rimu! project

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
        get() = "${Config.getCorePath()}databases/room-plus-${BuildConfig.BUILD_TYPE}.db"

    private lateinit var database: DroidDatabase

    /**
     * Imports song folders and beatmaps from the original upstream database file
     * (e.g. room-release.db, room-debug.db, client.db) into this database.
     * Falls back to scanning song directories if no upstream DB file is found.
     *
     * @return Number of beatmap sets after import, or -1 if fallback scanning was used.
     */
    @JvmStatic
    fun importFromUpstreamDatabase(): Int {
        val corePath = Config.getCorePath()
        val dbDir = File(corePath, "databases")
        val candidateNames = arrayOf(
            "room-release.db",
            "room-debug.db",
            "client.db",
            "osudroid.db"
        )

        var upstreamDbFile: File? = null
        val currentPath = databasePath

        if (dbDir.exists() && dbDir.isDirectory) {
            for (name in candidateNames) {
                val candidate = File(dbDir, name)
                if (candidate.exists() && candidate.length() > 0 && candidate.absolutePath != currentPath) {
                    upstreamDbFile = candidate
                    break
                }
            }
        }

        if (upstreamDbFile == null) {
            LibraryManager.scanDirectory(null)
            LibraryManager.loadLibrary()
            return -1
        }

        return try {
            val writableDb = database.openHelper.writableDatabase
            val escapedPath = upstreamDbFile.absolutePath.replace("'", "''")
            writableDb.execSQL("ATTACH DATABASE '$escapedPath' AS upstream")
            try {
                writableDb.execSQL("INSERT OR IGNORE INTO BeatmapInfo SELECT * FROM upstream.BeatmapInfo")
                try {
                    writableDb.execSQL("INSERT OR IGNORE INTO BeatmapOptions SELECT * FROM upstream.BeatmapOptions")
                } catch (e: Exception) {
                    Log.w("DatabaseManager", "BeatmapOptions import skipped: ${e.message}")
                }
                try {
                    writableDb.execSQL("INSERT OR IGNORE INTO BeatmapSetCollection SELECT * FROM upstream.BeatmapSetCollection")
                    writableDb.execSQL("INSERT OR IGNORE INTO BeatmapSetCollection_BeatmapSetInfo SELECT * FROM upstream.BeatmapSetCollection_BeatmapSetInfo")
                } catch (e: Exception) {
                    Log.w("DatabaseManager", "Collections import skipped: ${e.message}")
                }
            } finally {
                writableDb.execSQL("DETACH DATABASE upstream")
            }

            LibraryManager.loadLibrary()
            beatmapInfoTable.getBeatmapSetList().size
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Failed to attach upstream database $upstreamDbFile", e)
            LibraryManager.scanDirectory(null)
            LibraryManager.loadLibrary()
            -1
        }
    }


    @JvmStatic
    fun load(context: Context) {

        // Be careful when changing the database name, it may cause data loss.
        database = Room.databaseBuilder(context, DroidDatabase::class.java, databasePath)
            .addMigrations(*ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        if (!BuildConfig.DEBUG) {
            loadLegacyMigrations(context)
        }
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

                                scoreInfos += ScoreInfo(
                                    id = id,
                                    beatmapMD5 = replay.md5,
                                    playerName = it.getString(it.getColumnIndexOrThrow("playername")),
                                    replayFilename = FilenameUtils.getName(replayFilePath),
                                    mods = it.getString(it.getColumnIndexOrThrow("mode")),
                                    score = it.getInt(it.getColumnIndexOrThrow("score")),
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
                                    sliderEndHits = null
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
    version = 4,
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

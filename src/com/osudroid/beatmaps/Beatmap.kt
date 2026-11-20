package com.osudroid.beatmaps

import com.osudroid.GameMode
import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.Spinner
import com.osudroid.beatmaps.sections.BeatmapColor
import com.osudroid.beatmaps.sections.BeatmapControlPoints
import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.beatmaps.sections.BeatmapEvents
import com.osudroid.beatmaps.sections.BeatmapGeneral
import com.osudroid.beatmaps.sections.BeatmapHitObjects
import com.osudroid.beatmaps.sections.BeatmapMetadata
import com.osudroid.mods.IModApplicableToBeatmap
import com.osudroid.mods.IModApplicableToDifficulty
import com.osudroid.mods.IModApplicableToDifficultyWithMods
import com.osudroid.mods.IModApplicableToHitObject
import com.osudroid.mods.IModApplicableToHitObjectWithMods
import com.osudroid.mods.IModFacilitatesAdjustment
import com.osudroid.mods.Mod
import com.osudroid.mods.ModScoreV2
import com.osudroid.scoring.ScoreMultiplierCalculator
import com.osudroid.utils.ModHashMap
import com.osudroid.utils.applyMods
import kotlin.math.min
import kotlin.math.roundToLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * Represents a beatmap.
 *
 * @param mode The [GameMode] this [Beatmap] was parsed as.
 */
open class Beatmap(mode: GameMode) : IBeatmap, Cloneable {
    /**
     * The [GameMode] this [Beatmap] was parsed as.
     */
    var mode = mode
        private set

    override var formatVersion = 14
    override val general = BeatmapGeneral()
    override val metadata = BeatmapMetadata()
    override var difficulty = BeatmapDifficulty()
    override val events = BeatmapEvents()
    override val colors = BeatmapColor()
    override val controlPoints = BeatmapControlPoints()
    override var hitObjects = BeatmapHitObjects()
    override var filePath = ""
    override var md5 = ""

    override val maxCombo by lazy {
        hitObjects.objects.sumOf {
            if (it is Slider) it.nestedHitObjects.size else 1
        }
    }

    /**
     * Calculates the maximum score of this [Beatmap] with the given [ModHashMap].
     *
     * This does not take spinner bonus into account.
     *
     * @param mods The [ModHashMap] to calculate the maximum score with.
     * @return The maximum score of this [Beatmap] with [mods] applied.
     */
    fun calculateMaximumScore(mods: ModHashMap?): Long {
        val scoreMultiplier = if (mods != null) ScoreMultiplierCalculator(difficulty).calculateFor(mods.values) else 1.0

        if (mods != null && ModScoreV2::class in mods) {
            return (1e6 * scoreMultiplier).toLong()
        }

        // Spinners need non-rate adjusted to calculate required spins.
        val nonRateAdjustedDifficulty = difficulty.clone()

        if (mods != null) {
            nonRateAdjustedDifficulty.applyMods(mode, mods.values)
        }

        // osu!standard scores with the difficulty the player actually sees; mods such as
        // HR and DA therefore increase the combo multiplier as well.
        val difficultyMultiplier = 1 + min(nonRateAdjustedDifficulty.od, 10f) / 10 +
                min(nonRateAdjustedDifficulty.hp, 10f) / 10 +
                (min(nonRateAdjustedDifficulty.gameplayCS, 17.62f) - 3) / 4

        var combo = 0
        var score = 0L

        // osu!standard score formula: HitValue + HitValue * (Combo * DifficultyMultiplier) / 25.
        // Combo is 1-based, and slider heads/ticks/repeats are combo-scored too.
        fun addComboScore(hitValue: Int) {
            combo++
            score += hitValue + (hitValue * combo * difficultyMultiplier / 25).toLong()
        }

        for (obj in hitObjects) {
            when (obj) {
                is Slider -> {
                    addComboScore(30) // head
                    repeat(obj.repeatCount) { addComboScore(30) }
                    repeat(obj.tickCount) { addComboScore(10) }
                    addComboScore(30) // end
                }

                is Spinner -> {
                    // For each required rotation, a spinner tick (100 score) is awarded, but it does not
                    // contribute to combo.
                    val minRps = when (mode) {
                        GameMode.Droid -> 2 + 2 * nonRateAdjustedDifficulty.od / 10.0
                        GameMode.Standard -> BeatmapDifficulty.difficultyRange(
                            nonRateAdjustedDifficulty.od.toDouble(), 90.0, 150.0, 225.0
                        ) / 60.0
                    }

                    val requiredRotations = (minRps * obj.duration / 1000).toInt()

                    repeat(requiredRotations) { score += 100 }
                    addComboScore(300)
                }

                else -> addComboScore(300)
            }
        }

        return (score * scoreMultiplier).roundToLong()
    }

    /**
     * Constructs a [DroidPlayableBeatmap] from this [Beatmap], where all [HitObject] and [BeatmapDifficulty]
     * [Mod]s have been applied, and [HitObject]s have been fully constructed.
     *
     * @param mods The [Mod]s to apply to the [Beatmap]. Defaults to No Mod.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return The [DroidPlayableBeatmap].
     */
    @JvmOverloads
    fun createDroidPlayableBeatmap(
        mods: Iterable<Mod>? = null,
        scope: CoroutineScope? = null
    ) = DroidPlayableBeatmap(convert(GameMode.Droid, mods, scope), mods)

    /**
     * Constructs a [StandardPlayableBeatmap] from this [Beatmap], where all [HitObject] and [BeatmapDifficulty]
     * [Mod]s have been applied, and [HitObject]s have been fully constructed.
     *
     * @param mods The [Mod]s to apply to the [Beatmap]. Defaults to No Mod.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return The [StandardPlayableBeatmap].
     */
    @JvmOverloads
    fun createStandardPlayableBeatmap(
        mods: Iterable<Mod>? = null,
        scope: CoroutineScope? = null
    ) = StandardPlayableBeatmap(convert(GameMode.Standard, mods, scope), mods)

    /**
     * Converts this [Beatmap] to another [Beatmap] for the specified [GameMode], where all [HitObject] and
     * [BeatmapDifficulty] [Mod]s have been applied, and [HitObject]s have been fully constructed.
     *
     * @param mode The [GameMode] to convert the [Beatmap] to.
     * @param mods The [Mod]s to apply to the [Beatmap]. Defaults to No Mod.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return The converted [Beatmap].
     */
    @JvmOverloads
    fun convert(mode: GameMode, mods: Iterable<Mod>? = null, scope: CoroutineScope? = null): Beatmap {
        if (this.mode == mode && mods?.firstOrNull() == null) {
            // Beatmap is already playable as is.
            return this
        }

        val adjustmentMods = mods?.filterIsInstance<IModFacilitatesAdjustment>() ?: emptyList()

        val converter = BeatmapConverter(this, scope)

        // Convert
        val converted = converter.convert()
        converted.mode = mode

        // Apply difficulty mods
        mods?.filterIsInstance<IModApplicableToDifficulty>()?.forEach {
            scope?.ensureActive()
            it.applyToDifficulty(mode, converted.difficulty, adjustmentMods)
        }

        mods?.filterIsInstance<IModApplicableToDifficultyWithMods>()?.forEach {
            scope?.ensureActive()
            it.applyToDifficulty(mode, converted.difficulty, mods)
        }

        val processor = BeatmapProcessor(converted, scope)

        processor.preProcess()

        // Compute default values for hit objects, including creating nested hit objects in-case they're needed
        converted.hitObjects.objects.forEach {
            scope?.ensureActive()
            it.applyDefaults(converted.controlPoints, converted.difficulty, mode, scope)
        }

        mods?.filterIsInstance<IModApplicableToHitObject>()?.forEach {
            for (obj in converted.hitObjects.objects) {
                scope?.ensureActive()
                it.applyToHitObject(mode, obj, adjustmentMods, scope)
            }
        }

        mods?.filterIsInstance<IModApplicableToHitObjectWithMods>()?.forEach {
            for (obj in converted.hitObjects.objects) {
                scope?.ensureActive()
                it.applyToHitObject(mode, obj, mods, scope)
            }
        }

        processor.postProcess()

        mods?.filterIsInstance<IModApplicableToBeatmap>()?.forEach {
            scope?.ensureActive()
            it.applyToBeatmap(converted, scope)
        }

        return converted
    }

    public override fun clone() = super.clone() as Beatmap
}
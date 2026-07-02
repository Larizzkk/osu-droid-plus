package com.rian.osu.mods

import com.reco1l.toolkt.*
import com.rian.osu.GameMode
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.mods.settings.*
import com.rian.osu.utils.ModUtils
import kotlin.math.exp
import kotlin.math.pow
import kotlin.reflect.KProperty0
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * Represents the Difficulty Adjust mod. Serves as a container for forced difficulty statistics.
 */
class ModDifficultyAdjust @JvmOverloads constructor(
    cs: Float? = null,
    ar: Float? = null,
    od: Float? = null,
    hp: Float? = null
) : Mod(), IModApplicableToDifficultyWithMods, IModApplicableToHitObjectWithMods, IModRequiresOriginalBeatmap {

    /**
     * The circle size to enforce.
     */
    var cs by NullableFloatModSetting(
        name = "Circle size",
        key = "cs",
        valueFormatter = { (it ?: defaultValue)?.roundBy(1)?.toString() ?: "None" },
        defaultValue = null,
        minValue = -12.5f,
        maxValue = 11f,
        step = 0.1f,
        precision = 1,
        orderPosition = 0
    )

    /**
     * The approach rate to enforce.
     */
    var ar by NullableFloatModSetting(
        name = "Approach rate",
        key = "ar",
        valueFormatter = { (it ?: defaultValue)?.roundBy(1)?.toString() ?: "None" },
        defaultValue = null,
        minValue = -12.5f,
        maxValue = 12.5f,
        step = 0.1f,
        precision = 1,
        orderPosition = 1
    )

    /**
     * The overall difficulty to enforce.
     */
    var od by NullableFloatModSetting(
        name = "Overall difficulty",
        key = "od",
        valueFormatter = { (it ?: defaultValue)?.roundBy(1)?.toString() ?: "None" },
        defaultValue = null,
        minValue = -12.5f,
        maxValue = 11f,
        step = 0.1f,
        precision = 1,
        orderPosition = 2
    )

    /**
     * The health drain rate to enforce.
     */
    var hp by NullableFloatModSetting(
        name = "Health drain",
        key = "hp",
        valueFormatter = { (it ?: defaultValue)?.roundBy(1)?.toString() ?: "None" },
        defaultValue = null,
        minValue = -12.5f,
        maxValue = 11f,
        step = 0.1f,
        precision = 1,
        orderPosition = 3
    )

    init {
        // We set the default values here so that resetting the settings would reset them to null.
        updateDefaultValue(::cs, cs)
        updateDefaultValue(::ar, ar)
        updateDefaultValue(::od, od)
        updateDefaultValue(::hp, hp)

        this.cs = cs
        this.ar = ar
        this.od = od
        this.hp = hp
    }

    override val name = "Difficulty Adjust"
    override val acronym = "DA"
    override val description = "Override a beatmap's difficulty settings."
    override val type = ModType.Conversion
    override val requiresConfiguration = true
    override val isRanked = true

    // This mod has a different default than others as the default value of settings change based on the beatmap.
    override val usesDefaultSettings
        get() = settings.all { it.value == it.initialValue }

    override val scoreMultiplier: Float
        get() {
            // Balanced multiplier based on deviation from default
            var multiplier = 1f

            for (setting in listOf(::cs, ::ar, ::od, ::hp)) {
                val delegate = getModSettingDelegate<NullableFloatModSetting>(setting)
                if (delegate.value != null && delegate.defaultValue != null) {
                    val diff = delegate.value!! - delegate.defaultValue!!
                    multiplier *= if (diff >= 0) {
                        // Harder: small bonus (max ~1.25x at +12.5)
                        1 + 0.003f * diff.pow(1.3f)
                    } else {
                        // Easier: logistic penalty (min ~0.12x at -12.5)
                        2f / (1f + exp(-0.3f * diff))
                    }
                }
            }
            return multiplier
        }

    override fun isCompatibleWith(other: Mod): Boolean {
        if (!super.isCompatibleWith(other)) {
            return false
        }

        if (other is ModSmallCircle && cs != null) {
            return false
        }

        if (cs != null && ar != null && od != null && hp != null) {
            return other !is ModEasy && other !is ModHardRock && other !is ModReallyEasy
        }

        return true
    }

    override fun applyToDifficulty(mode: GameMode, difficulty: BeatmapDifficulty, mods: Iterable<Mod>) =
        difficulty.let {
            it.difficultyCS = getValue(cs, it.difficultyCS)
            it.gameplayCS = getValue(cs, it.gameplayCS)
            it.ar = getValue(ar, it.ar)
            it.od = getValue(od, it.od)
            it.hp = getValue(hp, it.hp)

            // Special case for force AR in replay version 6 and older, where the AR value is kept constant with respect
            // to game time. This makes the player perceive the AR as is under all speed multipliers.
            if (ar != null && mods.any { m -> m is ModReplayV6 }) {
                val preempt = BeatmapDifficulty.difficultyRange(
                    ar!!.toDouble(),
                    HitObject.PREEMPT_MAX,
                    HitObject.PREEMPT_MID,
                    HitObject.PREEMPT_MIN
                )
                val trackRate = ModUtils.calculateRateWithMods(mods)

                it.ar = BeatmapDifficulty.inverseDifficultyRange(
                    preempt * trackRate,
                    HitObject.PREEMPT_MAX,
                    HitObject.PREEMPT_MID,
                    HitObject.PREEMPT_MIN
                ).toFloat()
            }
        }

    override fun applyToHitObject(mode: GameMode, hitObject: HitObject, mods: Iterable<Mod>, scope: CoroutineScope?) {
        // Special case for force AR in replay version 6 and older, where the AR value is kept constant with respect to
        // game time. This makes the player perceive the fade in animation as is under all speed multipliers.
        if (ar == null || mods.none { it is ModReplayV6 }) {
            return
        }

        applyOldFadeAdjustment(hitObject, mods)

        if (hitObject is Slider) {
            hitObject.nestedHitObjects.forEach {
                scope?.ensureActive()

                applyOldFadeAdjustment(it, mods)
            }
        }
    }

    override fun applyFromBeatmap(beatmap: Beatmap) {
        val difficulty = beatmap.difficulty

        updateDefaultValue(::cs, difficulty.gameplayCS)
        updateDefaultValue(::ar, difficulty.ar)
        updateDefaultValue(::od, difficulty.od)
        updateDefaultValue(::hp, difficulty.hp)
    }

    private fun updateDefaultValue(property: KProperty0<Float?>, value: Float?) {
        val delegate = getModSettingDelegate<NullableFloatModSetting>(property)

        delegate.defaultValue = value
    }

    private fun applyOldFadeAdjustment(hitObject: HitObject, mods: Iterable<Mod>) {
        val initialTrackRate = ModUtils.calculateRateWithMods(mods)
        val currentTrackRate = ModUtils.calculateRateWithMods(mods, hitObject.startTime)

        // Cancel the rate that was initially applied to timePreempt (via applyToDifficulty above and
        // HitObject.applyDefaults) and apply the current one.
        hitObject.timePreempt *= currentTrackRate / initialTrackRate

        hitObject.timeFadeIn *= currentTrackRate
    }

    private fun getValue(value: Float?, fallback: Float) = value ?: fallback

    override val extraInformation: String
        get() {
            val settings = mutableListOf<String>()

            if (cs != null) {
                settings += "CS%.1f".format(cs)
            }

            if (ar != null) {
                settings += "AR%.1f".format(ar)
            }

            if (od != null) {
                settings += "OD%.1f".format(od)
            }

            if (hp != null) {
                settings += "HP%.1f".format(hp)
            }

            return settings.joinToString(", ")
        }
}

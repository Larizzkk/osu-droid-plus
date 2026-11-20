package com.osudroid.scoring

import com.osudroid.GameMode
import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.mods.*
import com.osudroid.utils.applyMods
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Current score multiplier calculator. This is used to calculate score after version 5 database migration.
 */
class ScoreMultiplierCalculator @JvmOverloads constructor(difficulty: BeatmapDifficulty? = null) :
    BaseScoreMultiplierCalculator<Double>(difficulty) {

    private var appliedDifficulty: BeatmapDifficulty? = null

    init {
        // region Difficulty Reduction

        single<ModEasy>(0.8)
        single<ModNoFail>(0.5)
        single<ModReallyEasy>(0.3)

        // endregion

        // region Difficulty Increase

        single<ModHardRock>(1.04)
        single<ModPrecise> { preciseMultiplier() }
        single<ModHidden> { hiddenMultiplier() }
        single<ModTraceable>(1.03)
        combination<ModFlashlight, ModFreezeFrame> { flashlight, _ -> 1 + (flashlight.flashlightMultiplier() - 1) / 2 }
        single<ModFlashlight> { flashlightMultiplier() }

        // endregion

        // region Conversion

        single<ModDifficultyAdjust> { difficultyAdjustMultiplier() }
        group<ModRateAdjust> { rateAdjustMultiplier() }

        // endregion

        // region Automation

        single<ModRelax>(1.0)
        single<ModAutopilot>(1.0)

        // endregion

        // region Fun

        // 2B and No Notelock remove the slider/spinner and note locks, so they
        // are easier than normal play. The server applies the same 0.8 to the
        // notelock flag these mods set.
        single<Mod2B>(0.9)
        single<ModNoNotelock>(0.8)
        single<ModWindUp> { timeRampMultiplier() }
        single<ModWindDown> { timeRampMultiplier() }
        single<ModRandom> { randomMultiplier() }
        single<ModRandomPlus> { randomPlusMultiplier() }
        single<ModApproachDifferent>(0.7)
        single<ModGravity>(1.06)
        single<ModSynesthesia>(0.99)

        // endregion
    }

    override fun calculateFor(mods: Iterable<Mod>): Double {
        val appliedDifficulty = difficulty?.clone()

        if (appliedDifficulty != null) {
            appliedDifficulty.applyMods(GameMode.Droid, mods)
            this.appliedDifficulty = appliedDifficulty
        }

        return super.calculateFor(mods)
    }

    override val defaultMultiplier = 1.0
    override fun multiply(a: Double, b: Double) = a * b

    /**
     * Random becomes harder as its intensity and angle sharpness grow, so the
     * score multiplier grows with it. This mirrors the server's PP bonus.
     */
    private fun ModRandom.randomMultiplier(): Double {
        val bonus = 1.15 + 0.10 * (aggressive.toDouble() - 1.0) + 0.015 * max(0.0, angleSharpness.toDouble() - 7.0)
        return bonus.coerceIn(1.15, 1.45)
    }

    /**
     * Random+ is harder than Random: every note is a real jump and edge bias
     * lengthens the movement. Mirrors the server's PP multiplier.
     */
    private fun ModRandomPlus.randomPlusMultiplier(): Double {
        val jumpBonus = 0.0015 * max(0.0, minimumJump.toDouble() - 100.0)
        val edgeBonus = 0.12 * edgeBias.toDouble().coerceIn(0.0, 1.0)

        return (1.3 + jumpBonus + edgeBonus).coerceIn(1.3, 1.7)
    }

    private fun ModDifficultyAdjust.difficultyAdjustMultiplier(): Double {
        var multiplier = 1.0

        cs?.let { csValue ->
            val diff = (csValue - (difficulty?.difficultyCS ?: return@let)).toDouble()

            multiplier *=
                if (diff >= 0) 1 + 0.0075 * diff.pow(1.5)
                else 2 / (1 + exp(-0.5 * diff))
        }

        od?.let { odValue ->
            val diff = (odValue - (difficulty?.od ?: return@let)).toDouble()

            multiplier *=
                if (diff >= 0) 1.0 + 0.005 * diff.pow(1.3)
                else 2.0 / (1.0 + exp(-0.25 * diff))
        }

        return multiplier
    }

    private fun preciseMultiplier(): Double {
        // Keep original multiplier if applied difficulty is not present.
        val appliedDifficulty = appliedDifficulty ?: return 1.06

        return 1.02 + 0.08 * (appliedDifficulty.od / 10.0).pow(2)
    }

    companion object {
        private fun ModHidden.hiddenMultiplier(): Double {
            var value = 1.06

            if (onlyFadeApproachCircles) {
                value -= 0.03
            }

            return value
        }

        private fun ModFlashlight.flashlightMultiplier(): Double {
            // Multiplier of 1.2x, reduced by 0.02 per 0.1 increase in flashlight size.
            val value = (1.2 - 0.2 * (sizeMultiplier - 1)).coerceIn(1.02, 1.2)

            return if (!comboBasedSize) 1 + (value - 1) / 5 else value
        }

        private fun ModTimeRamp.timeRampMultiplier(): Double {
            val minSpeed = min(initialRate, finalRate)
            val maxSpeed = max(initialRate, finalRate)

            val minMultiplier =
                if (minSpeed < 1f) halfTimeMultiplier(minSpeed)
                else doubleTimeMultiplier(minSpeed)

            val maxMultiplier =
                if (maxSpeed < 1f) halfTimeMultiplier(maxSpeed)
                else doubleTimeMultiplier(maxSpeed)

            return 0.8 * minMultiplier + 0.2 * maxMultiplier
        }

        private fun Iterable<ModRateAdjust>.rateAdjustMultiplier(): Double {
            val combinedRate = fold(1f) { acc, mod -> acc * mod.trackRateMultiplier }

            return if (combinedRate < 1f) halfTimeMultiplier(combinedRate)
            else doubleTimeMultiplier(combinedRate)
        }

        private fun halfTimeMultiplier(speedChange: Float) =
            // 0.25x at 0.5x speed, +0.075x per 0.05x speed increment. Default HT (0.75x) = 0.625.
            max(0.1, speedChange * 1.5 - 0.5)

        private fun doubleTimeMultiplier(speedChange: Float) =
            // Linear from 1.0 to 1.46.
            // Default DT (1.5x) = 1.23
            1 + (speedChange - 1) * 0.46
    }
}

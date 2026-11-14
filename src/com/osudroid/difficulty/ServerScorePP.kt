package com.osudroid.difficulty

import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.mods.ModAutopilot
import com.osudroid.mods.ModDifficultyAdjust
import com.osudroid.mods.ModEasy
import com.osudroid.mods.ModHardRock
import com.osudroid.mods.ModPrecise
import com.osudroid.mods.ModReallyEasy
import com.osudroid.mods.ModRelax
import com.osudroid.mods.ModWindDown
import com.osudroid.mods.ModWindUp
import com.osudroid.utils.ModHashMap
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow

/**
 * Mirrors the server's per-mode PP systems for the result screen.
 *
 * The server stores three separate values for a score: DPP (normal play),
 * RXPP (Relax) and APPP (Autopilot). The result screen used to always show the
 * local DPP calculation, even when the submitted score was RX/AP and the server
 * ranked a different value.
 *
 * Only RX/AP are handled here; normal play keeps the client's existing DPP
 * calculation. RX and AP formulas follow the server implementation:
 * - RX: standard PP with the server's OD-4/PR/RE adjustment, multiplied by the
 *   AR bonus; values >= 10000 are rejected.
 * - AP: speed/accuracy/flashlight only, normalized with exponent 1.1 and
 *   multiplied by 1.12, then passed through the same PP envelope.
 */
object ServerScorePP {
    private const val RX_MAX_PP = 10000.0
    private const val PP_ENVELOPE_START = 4500.0
    private const val PP_ENVELOPE_RANGE = 2500.0

    // The server only passes these mods to rosu. Client-only mods (2B, SV2,
    // PR/RE and automation mods) must not influence the displayed RX/AP PP.
    private val STANDARD_MOD_ACRONYMS = setOf(
        "NF", "EZ", "HD", "HR", "SD", "DT", "NC", "HT",
        "FL", "SO", "PF", "WD", "WU", "DA", "CS"
    )

    class Result(
        @JvmField val label: String,
        @JvmField val value: Double,
        @JvmField val max: Double,
        @JvmField val stars: Double
    )

    @JvmStatic
    fun calculate(beatmap: Beatmap, mods: ModHashMap, stat: StatisticV2): Result? {
        val isRX = mods.containsKey(ModRelax::class.java)
        val isAP = mods.containsKey(ModAutopilot::class.java)

        if (!isRX && !isAP) {
            return null
        }

        // The server rejects RX combined with AP/DA/WD/WU outright.
        if (isRX && (isAP ||
                mods.containsKey(ModDifficultyAdjust::class.java) ||
                mods.containsKey(ModWindDown::class.java) ||
                mods.containsKey(ModWindUp::class.java))
        ) {
            return Result("rxpp", 0.0, 0.0, 0.0)
        }

        // RX/AP are applied by the server's own formula, not by rosu's Relax
        // or Autopilot handling. PR/RE are droid-only and are not passed to
        // rosu either; their stat changes are applied explicitly below.
        val standardMods = ModHashMap(mods.values.filter { it.acronym in STANDARD_MOD_ACRONYMS })
        val playableBeatmap = beatmap.createStandardPlayableBeatmap(standardMods.values)
        val attributes = BeatmapDifficultyCalculator.calculateStandardDifficulty(beatmap, playableBeatmap)

        if (isRX) {
            val hasRE = mods.containsKey(ModReallyEasy::class.java)
            var od = beatmap.difficulty.od.toDouble() - 4.0

            if (mods.containsKey(ModPrecise::class.java)) {
                od += 4.0
            }
            if (hasRE) {
                od /= 2.0
            }
            od = od.coerceIn(0.0, 10.0)

            if (mods.containsKey(ModEasy::class.java) && !hasRE) {
                od *= 0.5
            }
            if (mods.containsKey(ModHardRock::class.java)) {
                od = min(od * 1.4, 11.0)
            }

            attributes.overallDifficulty = od

            val ar = if (hasRE) {
                beatmap.difficulty.ar - 0.5
            } else {
                attributes.approachRate
            }
            attributes.approachRate = ar

            val arAfterMods = rateAdjustedApproachRate(ar, attributes.clockRate)
            val objects = beatmap.hitObjects.objects.size
            val performance = BeatmapDifficultyCalculator.calculateStandardPerformance(
                playableBeatmap, attributes, stat
            )
            val maxPerformance = BeatmapDifficultyCalculator.calculateStandardPerformance(attributes)

            return Result(
                "rxpp",
                calculateRXPP(performance.total, arAfterMods, objects),
                calculateRXPP(maxPerformance.total, arAfterMods, objects),
                attributes.starRating
            )
        }

        val performance = BeatmapDifficultyCalculator.calculateStandardPerformance(
            playableBeatmap, attributes, stat
        )
        val maxPerformance = BeatmapDifficultyCalculator.calculateStandardPerformance(attributes)

        return Result(
            "appp",
            capScorePP(calculateAPPP(performance.speed, performance.accuracy, performance.flashlight, stat.misses)),
            capScorePP(calculateAPPP(maxPerformance.speed, maxPerformance.accuracy, maxPerformance.flashlight, 0)),
            attributes.starRating
        )
    }

    private fun calculateRXPP(rosuPP: Double, ar: Double, objects: Int): Double {
        var arBonus = 0.0
        if (ar > 10.33) {
            arBonus += 0.4 * (ar - 10.33)
        } else if (ar < 8.0) {
            arBonus += 0.01 * (8.0 - ar)
        }

        val objectScale = min(1.0, objects / 1000.0)
        val finalPP = rosuPP * (1.0 + min(arBonus, arBonus * objectScale))
        return if (!finalPP.isFinite() || finalPP < 0 || finalPP >= RX_MAX_PP) 0.0 else finalPP
    }

    private fun calculateAPPP(speed: Double, accuracy: Double, flashlight: Double, misses: Int): Double {
        var multiplier = 1.12
        if (misses > 0) {
            multiplier *= (1.0 - 0.02 * misses).coerceAtLeast(0.9)
        }
        return norm(1.1, 0.0, speed, accuracy, flashlight) * multiplier
    }

    private fun capScorePP(pp: Double): Double {
        if (!pp.isFinite() || pp <= 0) return 0.0
        if (pp <= PP_ENVELOPE_START) return pp
        val over = pp - PP_ENVELOPE_START
        return PP_ENVELOPE_START + PP_ENVELOPE_RANGE * (1.0 - exp(-over / PP_ENVELOPE_RANGE))
    }

    private fun norm(p: Double, vararg values: Double) =
        values.sumOf { it.coerceAtLeast(0.0).pow(p) }.pow(1.0 / p)

    private fun rateAdjustedApproachRate(ar: Double, clockRate: Double): Double {
        val preempt = BeatmapDifficulty.difficultyRange(
            ar,
            HitObject.PREEMPT_MAX,
            HitObject.PREEMPT_MID,
            HitObject.PREEMPT_MIN
        ) / clockRate

        return BeatmapDifficulty.inverseDifficultyRange(
            preempt,
            HitObject.PREEMPT_MAX,
            HitObject.PREEMPT_MID,
            HitObject.PREEMPT_MIN
        )
    }
}

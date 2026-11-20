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
import com.osudroid.utils.ModHashMap
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
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
 * - RX: standard PP with the server's OD-4/PR/RE adjustment (DA overrides the
 *   stats it sets), multiplied by the AR bonus; no 10000 PP limit.
 * - AP: speed/accuracy/flashlight only, normalized with exponent 1.1 and
 *   multiplied by 1.12; no envelope.
 */
object ServerScorePP {

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

        // RX+AP is contradictory; DA/WD/WU are valid and must still score.
        if (isRX && isAP) {
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
            val da = mods.values.filterIsInstance<ModDifficultyAdjust>().firstOrNull()
            var od = da?.od?.toDouble() ?: (beatmap.difficulty.od.toDouble() - 4.0)

            if (da?.od == null) {
                if (mods.containsKey(ModPrecise::class.java)) {
                    od += 4.0
                }
                if (hasRE) {
                    od /= 2.0
                }
            }
            od = od.coerceIn(0.0, 10.0)

            if (mods.containsKey(ModEasy::class.java) && !hasRE) {
                od *= 0.5
            }
            if (mods.containsKey(ModHardRock::class.java)) {
                od = min(od * 1.4, 11.0)
            }

            attributes.overallDifficulty = od

            val ar = when {
                da?.ar != null -> da.ar!!.toDouble()
                hasRE -> beatmap.difficulty.ar - 0.5
                else -> attributes.approachRate
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
        if (!finalPP.isFinite() || finalPP < 0) return 0.0
        return finalPP
    }

    private fun calculateAPPP(speed: Double, accuracy: Double, flashlight: Double, misses: Int): Double {
        var multiplier = 1.12
        if (misses > 0) {
            multiplier *= (1.0 - 0.02 * misses).coerceAtLeast(0.9)
        }
        return norm(1.1, 0.0, speed, accuracy, flashlight) * multiplier
    }

    /** No cap: only non-finite values are treated as zero. */
    private fun capScorePP(pp: Double): Double =
        if (!pp.isFinite() || pp <= 0) 0.0 else pp

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

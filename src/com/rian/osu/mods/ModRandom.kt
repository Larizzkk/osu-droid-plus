package com.rian.osu.mods

import com.reco1l.toolkt.roundBy
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.math.Random
import com.rian.osu.mods.settings.*
import com.rian.osu.utils.HitObjectGenerationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * Represents the Random mod.
 *
 * Truly randomizes hit object positions, angles, and slider orientations
 * while keeping everything within the playfield bounds.
 */
class ModRandom : Mod(), IModApplicableToBeatmap {
    override val name = "Random"
    override val acronym = "RD"
    override val description = "It never gets boring!"
    override val type = ModType.Conversion
    override val isRanked = true
    override val scoreMultiplier = 1f

    /**
     * The seed that is used to generate the random numbers.
     *
     * If `null`, a random seed will be generated.
     */
    var seed by NullableIntegerModSetting(
        name = "Seed",
        key = "seed",
        valueFormatter = { it?.toString() ?: "" },
        defaultValue = null,
        minValue = 0,
        orderPosition = 0,
        useManualInput = true
    )

    /**
     * Defines how sharp the jumps between [HitObject]s should be.
     *
     * Higher values enforce larger minimum distances between consecutive objects,
     * preventing them from clustering too closely together.
     */
    var angleSharpness by FloatModSetting(
        name = "Angle sharpness",
        key = "angleSharpness",
        valueFormatter = { "${it.roundBy(1)}x" },
        defaultValue = 7f,
        minValue = 1f,
        maxValue = 10f,
        step = 0.1f,
        precision = 1
    )

    /**
     * Controls the overall intensity of the randomization.
     *
     * Higher values increase the maximum jump distance and the probability of slider flips,
     * resulting in more chaotic and unpredictable patterns.
     */
    var aggressive by FloatModSetting(
        name = "Aggressive",
        key = "aggressive",
        valueFormatter = { "${it.roundBy(1)}x" },
        defaultValue = 1f,
        minValue = 0.1f,
        maxValue = 3f,
        step = 0.1f,
        precision = 1
    )

    private var random: Random? = null

    override fun applyToBeatmap(beatmap: Beatmap, scope: CoroutineScope?) {
        if (seed == null) {
            seed = Random.nextInt()
        }

        random = Random(seed!!)

        val positionInfos = HitObjectGenerationUtils.generatePositionInfos(beatmap.hitObjects.objects, scope)
        val sharpness = getModSettingDelegate<FloatModSetting>(::angleSharpness).value
        val aggressive = getModSettingDelegate<FloatModSetting>(::aggressive).value

        // Distance bounds scaled by aggressive and sharpness
        val minDist = (30f + sharpness * 12f + aggressive * 25f).coerceAtMost(180f)
        val maxDist = (100f + aggressive * 220f).coerceAtMost(500f)
        val flipChance = (0.12f * aggressive).coerceAtMost(0.7f)

        for (i in positionInfos.indices) {
            scope?.ensureActive()

            val positionInfo = positionInfos[i]
            val hitObject = positionInfo.hitObject

            // Truly random distance within bounds
            positionInfo.distanceFromPrevious = (minDist + random!!.nextDouble().toFloat() * (maxDist - minDist))

            // Fully random angle (uniform [0, 2π))
            positionInfo.relativeAngle = random!!.nextDouble().toFloat() * Math.PI.toFloat() * 2f

            // Slider randomization
            if (hitObject is Slider) {
                // Random slider rotation
                positionInfo.rotation = random!!.nextDouble().toFloat() * Math.PI.toFloat() * 2f

                // Randomly flip slider based on aggressive
                if (random!!.nextDouble() < flipChance) {
                    HitObjectGenerationUtils.flipSliderInPlaceHorizontally(hitObject, scope)
                }
            }
        }

        val repositionedObjects = HitObjectGenerationUtils.repositionHitObjects(positionInfos, scope)

        for (i in repositionedObjects.indices) {
            scope?.ensureActive()

            beatmap.hitObjects.objects[i] = repositionedObjects[i]
        }
    }

    companion object {
        private val playfieldDiagonal = HitObjectGenerationUtils.playfieldSize.length
    }
}

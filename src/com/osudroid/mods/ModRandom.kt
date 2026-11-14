package com.osudroid.mods

import com.reco1l.framework.math.roundBy
import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.math.Random
import com.osudroid.mods.settings.*
import com.osudroid.utils.HitObjectGenerationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * Represents the Random mod.
 *
 * Unlike the old pattern-preserving implementation, this randomizes every hit
 * object's position, jump distance and angle, and every slider's orientation.
 * The result is the same kind of layout the legacy osu!droid Random produced.
 */
class ModRandom : Mod(), IModApplicableToBeatmap {
    override val name = "Random"
    override val acronym = "RD"
    override val description = "It never gets boring!"
    override val type = ModType.Conversion

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
     * Defines how sharp the jumps between hit objects should be.
     */
    var angleSharpness by FloatModSetting(
        name = "Angle sharpness",
        key = "angleSharpness",
        valueFormatter = { it.roundBy(1).toString() },
        defaultValue = 7f,
        minValue = 1f,
        maxValue = 10f,
        step = 0.1f,
        precision = 1
    )

    /**
     * Overall randomization intensity. Higher values increase jump distances,
     * slider flips and therefore the difficulty (and PP bonus) of the map.
     */
    var aggressive by FloatModSetting(
        name = "Aggressive",
        key = "aggressive",
        valueFormatter = { it.roundBy(1).toString() },
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
        val intensity = getModSettingDelegate<FloatModSetting>(::aggressive).value

        val minDistance = (30f + sharpness * 12f + intensity * 25f).coerceAtMost(190f)
        val maxDistance = (100f + intensity * 220f).coerceAtMost(500f)
        val flipChance = (0.12f * intensity).coerceAtMost(0.7f)

        for (i in positionInfos.indices) {
            scope?.ensureActive()

            val positionInfo = positionInfos[i]
            val hitObject = positionInfo.hitObject

            // Every object gets a new distance and a fully random angle.
            positionInfo.distanceFromPrevious =
                minDistance + random!!.nextDouble().toFloat() * (maxDistance - minDistance)
            positionInfo.relativeAngle = random!!.nextDouble().toFloat() * (Math.PI * 2).toFloat()

            if (hitObject is Slider) {
                // Rotate the slider and sometimes flip it, so slider paths do
                // not stay aligned with the original map.
                positionInfo.rotation = random!!.nextDouble().toFloat() * (Math.PI * 2).toFloat()

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
}

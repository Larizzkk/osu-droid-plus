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

    private fun getRandomOffset(stdDev: Float): Float {
        val angleSharpness = getModSettingDelegate<FloatModSetting>(::angleSharpness)

        // Range: [0.5, 2]
        // Higher angle sharpness -> lower multiplier
        val customMultiplier =
            (1.5f * angleSharpness.maxValue - angleSharpness.value) /
            (1.5f * angleSharpness.maxValue - angleSharpness.defaultValue)

        return HitObjectGenerationUtils.randomGaussian(random!!, 0f, stdDev * customMultiplier)
    }

    /**
     * @param targetDistance The target distance between the previous and the current [HitObject].
     * @param offset The angle (in radians) by which the target angle should be offset.
     * @param flowDirection Whether the relative angle should be positive (`false`) or negative (`true`).
     */
    private fun getRelativeTargetAngle(targetDistance: Float, offset: Float, flowDirection: Boolean): Float {
        val angleSharpnessDelegate = getModSettingDelegate<FloatModSetting>(::angleSharpness)

        // Range: [0.1, 1]
        val angleSharpness = angleSharpnessDelegate.value / angleSharpnessDelegate.maxValue
        // Range: [0, 0.9]
        val angleWideness = 1 - angleSharpness

        // Range: [-60, 30]
        val customOffsetX = angleSharpness * 100 - 70
        // Range: [-0.075, 0.15]
        val customOffsetY = angleWideness * 0.25f - 0.075f

        val angle =
            (2.16 / (1 + 200 * exp(0.036 * (targetDistance + customOffsetX * 2 - 310))) + 0.5).toFloat() +
            offset + customOffsetY

        val relativeAngle = Math.PI.toFloat() - angle

        return if (flowDirection) -relativeAngle else relativeAngle
    }

    /**
     * Determines whether a new section should be started at the current [HitObject].
     */
    private fun shouldStartNewSection(
        beatmap: Beatmap,
        positionInfos: List<HitObjectGenerationUtils.HitObjectPositionInfo>,
        i: Int
    ): Boolean {
        if (i == 0) {
            return true
        }

        // Exclude new-combo-spam and 1-2-combos.
        val previousObjectStartedCombo =
            positionInfos[max(0, i - 2)].hitObject.indexInCurrentCombo > 1 && positionInfos[i - 1].hitObject.isNewCombo

        val previousObjectWasOnDownBeat =
            HitObjectGenerationUtils.isHitObjectOnBeat(beatmap, positionInfos[i - 1].hitObject, true)

        val previousObjectWasOnBeat =
            HitObjectGenerationUtils.isHitObjectOnBeat(beatmap, positionInfos[i - 1].hitObject)

        return (previousObjectStartedCombo && random!!.nextDouble() < 0.6f) ||
                previousObjectWasOnDownBeat ||
                (previousObjectWasOnBeat && random!!.nextDouble() < 0.4f)
    }

    private fun shouldApplyFlowChange(
        positionInfos: List<HitObjectGenerationUtils.HitObjectPositionInfo>,
        i: Int
    ): Boolean {
        // Exclude new-combo-spam and 1-2-combos.
        val previousObjectStartedCombo =
            positionInfos[max(0, i - 2)].hitObject.indexInCurrentCombo > 1 && positionInfos[i - 1].hitObject.isNewCombo

        return previousObjectStartedCombo && random!!.nextDouble() < 0.6
    }

    companion object {
        private val playfieldDiagonal = HitObjectGenerationUtils.playfieldSize.length
    }
}
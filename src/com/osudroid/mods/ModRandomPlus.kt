package com.osudroid.mods

import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.hitobjects.HitCircle
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.Spinner
import com.osudroid.math.Random
import com.osudroid.math.Vector2
import com.osudroid.mods.settings.FloatModSetting
import com.osudroid.mods.settings.NullableIntegerModSetting
import com.osudroid.utils.HitObjectGenerationUtils
import kotlin.math.PI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * Represents the Random + mod.
 *
 * Unlike [ModRandom], which keeps a chain of relative jumps, this mod places
 * every object independently anywhere inside the playfield. Positions are not
 * derived from the previous object and are allowed to overlap.
 */
class ModRandomPlus : Mod(), IModApplicableToBeatmap {
    override val name = "Random +"
    override val acronym = "R+"
    override val description = "Absolutely random object placement, independent on every note."
    override val type = ModType.Conversion
    override val isRanked = false

    /**
     * The seed used to generate the random positions.
     *
     * If `null`, a random seed is generated and stored so the result is
     * reproducible by a replay and by the mod's serialized settings.
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
     * How much of the random layout is applied.
     *
     * `0%` keeps the original layout, `100%` applies the full randomization
     * (including random slider rotations). Values in between interpolate from
     * the original position to the random position.
     */
    var intensity by FloatModSetting(
        name = "Intensity",
        key = "intensity",
        valueFormatter = { "${(it * 100).toInt()}%" },
        defaultValue = 1f,
        minValue = 0f,
        maxValue = 1f,
        step = 0.05f,
        precision = 2,
        orderPosition = 1
    )

    override fun applyToBeatmap(beatmap: Beatmap, scope: CoroutineScope?) {
        if (seed == null) {
            seed = Random.nextInt()
        }

        val random = Random(seed!!)
        val appliedIntensity = intensity.coerceIn(0f, 1f)

        if (appliedIntensity <= 0f) {
            return
        }

        for (hitObject in beatmap.hitObjects.objects) {
            scope?.ensureActive()

            if (hitObject is Spinner) {
                continue
            }

            val radius = hitObject.gameplayRadius.toFloat()

            if (hitObject is Slider) {
                val rotation = ((random.nextDouble().toFloat() * 2f - 1f) * PI.toFloat()) * appliedIntensity

                if (rotation != 0f) {
                    HitObjectGenerationUtils.rotateSlider(hitObject, rotation, scope)
                }

                var bounds = HitObjectGenerationUtils.calculatePossibleMovementBounds(hitObject, scope)

                if (bounds.width < 0 || bounds.height < 0) {
                    // This rotation cannot fit inside the playfield. Keep the
                    // original orientation instead of making the slider unhittable.
                    if (rotation != 0f) {
                        HitObjectGenerationUtils.rotateSlider(hitObject, -rotation, scope)
                    }

                    bounds = HitObjectGenerationUtils.calculatePossibleMovementBounds(hitObject, scope)
                }

                if (bounds.width < 0 || bounds.height < 0) {
                    // The slider is larger than the playfield even at its original
                    // rotation, so a random placement would be impossible.
                    hitObject.position = HitObjectGenerationUtils.playfieldCenter
                } else {
                    val original = Vector2(hitObject.position.x, hitObject.position.y)
                    val randomPosition = Vector2(
                        randomBetween(bounds.left, bounds.right, random),
                        randomBetween(bounds.top, bounds.bottom, random)
                    )
                    val target = original + (randomPosition - original) * appliedIntensity

                    hitObject.position = Vector2(
                        target.x.coerceIn(bounds.left, bounds.right),
                        target.y.coerceIn(bounds.top, bounds.bottom)
                    )
                }
            } else if (hitObject is HitCircle) {
                val original = Vector2(hitObject.position.x, hitObject.position.y)
                val randomPosition = randomPoint(radius, random)

                hitObject.position = original + (randomPosition - original) * appliedIntensity
            }
        }
    }

    private fun randomPoint(radius: Float, random: Random) = Vector2(
        randomBetween(radius, HitObjectGenerationUtils.playfieldSize.x - radius, random),
        randomBetween(radius, HitObjectGenerationUtils.playfieldSize.y - radius, random)
    )

    private fun randomBetween(min: Float, max: Float, random: Random): Float {
        if (max <= min) {
            return (min + max) / 2f
        }

        return min + random.nextDouble().toFloat() * (max - min)
    }
}

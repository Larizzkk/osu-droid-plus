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
 * every object independently inside the playfield. To make the result genuinely
 * harder, consecutive objects are kept at a configurable minimum distance and
 * the layout is biased towards the playfield edges.
 */
class ModRandomPlus : Mod(), IModApplicableToBeatmap {
    override val name = "Random +"
    override val acronym = "R+"
    override val description = "Independent random placement with long, edge-biased jumps."
    override val type = ModType.Conversion
    override val isRanked = true
    override val iconTextureNameSuffix = "random"

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

    /**
     * Minimum distance between two consecutive objects, in osu! pixels.
     *
     * Without this, independent random placement regularly put notes next to
     * each other, which was easier than the original map. A high value forces
     * every note to be a real jump.
     */
    var minimumJump by FloatModSetting(
        name = "Minimum jump",
        key = "minimumJump",
        valueFormatter = { "${it.toInt()}px" },
        defaultValue = 220f,
        minValue = 0f,
        maxValue = 400f,
        step = 5f,
        precision = 0,
        orderPosition = 2
    )

    /**
     * How strongly objects are pushed away from the playfield center.
     *
     * `0` is uniform placement, `100%` keeps objects close to the edges. Edge
     * placement creates longer cross-screen movements than a uniform layout.
     */
    var edgeBias by FloatModSetting(
        name = "Edge bias",
        key = "edgeBias",
        valueFormatter = { "${(it * 100).toInt()}%" },
        defaultValue = 0.3f,
        minValue = 0f,
        maxValue = 1f,
        step = 0.05f,
        precision = 2,
        orderPosition = 3
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

        var previousPosition: Vector2? = null
        val minJump = minimumJump.coerceIn(0f, 400f) * appliedIntensity

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
                    val positionBounds = bounds
                    val target = pickFarthest(previousPosition, minJump) {
                        val randomPosition = Vector2(
                            randomBetween(positionBounds.left, positionBounds.right, random),
                            randomBetween(positionBounds.top, positionBounds.bottom, random)
                        )
                        val interpolated = original + (randomPosition - original) * appliedIntensity

                        Vector2(
                            interpolated.x.coerceIn(positionBounds.left, positionBounds.right),
                            interpolated.y.coerceIn(positionBounds.top, positionBounds.bottom)
                        )
                    }

                    hitObject.position = target
                }
            } else if (hitObject is HitCircle) {
                val original = Vector2(hitObject.position.x, hitObject.position.y)

                hitObject.position = pickFarthest(previousPosition, minJump) {
                    val randomPosition = randomPoint(radius, random)
                    original + (randomPosition - original) * appliedIntensity
                }
            }

            previousPosition = Vector2(hitObject.position.x, hitObject.position.y)
        }
    }

    /**
     * Picks the farthest of several random candidates until one is at least
     * [minDistance] away from [previous]. If the playfield/bounds cannot fit such
     * a jump, the farthest candidate is used instead of leaving the object close.
     */
    private fun pickFarthest(previous: Vector2?, minDistance: Float, sample: () -> Vector2): Vector2 {
        val first = sample()

        if (previous == null || minDistance <= 0f) {
            return first
        }

        val minDistanceSquared = minDistance * minDistance
        var best = first
        var bestDistance = first.getDistanceSquared(previous)

        if (bestDistance >= minDistanceSquared) {
            return best
        }

        repeat(23) {
            val candidate = sample()
            val distance = candidate.getDistanceSquared(previous)

            if (distance > bestDistance) {
                best = candidate
                bestDistance = distance
            }

            if (bestDistance >= minDistanceSquared) {
                return best
            }
        }

        return best
    }

    private fun randomPoint(radius: Float, random: Random): Vector2 {
        val left = radius
        val right = HitObjectGenerationUtils.playfieldSize.x - radius
        val top = radius
        val bottom = HitObjectGenerationUtils.playfieldSize.y - radius
        var x = randomBetween(left, right, random)
        var y = randomBetween(top, bottom, random)

        if (edgeBias > 0f) {
            val centerX = HitObjectGenerationUtils.playfieldSize.x / 2f
            val centerY = HitObjectGenerationUtils.playfieldSize.y / 2f
            val scale = 1f + edgeBias.coerceIn(0f, 1f) * 1.4f

            x = (centerX + (x - centerX) * scale).coerceIn(left, right)
            y = (centerY + (y - centerY) * scale).coerceIn(top, bottom)
        }

        return Vector2(x, y)
    }

    private fun randomBetween(min: Float, max: Float, random: Random): Float {
        if (max <= min) {
            return (min + max) / 2f
        }

        return min + random.nextDouble().toFloat() * (max - min)
    }
}

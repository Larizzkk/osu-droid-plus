package com.osudroid.mods

import com.osudroid.GameMode
import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.BeatmapProcessor
import com.osudroid.beatmaps.hitobjects.HitCircle
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.SliderPath
import com.osudroid.beatmaps.hitobjects.SliderPathType
import com.osudroid.beatmaps.hitobjects.Spinner
import com.osudroid.math.Vector2
import com.osudroid.utils.HitObjectGenerationUtils
import org.junit.Assert
import org.junit.Test

class ModRandomPlusTest {
    @Test
    fun `Test objects are placed independently and reproducibly`() {
        val beatmap = createBeatmap()
        val originalPositions = beatmap.hitObjects.objects.map { it.position }

        val mod = ModRandomPlus().apply { seed = 123456789 }
        mod.applyToBeatmap(beatmap)

        val firstPass = beatmap.hitObjects.objects.map { it.position }

        // Circles and slider heads are moved, while spinners keep their position.
        Assert.assertNotEquals(originalPositions[0], firstPass[0])
        Assert.assertNotEquals(originalPositions[1], firstPass[1])
        Assert.assertNotEquals(originalPositions[2], firstPass[2])
        Assert.assertEquals(originalPositions[4], firstPass[4])

        for (hitObject in beatmap.hitObjects.objects) {
            if (hitObject is Spinner) continue

            val radius = hitObject.gameplayRadius.toFloat()

            if (hitObject is Slider) {
                val bounds = HitObjectGenerationUtils.calculatePossibleMovementBounds(hitObject)

                Assert.assertTrue(hitObject.position.x in bounds.left..bounds.right)
                Assert.assertTrue(hitObject.position.y in bounds.top..bounds.bottom)
            } else {
                Assert.assertTrue(hitObject.position.x in radius..(HitObjectGenerationUtils.playfieldSize.x - radius))
                Assert.assertTrue(hitObject.position.y in radius..(HitObjectGenerationUtils.playfieldSize.y - radius))
            }
        }

        // Applying the same seed again must produce exactly the same layout.
        val secondBeatmap = createBeatmap()
        ModRandomPlus().apply {
            seed = 123456789
            applyToBeatmap(secondBeatmap)
        }

        Assert.assertEquals(firstPass, secondBeatmap.hitObjects.objects.map { it.position })
    }

    @Test
    fun `Test minimum jump keeps consecutive notes apart`() {
        val beatmap = createBeatmap()

        ModRandomPlus().apply {
            seed = 987654321
            minimumJump = 220f
            edgeBias = 0f
            applyToBeatmap(beatmap)
        }

        val first = beatmap.hitObjects.objects[0].position
        val second = beatmap.hitObjects.objects[1].position

        Assert.assertTrue(first.getDistance(second) >= 220f)
    }

    @Test
    fun `Test zero intensity keeps the original layout`() {
        val beatmap = createBeatmap()
        val originalPositions = beatmap.hitObjects.objects.map { Vector2(it.position.x, it.position.y) }

        ModRandomPlus().apply {
            seed = 1
            intensity = 0f
            applyToBeatmap(beatmap)
        }

        Assert.assertEquals(originalPositions, beatmap.hitObjects.objects.map { it.position })
    }

    @Test
    fun `Test seed is generated when it is not set`() {
        val beatmap = createBeatmap()
        val mod = ModRandomPlus()

        mod.applyToBeatmap(beatmap)

        Assert.assertNotNull(mod.seed)
    }

    private fun createBeatmap(): Beatmap {
        val beatmap = Beatmap(GameMode.Droid).apply {
            difficulty.ar = 9f

            hitObjects.add(
                listOf(
                    HitCircle(startTime = 0.0, position = Vector2(100, 100), isNewCombo = true, comboOffset = 0),
                    HitCircle(startTime = 200.0, position = Vector2(200, 100), isNewCombo = false, comboOffset = 0),
                    Slider(
                        startTime = 400.0,
                        position = Vector2(300, 100),
                        repeatCount = 0,
                        path = SliderPath(
                            pathType = SliderPathType.Linear,
                            controlPoints = listOf(Vector2(0), Vector2(200, 0)),
                            expectedDistance = 200.0
                        ),
                        isNewCombo = false,
                        comboOffset = 0,
                        nodeSamples = mutableListOf()
                    ),
                    HitCircle(startTime = 800.0, position = Vector2(100, 300), isNewCombo = false, comboOffset = 0),
                    Spinner(startTime = 20000.0, endTime = 25000.0, isNewCombo = false)
                )
            )
        }

        val processor = BeatmapProcessor(beatmap)
        processor.preProcess()

        beatmap.hitObjects.objects.forEach {
            it.applyDefaults(beatmap.controlPoints, beatmap.difficulty, beatmap.mode)
        }

        processor.postProcess()

        return beatmap
    }
}

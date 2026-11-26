package com.acivev.ui

import org.anddev.andengine.entity.Entity
import org.anddev.andengine.entity.sprite.batch.ColoredSpriteBatch
import org.anddev.andengine.opengl.texture.region.TextureRegion
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager
import java.util.Random
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

/**
 * Port of danser-go's winter snow (`app/graphics/gui/drawables/snow.go`).
 *
 * The original uses five 256x256 snowflake textures, 200 sprites, additive
 * blending and mouse-driven wind. Drawing them as one batch per texture keeps
 * the whole effect at five draw calls instead of 200.
 */
class Snowfall : Entity() {

    private class Flake {
        var x = 0f
        var y = 0f
        var size = 0f
        var rotation = 0f
        var alpha = 0f
        var brightness = 1f
        var horizontalVelocity = 0f
        var wind = 0f
        var texture = 0
        var active = false
    }

    private val lock = Any()
    private val random = Random()
    private val flakes = Array(MAX_FLAKES) { Flake() }
    private val batches = ArrayList<SnowBatch>()
    private var firstUpdate = true
    private var lastPointerX = -1f
    private var textureCount = 0

    init {
        for (i in 1..5) {
            val region = ResourceManager.getInstance()
                .loadHighQualityAsset("snowflake$i", "gfx/snowflake$i.png") ?: continue

            val batch = SnowBatch(region, batches.size)
            batches.add(batch)
            attachChild(batch)
        }
        textureCount = batches.size
    }

    override fun onManagedUpdate(secondsElapsed: Float) {
        if (textureCount == 0) {
            return
        }

        synchronized(lock) {
            // danser: delta = (time - lastTime) / 16, i.e. 1 at 60 FPS.
            val delta = secondsElapsed * 1000f / 16f

            var mouse = pointerX
            var windUpdate = 0f
            if (mouse >= 0f) {
                val width = Config.getRES_WIDTH().toFloat()
                mouse = max(0f, min(mouse, width))
                if (lastPointerX != -1f) {
                    windUpdate = (mouse - lastPointerX) / 2000f
                }
                lastPointerX = mouse
            } else {
                lastPointerX = -1f
            }

            val screenHeight = Config.getRES_HEIGHT().toFloat()
            val heightScale = screenHeight / 768f
            var existing = 0

            for (flake in flakes) {
                if (!flake.active) {
                    continue
                }

                flake.horizontalVelocity += delta * flake.wind + windUpdate

                val baseSize = (MAX_SIZE * heightScale * 0.15f)
                val normalizedSize = if (baseSize > 0f) flake.size / baseSize else 1f

                flake.x += delta * flake.horizontalVelocity
                flake.y += delta * 0.5f * (1f - normalizedSize * 0.25f) * heightScale
                flake.rotation += delta * flake.horizontalVelocity / 6f * (180f / PI.toFloat())

                if (flake.y > screenHeight + flake.size / 2f) {
                    flake.active = false
                } else {
                    existing++
                }
            }

            val toAdd = MAX_FLAKES - existing
            if (toAdd > 0) {
                var added = 0
                for (flake in flakes) {
                    if (added >= toAdd) {
                        break
                    }
                    if (!flake.active) {
                        spawn(flake, firstUpdate)
                        added++
                    }
                }
                firstUpdate = false
            }
        }
    }

    private fun spawn(flake: Flake, onscreen: Boolean) {
        val width = Config.getRES_WIDTH().toFloat()
        val height = Config.getRES_HEIGHT().toFloat()
        val heightScale = height / 768f

        val size = (MIN_SIZE + random.nextFloat() * (MAX_SIZE - MIN_SIZE)) * heightScale * 0.15f

        flake.size = size
        flake.x = (random.nextFloat() * 1.4f - 0.2f) * width
        flake.y = if (onscreen) random.nextFloat() * height else -size
        flake.horizontalVelocity = (random.nextFloat() - 0.5f) / 8f
        flake.wind = (random.nextFloat() - 0.5f) / 4000f
        flake.rotation = random.nextFloat() * 360f
        flake.brightness = 1f - random.nextFloat() * 0.3f
        flake.alpha = 0.4f + random.nextFloat() * 0.3f
        flake.texture = random.nextInt(textureCount)
        flake.active = true
    }

    private inner class SnowBatch(
        private val region: TextureRegion,
        private val textureIndex: Int
    ) : ColoredSpriteBatch(region.texture, MAX_FLAKES + 1) {

        init {
            setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE)
            setIgnoreUpdate(true)
        }

        override fun onUpdateSpriteBatch(): Boolean {
            setIndex(0)
            resetColors()

            synchronized(lock) {
                var rect = 0
                for (flake in flakes) {
                    if (!flake.active || flake.texture != textureIndex) {
                        continue
                    }

                    val half = flake.size / 2f
                    drawWithoutChecks(
                        region,
                        flake.x - half,
                        flake.y - half,
                        flake.size,
                        flake.size,
                        flake.rotation,
                        1f,
                        1f
                    )
                    putColor(rect++, flake.brightness, flake.brightness, flake.brightness, flake.alpha)
                }

                setIndex(rect)
            }
            return true
        }
    }

    companion object {
        private const val MIN_SIZE = 60f
        private const val MAX_SIZE = 200f
        private const val MAX_FLAKES = 200

        /** Screen X of the last touch, set by [setSnowPointerX]. */
        @Volatile
        @JvmField
        var pointerX = -1f
    }
}

fun setSnowPointerX(x: Float) {
    Snowfall.pointerX = x
}

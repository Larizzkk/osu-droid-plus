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
 * Seasonal falling particles for the main menu, based on danser-go's winter
 * snow (`app/graphics/gui/drawables/snow.go`).
 *
 * Each style uses its own textures, particle count and blend function. Every
 * texture gets one batch, so the effect only costs a few draw calls.
 */
class Snowfall(private val style: Style = Style.SNOW) : Entity() {

    enum class Style {
        SNOW,
        VALENTINE_HEARTS,
        SPRING_LEAVES,
        AUTUMN_LEAVES,
        HALLOWEEN
    }

    private class Flake {
        var x = 0f
        var y = 0f
        var size = 0f
        var rotation = 0f
        var alpha = 0f
        var brightness = 1f
        var red = 1f
        var green = 1f
        var blue = 1f
        var horizontalVelocity = 0f
        var wind = 0f
        var texture = 0
        var active = false
    }

    private val lock = Any()
    private val random = Random()
    private val flakes = Array(MAX_SNOW_FLAKES) { Flake() }
    private val batches = ArrayList<SnowBatch>()
    private var firstUpdate = true
    private var lastPointerX = -1f
    private var textureCount = 0
    private val maxFlakes = when (style) {
        Style.SNOW -> MAX_SNOW_FLAKES
        Style.VALENTINE_HEARTS -> MAX_HEARTS
        Style.HALLOWEEN -> MAX_HALLOWEEN
        else -> MAX_LEAVES
    }
    private val maxFlakeSize = when (style) {
        Style.SNOW -> MAX_SNOW_SIZE
        Style.VALENTINE_HEARTS -> MAX_HEART_SIZE
        Style.HALLOWEEN -> MAX_HALLOWEEN_SIZE
        else -> MAX_LEAF_SIZE
    }

    init {
        val textureNames = when (style) {
            Style.SNOW -> (1..5).map { "snowflake$it" }
            Style.VALENTINE_HEARTS -> (1..3).map { "heart$it" }
            Style.HALLOWEEN -> listOf("pumpkin1", "ghost1")
            else -> (1..3).map { "leaf$it" }
        }
        for (name in textureNames) {
            val region = ResourceManager.getInstance()
                .loadHighQualityAsset(name, "gfx/$name.png") ?: continue

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

                val baseSize = (maxFlakeSize * heightScale * 0.15f)
                val normalizedSize = if (baseSize > 0f) flake.size / baseSize else 1f

                flake.x += delta * flake.horizontalVelocity
                flake.y += delta * 0.5f * (1f - normalizedSize * 0.25f) * heightScale
                if (style != Style.HALLOWEEN) {
                    flake.rotation += delta * flake.horizontalVelocity / 6f * (180f / PI.toFloat())
                }

                if (flake.y > screenHeight + flake.size / 2f) {
                    flake.active = false
                } else {
                    existing++
                }
            }

            val toAdd = maxFlakes - existing
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

        val minSize = when (style) {
            Style.SNOW -> MIN_SNOW_SIZE
            Style.VALENTINE_HEARTS -> MIN_HEART_SIZE
            Style.HALLOWEEN -> MIN_HALLOWEEN_SIZE
            else -> MIN_LEAF_SIZE
        }
        val maxSize = when (style) {
            Style.SNOW -> MAX_SNOW_SIZE
            Style.VALENTINE_HEARTS -> MAX_HEART_SIZE
            Style.HALLOWEEN -> MAX_HALLOWEEN_SIZE
            else -> MAX_LEAF_SIZE
        }
        val size = (minSize + random.nextFloat() * (maxSize - minSize)) * heightScale * 0.15f

        flake.size = size
        flake.x = (random.nextFloat() * 1.4f - 0.2f) * width
        flake.y = if (onscreen) random.nextFloat() * height else -size
        flake.horizontalVelocity = (random.nextFloat() - 0.5f) / 8f
        flake.wind = (random.nextFloat() - 0.5f) / 4000f
        flake.rotation = if (style == Style.HALLOWEEN) {
            (random.nextFloat() - 0.5f) * 24f
        } else {
            random.nextFloat() * 360f
        }
        flake.brightness = 1f - random.nextFloat() * 0.3f
        flake.alpha = 0.4f + random.nextFloat() * 0.3f
        flake.texture = random.nextInt(textureCount)

        when (style) {
            Style.SNOW -> {
                flake.red = flake.brightness
                flake.green = flake.brightness
                flake.blue = flake.brightness
            }
            Style.VALENTINE_HEARTS -> {
                setParticleColor(flake, VALENTINE_HEART_COLORS)
                flake.alpha = 0.8f + random.nextFloat() * 0.2f
            }
            Style.SPRING_LEAVES -> setParticleColor(flake, SPRING_LEAF_COLORS)
            Style.AUTUMN_LEAVES -> setParticleColor(flake, AUTUMN_LEAF_COLORS)
            Style.HALLOWEEN -> {
                // The pumpkin and ghost textures already carry their colours;
                // white vertex colour keeps them unchanged.
                flake.red = 1f
                flake.green = 1f
                flake.blue = 1f
                flake.alpha = 0.85f + random.nextFloat() * 0.15f
            }
        }
        flake.active = true
    }

    private fun setParticleColor(flake: Flake, colors: IntArray) {
        val color = colors[random.nextInt(colors.size)]
        flake.red = ((color shr 16) and 0xFF) / 255f
        flake.green = ((color shr 8) and 0xFF) / 255f
        flake.blue = (color and 0xFF) / 255f
        flake.brightness = 1f
        flake.alpha = 0.65f + random.nextFloat() * 0.25f
    }

    private inner class SnowBatch(
        private val region: TextureRegion,
        private val textureIndex: Int
    ) : ColoredSpriteBatch(region.texture, MAX_SNOW_FLAKES + 1) {

        init {
            if (style == Style.SNOW) {
                setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE)
            } else {
                setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
            }
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
                    putColor(rect++, flake.red, flake.green, flake.blue, flake.alpha)
                }

                setIndex(rect)
            }
            return true
        }
    }

    companion object {
        private const val MIN_SNOW_SIZE = 60f
        private const val MAX_SNOW_SIZE = 200f
        private const val MIN_LEAF_SIZE = 130f
        private const val MAX_LEAF_SIZE = 260f
        private const val MIN_HEART_SIZE = 90f
        private const val MAX_HEART_SIZE = 180f
        private const val MAX_SNOW_FLAKES = 200
        private const val MAX_LEAVES = 90
        private const val MAX_HEARTS = 80
        private const val MIN_HALLOWEEN_SIZE = 150f
        private const val MAX_HALLOWEEN_SIZE = 300f
        private const val MAX_HALLOWEEN = 60

        private val SPRING_LEAF_COLORS = intArrayOf(0xFFB7C5, 0xFF9EB5, 0xFFC1CC, 0xE91E63)
        private val AUTUMN_LEAF_COLORS = intArrayOf(0xE65100, 0xFF9800, 0xD84315, 0xFFC107)
        private val VALENTINE_HEART_COLORS = intArrayOf(0xFF4D88, 0xE91E63, 0xFF80AB, 0xFF1744)

        /** Screen X of the last touch, set by [setSnowPointerX]. */
        @Volatile
        @JvmField
        var pointerX = -1f
    }
}

fun setSnowPointerX(x: Float) {
    Snowfall.pointerX = x
}

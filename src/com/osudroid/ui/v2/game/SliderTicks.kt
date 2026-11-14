package com.osudroid.ui.v2.game

import com.edlplan.framework.easing.Easing
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.sliderobject.SliderTick
import com.osudroid.utils.IPoolable
import com.osudroid.utils.SynchronizedPool
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.container.*
import com.reco1l.andengine.sprite.*
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.GameHelper

class SliderTickContainer : UIContainer() {
    private var slider: Slider? = null

    fun init(beatmapSlider: Slider) {
        slider = beatmapSlider

        detachChildren()

        val position = beatmapSlider.screenSpaceGameplayStackedPosition

        setPosition(position.x, position.y)

        for (i in 1 until beatmapSlider.nestedHitObjects.size) {

            val tick = beatmapSlider.nestedHitObjects[i] as? SliderTick ?: break
            val tickPosition = tick.screenSpaceGameplayStackedPosition

            val sprite = SliderTickSprite.obtain()

            // We're subtracting the position of the slider because the tick container is
            // already at the position of the slider since it's a child of the slider's body.
            sprite.setPosition(tickPosition.x - position.x, tickPosition.y - position.y)
            attachChild(sprite)

            sprite.init(beatmapSlider, tick)
        }
    }

    fun onNewSpan(newSpanIndex: Int) {
        if (slider == null) {
            return
        }

        val spanStartIndex =
            // Amount of slider ticks passed.
            newSpanIndex * childCount +
            // Amount of slider repeats passed.
            newSpanIndex +
            // The slider head.
            1

        for (i in spanStartIndex until spanStartIndex + childCount) {
            val tick = slider!!.nestedHitObjects[i] as? SliderTick ?: break

            // For reverse sliders, the ticks are in the opposite order.
            val sprite = getChild(
                if (newSpanIndex % 2 != 0) childCount - (i - spanStartIndex) - 1 else i - spanStartIndex
            ) as? SliderTickSprite ?: break

            sprite.init(slider!!, tick)
        }
    }

    override fun onDetached() {
        super.onDetached()
        detachChildren()
    }

}


class SliderTickSprite : UISprite(), IPoolable {
    override var isRecycled = false
    private var tick: SliderTick? = null

    init {
        textureRegion = ResourceManager.getInstance().getTexture("sliderscorepoint")
        origin = Anchor.Center
    }

    /**
     * Initializes this [SliderTickSprite] with the given [SliderTick].
     *
     * The tick pop-in mirrors danser-go: a tick only starts growing once the slider's
     * snake-in front reaches it, grows from 0.5x to 1.2x, then settles back to 1x with
     * an ease-out curve while fading in. Ticks on later spans are timed from the middle
     * of the span instead, so they pop when the ball travels back over them.
     *
     * @param slider The [Slider] that owns [tick].
     * @param tick The [SliderTick] represented by this [SliderTickSprite].
     */
    fun init(slider: Slider, tick: SliderTick) {
        this.tick = tick

        val sliderStartTime = slider.startTime
        val partLen = slider.spanDuration
        val preempt = slider.timePreempt
        val tickTime = tick.startTime

        val repeatProgress = if (partLen > 0.0) (tickTime - sliderStartTime) / partLen else 0.0

        var startTime: Double
        var endTime: Double

        if (repeatProgress < 1.0) {
            val snakeInStart = sliderStartTime - preempt
            val snakeInEnd = sliderStartTime - preempt * 2.0 / 3.0
            val normalStart = (tickTime - sliderStartTime) / 2.0 + snakeInEnd

            startTime = max(repeatProgress * (snakeInEnd - snakeInStart) + snakeInStart, normalStart)
            endTime = min(startTime + 150.0, tickTime - 36.0)
        } else {
            val spanStart = sliderStartTime + partLen * floor(repeatProgress)

            endTime = spanStart + (tickTime - spanStart) / 2.0
            startTime = endTime - 200.0
        }

        // A tick close to the slider's start can be given a negative animation window;
        // collapse it so the sprite simply appears at full scale.
        if (endTime < startTime) {
            endTime = startTime
        }

        val start = (startTime / 1000.0).toFloat()
        val peak = (endTime / 1000.0).toFloat()
        val hitTime = (tickTime / 1000.0).toFloat()

        clearEntityModifiers()

        alpha = 0f
        setScale(0.5f)

        val growDuration = (peak - start).coerceAtLeast(0f)

        if (growDuration > 0f) {
            beginAbsoluteSequence(start) {
                scaleTo(1.2f, growDuration)
                fadeIn(growDuration)
            }
        } else {
            alpha = 1f
            setScale(1.2f)
        }

        beginAbsoluteSequence(peak) {
            scaleTo(1f, ANIM_DURATION, Easing.OutQuad)
        }

        if (GameHelper.isHidden() && !GameHelper.getHidden().onlyFadeApproachCircles) {
            val fadeOutStartTime = max(peak, hitTime - 1f)

            beginAbsoluteSequence(fadeOutStartTime) {
                fadeTo(0f, hitTime - fadeOutStartTime)
            }
        }
    }

    /**
     * Called when the [SliderTick] that this [SliderTickSprite] represents is hit.
     *
     * @param isSuccessful Whether the hit resulted in a successful hit.
     */
    fun onHit(isSuccessful: Boolean) {
        val tick = tick ?: return

        clearEntityModifiers()

        beginAbsoluteSequence(tick.startTime.toFloat() / 1000) {
            fadeOut(ANIM_DURATION, Easing.OutQuint)

            if (isSuccessful) {
                scaleTo(1.5f, ANIM_DURATION, Easing.Out)
            }
        }

        this.tick = null
    }

    override fun onDetached() {
        super.onDetached()
        clearEntityModifiers()
        pool.release(this)
    }

    companion object {
        private const val ANIM_DURATION = 0.15f
        private val pool = SynchronizedPool<SliderTickSprite>(20).apply { release(SliderTickSprite()) }

        /**
         * Renews the [SliderTickSprite] pool with fresh instances.
         *
         * @param size The number of [SliderTickSprite] instances to pre-populate the pool with.
         */
        @JvmStatic
        fun renew(size: Int) {
            pool.clear()
            repeat(size) { pool.release(SliderTickSprite()) }
        }

        /**
         * Obtains a [SliderTickSprite] from the pool, or creates a new one if the pool is empty.
         */
        @JvmStatic
        fun obtain(): SliderTickSprite {
            val sprite = pool.acquire() ?: return SliderTickSprite()
            sprite.textureRegion = ResourceManager.getInstance().getTexture("sliderscorepoint")
            return sprite
        }
    }

}

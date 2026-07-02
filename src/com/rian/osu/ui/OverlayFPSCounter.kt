package com.rian.osu.ui

import org.anddev.andengine.entity.Entity
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osuplusplus.ResourceManager

/**
 * A global FPS counter that attaches to the UIEngine overlay and is visible across all scenes.
 * It reads [Config.isShowFPS] to toggle visibility, so the user can enable/disable it in settings.
 *
 * The counter fades out smoothly when FPS values remain unchanged for [FADE_STABLE_THRESHOLD] seconds,
 * and fades back in when values change again.
 */
class OverlayFPSCounter : Entity() {

    private val fpsCounter = FPSCounter(ResourceManager.getInstance().getFont("smallFont"))
    private var lastUpdateTime = 0L

    /** The last recorded FPS value, used to detect changes. */
    private var lastFps = 0f

    /** Timer counting how long the FPS has remained unchanged. */
    private var fpsStableTimer = 0f

    /** How many seconds of unchanged FPS before fading out. */
    private val FADE_STABLE_THRESHOLD = 3f

    /** Speed of the fade in/out transition. */
    private val FADE_SPEED = 3f

    init {
        fpsCounter.setPosition(
            Config.getRES_WIDTH() - fpsCounter.getWidthScaled() - 5,
            Config.getRES_HEIGHT() - fpsCounter.getHeightScaled() - 10
        )
        attachChild(fpsCounter)
    }

    override fun onManagedUpdate(pSecondsElapsed: Float) {
        super.onManagedUpdate(pSecondsElapsed)

        // Respect the user's FPS visibility setting
        isVisible = Config.isShowFPS()

        if (!isVisible) return

        val currentTime = System.currentTimeMillis()
        if (lastUpdateTime == 0L) {
            lastUpdateTime = currentTime
            return
        }

        val deltaTime = (currentTime - lastUpdateTime) / 1000f
        lastUpdateTime = currentTime

        fpsCounter.updateFps(deltaTime)

        // Track whether the FPS value has changed
        val currentFps = fpsCounter.fps
        if (currentFps != lastFps) {
            lastFps = currentFps
            fpsStableTimer = 0f
        } else {
            fpsStableTimer += pSecondsElapsed
        }

        // Fade out when stable, fade in when changing
        if (fpsStableTimer >= FADE_STABLE_THRESHOLD) {
            alpha -= pSecondsElapsed * FADE_SPEED
            if (alpha < 0f) alpha = 0f
        } else {
            alpha += pSecondsElapsed * FADE_SPEED
            if (alpha > 1f) alpha = 1f
        }
    }
}

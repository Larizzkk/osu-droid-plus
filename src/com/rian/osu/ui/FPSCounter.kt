package com.rian.osu.ui

import com.rian.osu.math.Interpolation.dampContinuously
import kotlin.math.min
import kotlin.math.roundToInt
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.opengl.font.Font
import ru.nsu.ccfit.zuev.osuplusplus.GlobalManager.getInstance as getGlobal
import java.util.Locale
import javax.microedition.khronos.opengles.GL10

class FPSCounter(font: Font) : ChangeableText(
    0f, 0f, font, "999/999 FPS - 99.9ms", 24
) {
    private val backgroundRect = Rectangle(0f, 0f, 1f, 1f).apply {
        setColor(0f, 0f, 0f, 0.5f)
    }
    private val backgroundPadding = 6f

    @get:JvmName("getFPS")
    var fps = 0f; private set

    @get:JvmName("getMaximumFPS")
    var maximumFps = 0f; private set(value) { field = value; forceUpdate = true }

    @get:JvmName("getAverageFPS")
    var averageFps = 0f; private set

    private val spikeTime = 0.02f
    private val dampTime = 0.1f
    private var timeUntilNextAverageFpsCalculation = 0f
    private var timeSinceLastAverageFpsCalculation = 0f
    private var framesSinceLastAverageFpsCalculation = 0
    private val averageFpsCalculationInterval = 0.25f

    fun updateFps(deltaTime: Float) {
        if (timeUntilNextAverageFpsCalculation <= 0) {
            timeUntilNextAverageFpsCalculation += averageFpsCalculationInterval
            averageFps = if (framesSinceLastAverageFpsCalculation == 0) 0f
                        else framesSinceLastAverageFpsCalculation / timeSinceLastAverageFpsCalculation
            timeSinceLastAverageFpsCalculation = 0f
            framesSinceLastAverageFpsCalculation = 0
            maximumFps = getGlobal().mainActivity.refreshRate
        }
        framesSinceLastAverageFpsCalculation++
        timeUntilNextAverageFpsCalculation -= deltaTime
        timeSinceLastAverageFpsCalculation += deltaTime
        val hasSpike = fps > 1 / spikeTime && deltaTime > spikeTime
        fps = min(maximumFps, if (hasSpike) 1 / deltaTime else dampContinuously(fps, averageFps, dampTime, deltaTime))
    }

    private var lastDisplayedFps = 0
    private var forceUpdate = false
    private var timeSinceLastUpdate = 0f
    private val updateInterval = 0.1f

    override fun onManagedUpdate(pSecondsElapsed: Float) {
        super.onManagedUpdate(pSecondsElapsed)
        if (pSecondsElapsed > 10) return
        timeSinceLastUpdate += pSecondsElapsed
        if (!forceUpdate && timeSinceLastUpdate < updateInterval) return
        forceUpdate = false
        timeSinceLastUpdate = 0f
        val displayedFps = fps.roundToInt()
        if (!forceUpdate && displayedFps == lastDisplayedFps) return
        lastDisplayedFps = displayedFps
        val frameTimeMs = if (fps > 0f) (1000f / fps) else 0f
        text = String.format(Locale.US, "%d/%d FPS - %.1fms", displayedFps, maximumFps.roundToInt(), frameTimeMs)
        updateBackground()
        updateColor()
    }

    private fun updateBackground() {
        backgroundRect.setSize(width + backgroundPadding * 2, height + backgroundPadding * 2)
        backgroundRect.setPosition(-backgroundPadding, -backgroundPadding)
    }

    private fun updateColor() {
        val r: Float; val g: Float; val b: Float
        when {
            fps < 30f -> { r = 1f; g = 0f; b = 0f }
            fps < 50f -> { r = 1f; g = 0.84f; b = 0f }
            else -> { r = 0f; g = 1f; b = 0f }
        }
        setColor(r, g, b)
    }

    override fun onManagedDraw(pGL: GL10, pCamera: Camera) {
        pGL.glPushMatrix()
        this.onApplyTransformations(pGL, pCamera)
        if (backgroundRect.isVisible) backgroundRect.onDraw(pGL, pCamera)
        this.doDraw(pGL, pCamera)
        pGL.glPopMatrix()
    }

    override fun reset() {
        super.reset()
        timeUntilNextAverageFpsCalculation = 0f
        timeSinceLastAverageFpsCalculation = 0f
        framesSinceLastAverageFpsCalculation = 0
        fps = 0f; averageFps = 0f; maximumFps = 0f
        lastDisplayedFps = 0; forceUpdate = false; timeSinceLastUpdate = 0f
    }
}

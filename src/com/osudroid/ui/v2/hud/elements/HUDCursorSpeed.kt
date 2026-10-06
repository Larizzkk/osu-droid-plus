package com.osudroid.ui.v2.hud.elements

import android.graphics.PointF
import android.util.DisplayMetrics
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osuplusplus.GlobalManager
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * HUD element that shows how fast the cursor moves, in whole cm/s.
 *
 * The reading comes straight from the touch events the engine already collects:
 * [com.osudroid.game.CursorEvent.systemTime] is the real timestamp of the event,
 * so the time between two cursor positions is measured instead of guessed from
 * the frame length. Every new event therefore updates the displayed value in the
 * same frame it arrived — there is no smoothing window, no averaging and no lag.
 *
 * The value is the speed of the current movement burst: the distance and the time
 * are accumulated while the cursor keeps moving and the accumulator is dropped as
 * soon as the finger rests, so lifting and putting the finger down again starts
 * a fresh burst instead of reporting a jump.
 *
 * Under autoplay there is no finger to read, so the auto cursor is sampled with
 * the frame length instead.
 */
class HUDCursorSpeed : HUDStatisticCounter("Speed") {

    override val name = "Cursor speed"

    /** Screen pixels per centimetre, from the display DPI. */
    private var pxPerCm = -1f

    // ── Movement burst being measured ──
    private var burstDistancePx = 0f
    private var burstSeconds = 0f

    // ── Previous sample ──
    private var prevX = 0f
    private var prevY = 0f
    private var prevSampleTime = -1L
    private var hasPrevSample = false

    // ── Readings, whole cm/s ──
    private var speed = 0
    private var peak = 0

    /** The scene the samples belong to. */
    private var trackedScene: GameScene? = null

    /** Song position of the last sample, to notice a retry on the same scene. */
    private var trackedSongPosition = -1f

    init {
        updateText()
    }

    override fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float) {
        resolveScale()

        // A new map, or the same map started over after a retry: every reading
        // belongs to one attempt, so all of them start from zero again.
        if (trackedScene !== gameScene || isRestart(gameScene)) {
            startNewAttempt(gameScene)
            return
        }

        if (GameHelper.isAutoplay()) {
            sampleAutoCursor(gameScene, secondsElapsed)
        } else {
            sampleTouch(gameScene)
        }

        updateText()
    }

    override fun onSeek() {
        // Seeking moves the playfield (and the auto cursor) discontinuously,
        // so the next distance would be a jump instead of a movement.
        startNewAttempt(trackedScene ?: return)
    }

    /**
     * Reads the newest touch event of the cursor.
     *
     * `getLatestEvent()` returns the same object until a new event arrives, so
     * the timestamp is what tells a fresh sample from the one already measured.
     */
    private fun sampleTouch(gameScene: GameScene) {
        val cursor = gameScene.getCursor(0) ?: run {
            dropSample()
            return
        }

        val event = cursor.getLatestEvent() ?: run {
            dropSample()
            return
        }

        val position = event.position ?: run {
            dropSample()
            return
        }

        val time = event.systemTime

        if (!hasPrevSample || time <= prevSampleTime) {
            // First event of a gesture, or a recycled event object.
            prevX = position.x
            prevY = position.y
            prevSampleTime = time
            hasPrevSample = true
            burstDistancePx = 0f
            burstSeconds = 0f
            return
        }

        val deltaSeconds = (time - prevSampleTime) / 1000f
        val distance = distanceTo(position.x, position.y)

        prevX = position.x
        prevY = position.y
        prevSampleTime = time

        // A long gap means the finger rested: the previous burst says nothing
        // about how fast the cursor moves now.
        if (deltaSeconds > IDLE_GAP_SECONDS) {
            burstDistancePx = distance
            burstSeconds = deltaSeconds
        } else {
            burstDistancePx += distance
            burstSeconds += deltaSeconds
        }

        publish()
    }

    /** Autoplay has no touch events, so the auto cursor is sampled per frame. */
    private fun sampleAutoCursor(gameScene: GameScene, secondsElapsed: Float) {
        val autoCursor = gameScene.autoCursor ?: run {
            dropSample()
            return
        }

        if (secondsElapsed <= MIN_FRAME_SECONDS || secondsElapsed >= MAX_FRAME_SECONDS) {
            return
        }

        val distance = distanceTo(autoCursor.x, autoCursor.y)

        prevX = autoCursor.x
        prevY = autoCursor.y
        hasPrevSample = true

        if (burstSeconds == 0f) {
            burstDistancePx = distance
            burstSeconds = secondsElapsed
        } else {
            burstDistancePx += distance
            burstSeconds += secondsElapsed
        }

        publish()
    }

    /** Turns the accumulated burst into a whole-number reading. */
    private fun publish() {
        if (burstSeconds <= 0f) {
            speed = 0
            return
        }

        speed = (burstDistancePx / pxPerCm / burstSeconds).roundToInt()

        if (speed > peak) {
            peak = speed
        }
    }

    /** The cursor produced nothing this frame; stop reporting a stale burst. */
    private fun dropSample() {
        hasPrevSample = false
        burstDistancePx = 0f
        burstSeconds = 0f
        speed = 0
    }

    private fun distanceTo(x: Float, y: Float): Float {
        val dx = x - prevX
        val dy = y - prevY
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Whether the song position went backwards, which is what a retry on the
     * same [GameScene] looks like from here.
     */
    private fun isRestart(gameScene: GameScene): Boolean {
        val position = gameScene.elapsedTime
        val restarted = trackedSongPosition >= 0f && position < trackedSongPosition

        trackedSongPosition = position

        return restarted
    }

    private fun startNewAttempt(gameScene: GameScene) {
        trackedScene = gameScene
        trackedSongPosition = gameScene.elapsedTime

        prevX = 0f
        prevY = 0f
        prevSampleTime = -1L
        hasPrevSample = false
        burstDistancePx = 0f
        burstSeconds = 0f
        speed = 0
        peak = 0

        updateText()
    }

    private fun resolveScale() {
        if (pxPerCm >= 0f) return

        pxPerCm = try {
            val metrics = DisplayMetrics()
            GlobalManager.getInstance().mainActivity
                ?.windowManager?.defaultDisplay?.getMetrics(metrics)

            // densityDpi is pixels per inch and an inch is 2.54 cm.
            (metrics.densityDpi / 2.54f).takeIf { it > 0f } ?: FALLBACK_PX_PER_CM
        } catch (_: Exception) {
            FALLBACK_PX_PER_CM
        }
    }

    private fun updateText() {
        valueText.text = "$speed / $peak"
    }

    private companion object {
        /** A gap longer than this means the finger rested between two samples. */
        const val IDLE_GAP_SECONDS = 0.08f

        /** Frames shorter than this (~240 fps) carry no usable distance. */
        const val MIN_FRAME_SECONDS = 1f / 240f

        /** A frame this long is a hitch, not movement. */
        const val MAX_FRAME_SECONDS = 0.1f

        /** ~mdpi, used when the display metrics cannot be read. */
        const val FALLBACK_PX_PER_CM = 160f / 2.54f
    }
}
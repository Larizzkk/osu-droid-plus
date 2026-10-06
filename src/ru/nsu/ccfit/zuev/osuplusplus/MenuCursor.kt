package ru.nsu.ccfit.zuev.osuplusplus

import android.util.Log
import android.view.MotionEvent
import com.osudroid.ui.v2.GameLoaderScene
import com.reco1l.andengine.UIEngine
import com.reco1l.andengine.convertSurfaceToSceneCoordinates
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osuplusplus.game.cursor.main.CursorEntity

/**
 * A global skin cursor for every menu scene, rendered exactly like the gameplay
 * cursors (same [CursorEntity]: skin sprite, press animation and the very same
 * trail — see [CursorEntity.attachToScene], which attaches the trail to the
 * scene together with the cursor).
 *
 * The entity lives on the engine's overlay HUD, which is drawn on top of the main
 * scene and all child scenes (fragments, dialogs), so a single instance covers all
 * AndEngine menu scenes. It is hidden whenever gameplay is active (GameLoaderScene
 * or the GameScene's own playfield scene) — gameplay draws its own per-pointer
 * cursors — and whenever the "showcursor" preference is disabled.
 *
 * Input is read from the same SPSC sample queues that feed gameplay
 * (DirectInputSurfaceView): every update tick drains all queued samples, keeps the
 * latest position per pointer and derives press/release from the recorded actions.
 * The queues are only drained while gameplay is inactive, so gameplay input is
 * never stolen.
 */
class MenuCursor(private val engine: UIEngine) {

    companion object {
        /**
         * Number of pointer slots mirrored from DirectInputSurfaceView
         * (SAMPLE_POINTER_SLOTS). Android pointer IDs are small sequential ints.
         */
        private const val MAX_SLOTS = 10
    }

    private var cursor: CursorEntity? = null
    private var skinGeneration = -1

    /** Index of the pointer the cursor currently follows, -1 when idle. */
    private var activePointer = -1

    /** True while gameplay owns the input queues (drain is suspended). */
    private var drainSuspended = false

    // Per-pointer cached state: a stationary finger emits no new samples, so the
    // down state and last position must persist across ticks.
    private val pointerDown = BooleanArray(MAX_SLOTS)
    private val pointerX = FloatArray(MAX_SLOTS)
    private val pointerY = FloatArray(MAX_SLOTS)
    private val pointerPressedThisTick = BooleanArray(MAX_SLOTS)

    // Scratch buffers for the sample drain, allocated once (no per-tick GC).
    private val sampleCoords = FloatArray(4)
    private val sampleTime = LongArray(1)
    private val sampleAction = IntArray(1)

    // Scratch buffer for the surface → scene conversion (allocation free).
    private val sceneCoordinates = FloatArray(2)

    // Coordinate diagnostics throttle (drag → logcat: adb logcat -s MenuCursor CursorCoord).
    private var lastCoordLogMs = 0L

    fun update(deltaSeconds: Float) {
        val entity = cursor

        // Skin hot-swap: rebuild the sprite/trail textures in place when the
        // skin generation changes (same pattern as UI scenes).
        if (entity != null && skinGeneration != ResourceManager.getSkinGeneration()) {
            skinGeneration = ResourceManager.getSkinGeneration()
            entity.refreshSkinTextures()
        }

        // Lazy creation: the constructor needs the "cursor" texture, which is only
        // available after the initial skin load.
        val cursorEntity = entity ?: createCursor()?.also { cursor = it } ?: return

        // Gameplay draws its own cursors — never double-draw, and never drain the
        // input queues while gameplay is active (GameScene consumes them itself).
        if (isGameplayActive()) {
            if (!drainSuspended) {
                drainSuspended = true
                resetInput()
                cursorEntity.setShowing(false)
                cursorEntity.resetTrail()
            }
            // Still tick while hidden so the ghost/trail points age out instead of
            // popping back in on the next menu frame.
            cursorEntity.update(deltaSeconds)
            return
        }

        if (drainSuspended) {
            // Coming back from gameplay: start from a clean slate (any pointer
            // state cached before the transition is stale by definition).
            drainSuspended = false
            resetInput()
        }

        // Drained even while hidden so the queues never accumulate stale samples
        // in menus and the cached pointer state stays current.
        drainSamples()

        // Read the preference live so the toggle applies immediately in menus.
        if (!Config.getBoolean("showcursor", false)) {
            if (cursorEntity.isVisible) {
                cursorEntity.setShowing(false)
            }
            cursorEntity.update(deltaSeconds)
            return
        }

        trackPointer(cursorEntity, deltaSeconds)

        // Always update: while hidden this ages the remaining trail points out,
        // while showing it drives the press animation and the trail.
        cursorEntity.update(deltaSeconds)
    }

    /**
     * Whether gameplay currently owns the screen (and the input queues).
     *
     * GameScene is not an AndEngine scene — during a play the engine's scene is
     * either the [GameLoaderScene] (loading) or the playfield UIScene that
     * GameScene builds in `startGame()` and hands to `engine.setScene(...)`.
     */
    private fun isGameplayActive(): Boolean {
        val activeScene = engine.scene ?: return false

        if (activeScene is GameLoaderScene) {
            return true
        }

        val gameplayScene = GlobalManager.getInstance().gameScene?.scene ?: return false
        return activeScene === gameplayScene
    }

    private fun resetInput() {
        activePointer = -1
        pointerDown.fill(false)
        pointerPressedThisTick.fill(false)
    }

    private fun createCursor(): CursorEntity? {
        val textureManager = ResourceManager.getInstance()
        val cursorTex = textureManager.getTextureIfLoaded("cursor")
        if (cursorTex == null) {
            return null
        }

        skinGeneration = ResourceManager.getSkinGeneration()

        val entity = CursorEntity()
        // Attaches the ghost trail + trail batch + cursor in render order, so the
        // motion blur and the ribbon actually draw (attaching only the entity
        // leaves those siblings outside the scene graph, i.e. never rendered).
        entity.attachToScene(engine.overlay)
        return entity
    }

    private fun drainSamples() {
        val view = GlobalManager.getInstance().mainActivity.directInputSurface ?: return

        java.util.Arrays.fill(pointerPressedThisTick, false)

        for (pointerId in 0 until view.maxSampleSlots) {
            while (view.popPointerSample(pointerId, sampleCoords, sampleTime, sampleAction)) {
                pointerX[pointerId] = sampleCoords[0]
                pointerY[pointerId] = sampleCoords[1]
                pointerDown[pointerId] = sampleCoords[3] > 0f
                if (sampleAction[0] == MotionEvent.ACTION_DOWN) {
                    pointerPressedThisTick[pointerId] = true
                }
            }
        }
    }

    private fun trackPointer(entity: CursorEntity, deltaSeconds: Float) {
        // Keep following the current pointer while it is down; otherwise switch to
        // the first pointer that is down (multi-touch).
        if (activePointer !in pointerDown.indices || !pointerDown[activePointer]) {
            activePointer = pointerDown.indexOfFirst { it }
        }

        if (activePointer < 0) {
            if (entity.isVisible) {
                // Marks an input discontinuity: the trail fades out from where the
                // finger was lifted instead of interpolating to the next press.
                entity.setShowing(false)
            }
            return
        }

        // Samples are recorded in surface pixels while the scene runs in the design
        // resolution (e.g. 2400x1080 surface vs 1280x720 scene), so the position has
        // to be projected into scene space before it can move an entity.
        val camera = engine.camera

        if (camera.surfaceWidth <= 0 || camera.surfaceHeight <= 0) {
            return
        }

        sceneCoordinates[0] = pointerX[activePointer]
        sceneCoordinates[1] = pointerY[activePointer]
        camera.convertSurfaceToSceneCoordinates(sceneCoordinates)

        var x = sceneCoordinates[0].coerceIn(0f, Config.getRES_WIDTH().toFloat())
        var y = sceneCoordinates[1].coerceIn(0f, Config.getRES_HEIGHT().toFloat())

        // The HUD is shifted together with the scene while a text input has focus
        // (software keyboard) — compensate so the cursor stays under the finger.
        x -= engine.overlay.x
        y -= engine.overlay.y

        entity.setPosition(x, y)

        val now = System.currentTimeMillis()
        if (now - lastCoordLogMs >= 500) {
            lastCoordLogMs = now
            Log.d(
                "MenuCursor",
                "raw=(${pointerX[activePointer]},${pointerY[activePointer]}) " +
                    "converted=(${sceneCoordinates[0]},${sceneCoordinates[1]}) " +
                    "pos=($x,$y) overlay=(${engine.overlay.x},${engine.overlay.y}) " +
                    "cam=[${camera.minX}..${camera.maxX}]x[${camera.minY}..${camera.maxY}] " +
                    "surf=${camera.surfaceWidth}x${camera.surfaceHeight} " +
                    "parent=${entity.parent?.javaClass?.simpleName}"
            )
        }

        if (!entity.isVisible) {
            // Fresh press: restart the trail instead of interpolating across the
            // gap (mirrors the gameplay DOWN handling).
            entity.setShowing(true)
            entity.onCursorPress()
        }

        if (pointerPressedThisTick[activePointer]) {
            entity.onCursorPress()
            entity.click()
        }

        entity.updateTrailFromMovement(deltaSeconds)
    }
}

package ru.nsu.ccfit.zuev.osuplusplus

import com.osudroid.ui.v2.GameLoaderScene
import com.reco1l.andengine.UIEngine
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorEntity

/**
 * Legacy-style skin cursor for every menu scene, ported from the legacy
 * `MenuCursor` but built on this port's input pipeline (AndEngine touch events
 * dispatched to the HUD, which already delivers scene coordinates).
 *
 * Known legacy bugs that are deliberately avoided here:
 *  - the cursor is never attached twice and is re-attached when the menu scene
 *    changes, so a scene switch cannot leave it on a detached parent;
 *  - the position is converted from scene coordinates and compensated for the
 *    scene offset (software keyboard), so it stays under the finger;
 *  - a fresh press always marks an input discontinuity (`onCursorPress`) instead
 *    of interpolating the trail from the previous tap;
 *  - it is hidden (and its trail aged out) during gameplay, which draws its own
 *    cursors; menu input is never stolen because the touch handler only observes.
 */
class MenuCursor(private val engine: UIEngine) {

    private var entity: CursorEntity? = null
    private var attachedScene: org.anddev.andengine.entity.scene.Scene? = null
    private var creationFailed = false

    /**
     * Pointer followed by the cursor, -1 when idle. Android pointer ids are not
     * always 0 (the second finger gets 1), so the id has to be tracked instead of
     * assuming the primary pointer.
     */
    private var activePointerId = -1

    private var pointerDown = false
    private var pointerX = 0f
    private var pointerY = 0f
    private var pressedThisFrame = false

    /** True while gameplay owns the screen (and draws its own cursors). */
    private var gameplayActive = false

    // Config is backed by SharedPreferences, so the "showcursor" key is re-read on
    // a timer instead of every frame. CursorEntity refreshes its own trail settings
    // on the same kind of throttle.
    private var settingsTimer = 0f
    private var showCursor = Config.getBoolean("showcursor", false)

    /** Observes a scene-space touch event; never consumes it. */
    fun onTouch(event: TouchEvent) {
        val pointerId = event.pointerID

        when {
            event.isActionDown -> {
                // Follow the first pointer that touches; a second finger does not
                // steal the cursor while the first one is held.
                if (activePointerId == -1) {
                    activePointerId = pointerId
                }
                if (pointerId != activePointerId) {
                    return
                }
                pointerX = event.x
                pointerY = event.y
                pointerDown = true
                pressedThisFrame = true
            }

            event.isActionMove -> {
                if (pointerId != activePointerId) {
                    return
                }
                pointerX = event.x
                pointerY = event.y
            }

            event.isActionUp || event.isActionCancel || event.isActionOutside -> {
                if (pointerId != activePointerId) {
                    return
                }
                pointerDown = false
                activePointerId = -1
            }
        }
    }

    fun update(deltaSeconds: Float) {
        settingsTimer += deltaSeconds
        if (settingsTimer >= 0.5f) {
            settingsTimer = 0f
            showCursor = Config.getBoolean("showcursor", false)
        }

        val nowGameplay = isGameplayActive()

        if (nowGameplay) {
            if (!gameplayActive) {
                gameplayActive = true
                pointerDown = false
                activePointerId = -1
                entity?.setShowing(false)
                entity?.resetTrail()
            }
            entity?.update(deltaSeconds)
            pressedThisFrame = false
            return
        }

        if (gameplayActive) {
            // Returning from gameplay: cached pointer state is stale by definition.
            gameplayActive = false
            pointerDown = false
            activePointerId = -1
            pressedThisFrame = false
        }

        val activeScene = engine.scene
        if (activeScene == null) {
            pressedThisFrame = false
            return
        }

        // Nothing to drive and nothing to keep alive while the option is off.
        if (!showCursor) {
            entity?.let {
                if (it.isVisible) {
                    it.setShowing(false)
                }
                it.update(deltaSeconds)
            }
            attachedScene = null
            pressedThisFrame = false
            return
        }

        // Created once and cached: constructing a CursorEntity builds its sprite and
        // trail, and doing that on every frame leaks textures until the process dies.
        if (attachedScene !== activeScene) {
            val cursor = entity ?: createCursor() ?: run {
                pressedThisFrame = false
                return
            }
            cursor.attachToScene(activeScene)
            attachedScene = activeScene
            cursor.setShowing(false)
            cursor.resetTrail()
        }

        val cursor = entity ?: run {
            pressedThisFrame = false
            return
        }

        if (!pointerDown) {
            if (cursor.isVisible) {
                cursor.setShowing(false)
            }
            cursor.update(deltaSeconds)
            pressedThisFrame = false
            return
        }

        // Touch events are in camera (scene) space; remove the scene offset applied
        // while a text field has focus so the cursor stays under the finger.
        val scene = activeScene
        var x = pointerX - scene.x
        var y = pointerY - scene.y

        x = x.coerceIn(0f, Config.getRES_WIDTH().toFloat())
        y = y.coerceIn(0f, Config.getRES_HEIGHT().toFloat())

        cursor.setPosition(x, y)

        if (!cursor.isVisible) {
            cursor.setShowing(true)
            cursor.onCursorPress()
        }

        if (pressedThisFrame) {
            cursor.onCursorPress()
            cursor.click()
            pressedThisFrame = false
        }

        cursor.updateTrailFromMovement(deltaSeconds)
        cursor.update(deltaSeconds)
    }

    /**
     * Gameplay draws its own per-pointer cursors, so the menu cursor must stay
     * hidden there. During loading the engine scene is a [GameLoaderScene]; during
     * a play it is the playfield scene owned by [ru.nsu.ccfit.zuev.osu.game.GameScene].
     */
    private fun isGameplayActive(): Boolean {
        val activeScene = engine.scene ?: return false

        if (activeScene is GameLoaderScene) {
            return true
        }

        val gameplayScene = GlobalManager.getInstance().gameScene?.scene ?: return false
        return activeScene === gameplayScene
    }

    private fun createCursor(): CursorEntity? {
        if (creationFailed) {
            return null
        }

        val resources = ResourceManager.getInstance()

        // Wait for the whole skin to be loaded, not just its cursor.png: creating the
        // cursor mid-load lazily cached the built-in cursortrail and the skin's trail
        // never replaced it.
        if (!resources.isSkinLoadFinished || resources.getTextureIfLoaded("cursor") == null) {
            return null
        }

        val scene = engine.scene ?: return null

        return try {
            val cursor = CursorEntity()
            cursor.attachToScene(scene)
            attachedScene = scene
            entity = cursor
            cursor
        } catch (t: Throwable) {
            // A missing/broken skin texture must never take the menu down.
            creationFailed = true
            null
        }
    }
}

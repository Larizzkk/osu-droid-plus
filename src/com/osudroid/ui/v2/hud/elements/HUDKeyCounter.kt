package com.osudroid.ui.v2.hud.elements

import com.osudroid.ui.v2.SpriteFont
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.container.UILinearContainer
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.sprite.UISprite
import com.reco1l.framework.Color4
import com.osudroid.ui.v2.hud.HUDElement
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.skins.OsuSkin

/**
 * Key counter similar to the one used by osu-droid-spectator: one box per finger
 * (gameplay cursor), showing the amount of presses and lighting up while held.
 *
 * The box texture is the standard `inputoverlay-key` skin element — a custom skin
 * providing that file overrides it, otherwise the bundled default asset is used.
 * The press count is drawn with the skin's score digits (like other HUD counters).
 */
class HUDKeyCounter : HUDElement() {

    /** Boxes by cursor index, in creation order. */
    private val boxes = mutableMapOf<Int, KeyBox>()

    /** Previous pressed state per cursor, to detect press transitions. */
    private val lastPressed = mutableMapOf<Int, Boolean>()

    /**
     * Whether the counter must be driven by gameplay hits instead of touches.
     * Autoplay never touches the screen and Relax auto-hits without taps, so like
     * osu!stable (whose autoplay/relax playback feeds real key presses into the key
     * overlay) we synthesize presses from the hits themselves.
     */
    private val isSimulated
        get() = GameHelper.isAutoplay() || GameHelper.isRelax()

    /** Last known gameplay time in seconds, used to time simulated presses. */
    private var gameplayTime = 0f

    /** Simulated box -> time at which its press ends. */
    private val simulatedReleases = mutableMapOf<KeyBox, Float>()

    /**
     * Box held by an autoplay/relax slider or spinner hold, if any. Unlike the
     * short flash presses, this stays lit from slider head to slider end (and
     * spinner start to spinner end).
     */
    private var heldBox: KeyBox? = null

    /** Gameplay time at which the active hold releases; NaN while no end was requested. */
    private var holdReleaseAt = Float.NaN

    /** Alternates between the first two boxes the way stable alternates K1/K2. */
    private var simulatedToggle = false

    /** Time of the last simulated press, to throttle dense judgement streams. */
    private var lastSimulatedPressAt = Float.NEGATIVE_INFINITY


    /** Keys are stacked vertically, one per finger, like in osu-droid-spectator. */
    private val container = UILinearContainer().apply {
        orientation = Orientation.Vertical
        spacing = KEY_SPACING
    }


    init {
        attachChild(container)

        // Keep at least the first key visible so the element is shown in the HUD
        // editor and before any finger has touched the screen.
        getOrCreateBox(0)
    }


    override fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float) {
        gameplayTime = gameScene.elapsedTime

        if (isSimulated) {
            // A hold end requested by onKeyHoldEnd() is applied on the next tick, so
            // the object's final judgement — which GameScene fires in the same frame
            // — still sees the key as held and its flash press stays suppressed.
            val held = heldBox
            if (held != null && !holdReleaseAt.isNaN() && gameplayTime >= holdReleaseAt) {
                held.setPressed(false)
                heldBox = null
                holdReleaseAt = Float.NaN
            }

            // Stable-style simulated presses: release the boxes whose flash elapsed.
            if (simulatedReleases.isNotEmpty()) {
                val iterator = simulatedReleases.entries.iterator()

                while (iterator.hasNext()) {
                    val entry = iterator.next()

                    if (gameplayTime >= entry.value) {
                        entry.key.setPressed(false)
                        iterator.remove()
                    }
                }
            }
            return
        }

        for (i in 0 until gameScene.cursorsCount) {
            val pressed = gameScene.getCursor(i).mouseDown
            val wasPressed = lastPressed[i] ?: false

            if (pressed && !wasPressed) {
                getOrCreateBox(i)?.increment()
            }

            lastPressed[i] = pressed
            boxes[i]?.setPressed(pressed)
        }
    }

    override fun onNoteHit(statistics: StatisticV2) {
        if (!isSimulated) {
            return
        }

        // While a slider/spinner hold is active the key is down: slider ticks and
        // the spinner's final judgement must neither add presses nor release it.
        if (heldBox != null) {
            return
        }

        // Judged objects arrive through onCircleHit/onSliderHit/onSpinnerHit, which
        // autoplay DOES drive (its hit path skips registerAccuracy entirely — the
        // auto branch in GameplayHitCircle never calls it, so key presses fed from
        // onAccuracyRegister never fired under autoplay).
        if (gameplayTime - lastSimulatedPressAt < MIN_SIMULATED_PRESS_INTERVAL) {
            return
        }
        lastSimulatedPressAt = gameplayTime

        val box = getOrCreateBox(if (simulatedToggle) 1 else 0) ?: return
        simulatedToggle = !simulatedToggle

        box.increment()
        box.setPressed(true)
        simulatedReleases[box] = gameplayTime + SIMULATED_PRESS_DURATION
    }

    override fun onKeyHoldStart() {
        if (!isSimulated) {
            return
        }

        if (heldBox != null) {
            // Either the object repeats its start event, or the previous object's
            // release was scheduled this frame but not applied yet (objects back to
            // back). Either way the key stays continuously lit: drop the release.
            holdReleaseAt = Float.NaN
            return
        }

        val box = getOrCreateBox(if (simulatedToggle) 1 else 0) ?: return
        simulatedToggle = !simulatedToggle

        box.increment()
        box.setPressed(true)
        // The hold supersedes any pending flash release of the same box.
        simulatedReleases.remove(box)

        heldBox = box
        holdReleaseAt = Float.NaN
    }

    override fun onKeyHoldEnd() {
        if (!isSimulated) {
            return
        }

        // Scheduling instead of releasing now keeps the box lit through the object's
        // final judgement (the classic spinner ends in onSpinnerEnd, then
        // onSpinnerHit runs), which must see the hold as active.
        if (heldBox != null && holdReleaseAt.isNaN()) {
            holdReleaseAt = gameplayTime
        }
    }

    override fun onSeek() {
        // Counts are built from live presses, they cannot be restored after a seek.
        boxes.values.forEach(KeyBox::resetCount)
        lastPressed.clear()

        simulatedReleases.clear()
        heldBox = null
        holdReleaseAt = Float.NaN
        simulatedToggle = false
        lastSimulatedPressAt = Float.NEGATIVE_INFINITY
    }


    /**
     * At most [MAX_KEYS] buttons are ever shown: extra fingers (and extra
     * simulated keys) do not create new boxes.
     */
    private fun getOrCreateBox(index: Int): KeyBox? {
        if (index >= MAX_KEYS) {
            return null
        }

        return boxes.getOrPut(index) {
            KeyBox().also(container::attachChild)
        }
    }


    private inner class KeyBox : UIContainer() {

        private val keySprite = UISprite(ResourceManager.getInstance().getTexture(KEY_TEXTURE))

        private val countText = SpriteFont(OsuSkin.get().scorePrefix).apply {
            text = "0"
            color = Color4.Black
            setTextureScale(COUNT_TEXT_SCALE)

            // Centers the count within the key box.
            anchor = Anchor.Center
            origin = Anchor.Center
        }

        private var presses = 0
        private var isPressed = false


        init {
            attachChild(keySprite)
            attachChild(countText)

            setSize(keySprite.width, keySprite.height)
            refresh()
        }

        fun increment() {
            countText.text = (++presses).toString()
        }

        fun setPressed(value: Boolean) {
            if (isPressed != value) {
                isPressed = value
                refresh()
            }
        }

        fun resetCount() {
            presses = 0
            countText.text = "0"
            setPressed(false)
        }

        private fun refresh() {
            keySprite.color = if (isPressed) PRESSED_COLOR else Color4.White
        }
    }


    companion object {
        /**
         * The standard osu! key counter texture, overridable by a skin.
         */
        private const val KEY_TEXTURE = "inputoverlay-key"

        private const val COUNT_TEXT_SCALE = 0.5f

        private const val KEY_SPACING = 6f

        /** Hard cap on how many key buttons the element ever shows. */
        private const val MAX_KEYS = 3

        /** How long a synthesized autoplay/relax press stays lit (seconds). */
        private const val SIMULATED_PRESS_DURATION = 0.06f

        /**
         * Minimum gap between synthesized presses: slider ticks/spinner rotations
         * report through the same path and would otherwise spin the counter.
         */
        private const val MIN_SIMULATED_PRESS_INTERVAL = 0.05f

        /**
         * Matches the highlight color osu-droid-spectator uses for held keys.
         */
        private val PRESSED_COLOR = Color4(0xFFFFEE00)
    }
}

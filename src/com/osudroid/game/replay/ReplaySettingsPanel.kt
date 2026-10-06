package com.osudroid.game.replay

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Axes
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.container.UILinearContainer
import com.reco1l.andengine.container.UIScrollableContainer
import com.reco1l.andengine.linearContainer
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.text
import com.reco1l.andengine.ui.UICard
import com.reco1l.framework.Color4
import com.reco1l.framework.math.Vec4
import org.anddev.andengine.entity.Entity
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osuplusplus.ResourceManager

/**
 * A panel that contains settings for replay playback. Can be expanded and collapsed by tapping the button on the left.
 *
 * The content is a vertical stack of cards exactly like the upstream (osu-droid) replay panel:
 * Playback, Visual settings, Skin and Movement — one collapsible UICard per section. The Movement
 * card is hidden while watching a replay (only meaningful for autoplay).
 */
class ReplaySettingsPanel : UIContainer() {
    lateinit var playbackControl: ReplayPlaybackControl
        private set

    lateinit var visualSettingsControl: ReplayVisualSettingsControl
        private set

    lateinit var skinControl: ReplaySkinControl
        private set

    lateinit var movementControl: ReplayMovementControl
        private set

    private var movementCard: UICard? = null

    private val isExpanded
        get() = elementContainer.isVisible

    private val elementContainer = UIScrollableContainer().apply {
        x = BUTTON_WIDTH

        scrollAxes = Axes.Y
        width = PANEL_WIDTH
        height = FillParent

        linearContainer {
            width = FillParent
            spacing = 20f
            padding = Vec4(0f, 20f)
            orientation = Orientation.Vertical

            addControls()
        }
    }

    init {
        x = PANEL_WIDTH
        height = FillParent
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        alpha = IDLE_ALPHA
        elementContainer.isVisible = false

        +ReplaySettingsPanelButton()
        +elementContainer
    }

    fun expand() {
        if (isExpanded) {
            return
        }

        elementContainer.isVisible = true

        clearEntityModifiers()
        moveToX(0f, 0.2f)
        fadeIn(0.2f)
    }

    fun collapse() {
        if (!isExpanded) {
            return
        }

        clearEntityModifiers()
        moveToX(PANEL_WIDTH, 0.2f)
        fadeTo(IDLE_ALPHA, 0.2f).after { elementContainer.isVisible = false }
    }

    private fun UILinearContainer.addControls() {
        playbackControl = ReplayPlaybackControl()
        visualSettingsControl = ReplayVisualSettingsControl()
        skinControl = ReplaySkinControl()
        movementControl = ReplayMovementControl()

        // Upstream layout: one collapsible card per section, stacked vertically.
        +playbackControl
        +visualSettingsControl
        +skinControl
        +movementControl.also { movementCard = it }
    }

    /**
     * The movement section only affects the AUTOPLAY cursor, so it is hidden when watching a
     * replay (the cursor belongs to the recording).
     */
    fun setMovementTabVisible(visible: Boolean) {
        (movementCard as? Entity)?.isVisible = visible
    }

    /** Java-friendly accessor for [visualSettingsControl] (its setter is private). */
    @JvmName("visualSettingsControlJava")
    fun getVisualSettingsControl(): ReplayVisualSettingsControl = visualSettingsControl

    /** Java-friendly accessor for [skinControl]. */
    @JvmName("skinControlJava")
    fun getSkinControl(): ReplaySkinControl = skinControl

    /** Java-friendly accessor for [movementControl]. */
    @JvmName("movementControlJava")
    fun getMovementControl(): ReplayMovementControl = movementControl

    private inner class ReplaySettingsPanelButton : UIContainer() {
        init {
            background = UIBox().apply {
                cornerRadius = BUTTON_RADIUS
                color = Color4(0xFF181825)
            }

            setSize(BUTTON_WIDTH, 150f)
            y = -125f

            anchor = Anchor.CenterLeft
            origin = Anchor.CenterLeft

            text {
                rotation = -90f
                anchor = Anchor.Center
                origin = Anchor.Center
                font = ResourceManager.getInstance().getFont("smallFont")
                text = "Settings"
            }
        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float) = when {
            event.isActionDown || event.isActionMove -> true

            event.isActionUp -> {
                if (isExpanded) {
                    collapse()
                } else {
                    expand()
                }

                false
            }

            else -> false
        }
    }

    companion object {
        private const val PANEL_WIDTH = 440f
        private const val BUTTON_WIDTH = 48f
        private const val BUTTON_RADIUS = 12f

        /**
         * Panel opacity while collapsed (upstream fades to this in [onLoadComplete]).
         */
        private const val IDLE_ALPHA = 0.5f
    }
}

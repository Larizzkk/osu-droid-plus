package com.osudroid.game.replay

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import org.anddev.andengine.input.touch.TouchEvent

class ReplaySettingsPanel : UILinearContainer() {
    val playbackControl = ReplayPlaybackControl()

    private val button = object : UILinearContainer() {
        init {
            setSize(BUTTON_WIDTH, 150f)
            y = -125f

            anchor = Anchor.CenterLeft
            origin = Anchor.CenterLeft
        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float) = when {
            event.isActionDown || event.isActionMove -> true
            event.isActionUp -> {
                if (elementContainer.isVisible) collapse() else expand()
                false
            }
            else -> false
        }
    }

    private val elementContainer = UILinearContainer().apply {
        x = BUTTON_WIDTH
        width = PANEL_WIDTH
        height = FillParent
        orientation = Orientation.Vertical
        spacing = 20f
        isVisible = false
    }

    init {
        x = PANEL_WIDTH
        height = FillParent
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        +button
        +elementContainer

        elementContainer.attachChild(playbackControl)
    }

    fun expand() {
        if (elementContainer.isVisible) return
        elementContainer.isVisible = true
        clearEntityModifiers()
        moveToX(0f, 0.2f)
    }

    fun collapse() {
        if (!elementContainer.isVisible) return
        clearEntityModifiers()
        moveToX(PANEL_WIDTH, 0.2f)
        elementContainer.isVisible = false
    }

    companion object {
        const val PANEL_WIDTH = 440f
        const val BUTTON_WIDTH = 48f
    }
}

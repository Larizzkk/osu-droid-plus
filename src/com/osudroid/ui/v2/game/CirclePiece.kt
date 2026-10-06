package com.osudroid.ui.v2.game
import ru.nsu.ccfit.zuev.osuplusplus.ResourceManager

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.sprite.*
import com.osudroid.ui.v2.SpriteFont
import com.reco1l.framework.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.skins.*

open class CirclePiece(

    circleTexture: String,
    overlayTexture: String

) : UIContainer() {


    init {
        origin = Anchor.Center
    }


    private val circle = UISprite().also {

        it.origin = Anchor.Center
        it.anchor = Anchor.Center
        it.textureRegion = ResourceManager.getInstance().getTexture(circleTexture)

        attachChild(it)
    }

    private val overlay = UISprite().also {

        it.origin = Anchor.Center
        it.anchor = Anchor.Center
        it.textureRegion = ResourceManager.getInstance().getTexture(overlayTexture)

        attachChild(it)
    }


    fun setCircleColor(red: Float, green: Float, blue: Float) {
        circle.setColor(red, green, blue)
    }

    fun setCircleColor(color: Color4) {
        circle.color = color
    }

    fun setCircleTextureRegion(circleTexture: String) {
        circle.textureRegion = ResourceManager.getInstance().getTexture(circleTexture)
    }

    fun setOverlayTextureRegion(overlayTexture: String) {
        overlay.textureRegion = ResourceManager.getInstance().getTexture(overlayTexture)
    }

    /**
     * Beat pulse (danser-go ScaleToTheBeat style): scales the circle and
     * overlay sprites around their own centers (anchor = Center), so the
     * result is independent of skin texture sizes or the container layout.
     *
     * Kept separate from [setScale] so the beatmap scale and the pulse
     * never fight each other.
     */
    open fun setPulseScale(scale: Float) {
        if (pulseScale == scale) return
        pulseScale = scale
        circle.setScale(scale)
        overlay.setScale(scale)
    }

    private var pulseScale = 1f

}

class NumberedCirclePiece(circleTexture: String, overlayTexture: String) : CirclePiece(circleTexture, overlayTexture) {


    private val number = SpriteFont(OsuSkin.get().hitCirclePrefix).also {

        it.origin = Anchor.Center
        it.anchor = Anchor.Center
        it.spacing = -OsuSkin.get().hitCircleOverlap

        attachChild(it)
    }


    fun setNumberText(value: Int) {
        number.text = value.toString()
    }

    fun setNumberScale(value: Float) {
        number.setTextureScale(value)
    }

    fun showNumber() {
        number.alpha = 1f
    }

    fun hideNumber() {
        number.alpha = 0f
    }

    /**
     * Re-pulls the number glyphs from the (possibly hot-swapped) skin and re-applies
     * the skin's spacing/scale. Called from [com.osudroid.ui.v2.game.GameplayHitCircle.refreshSkinTextures].
     */
    fun refreshNumberSkin() {
        number.onSkinChanged()
        number.spacing = -OsuSkin.get().hitCircleOverlap
    }

    /**
     * Pulses the combo number together with the circle body (danser-go pulses
     * hitCircle, hitCircleOverlay AND comboText as one group, see
     * `app/beatmap/objects/circle.go` where all three share the same transforms).
     */
    override fun setPulseScale(scale: Float) {
        super.setPulseScale(scale)
        number.setScale(scale)
    }

}
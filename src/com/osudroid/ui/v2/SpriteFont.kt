package com.osudroid.ui.v2

import com.reco1l.andengine.text.UITextureText
import com.reco1l.andengine.ui.ISkinnable
import org.anddev.andengine.opengl.texture.region.TextureRegion
import ru.nsu.ccfit.zuev.osuplusplus.ResourceManager
import ru.nsu.ccfit.zuev.skins.StringSkinData

open class SpriteFont(private val texturePrefix: StringSkinData) : UITextureText(mutableMapOf<Char, TextureRegion>().also {

    fun addChar(char: Char, textureName: String) {
        it[char] = ResourceManager.getInstance().getTextureWithPrefix(texturePrefix, textureName)
    }

    for (i in 0..9) {
        addChar('0' + i, i.toString())
    }

    addChar('.', "comma")
    addChar('%', "percent")
    addChar('x', "x")
    addChar('d', "d")
    addChar('p', "p")

}), ISkinnable {

    /**
     * Re-pulls every glyph from the (possibly hot-swapped) skin; glyphs would
     * otherwise keep the previous skin's (unloaded) GL textures after a mid-game
     * skin switch.
     */
    override fun onSkinChanged() {
        fun refreshChar(char: Char, textureName: String) {
            characters[char] = ResourceManager.getInstance().getTextureWithPrefix(texturePrefix, textureName)
        }

        for (i in 0..9) {
            refreshChar('0' + i, i.toString())
        }
        refreshChar('.', "comma")
        refreshChar('%', "percent")
        refreshChar('x', "x")
        refreshChar('d', "d")
        refreshChar('p', "p")
        onUpdateText()
    }

}
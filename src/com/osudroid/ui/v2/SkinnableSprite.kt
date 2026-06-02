package com.osudroid.ui.v2
import ru.nsu.ccfit.zuev.osuplusplus.ResourceManager

import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.ui.*
import ru.nsu.ccfit.zuev.osu.*

class OsuSkinnableSprite(val textureLookup: String) : UISprite(), ISkinnable {

    init {
        onSkinChanged()
    }

    override fun onSkinChanged() {
        textureRegion = ResourceManager.getInstance().getTexture(textureLookup)
    }

}
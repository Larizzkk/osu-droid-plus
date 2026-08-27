package com.osudroid.ui.v2.hud.elements

import com.osudroid.ui.v2.hud.HUDElement
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.sprite.UISprite
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osuplusplus.ResourceManager

class HUDGradeDisplay : HUDElement() {

    private val gradeSprite = UISprite()

    init {
        gradeSprite.origin = Anchor.Center
        gradeSprite.setScale(0.8f)
        attachChild(gradeSprite)
    }

    override fun onGameplayUpdate(game: GameScene, secondsElapsed: Float) {
        val mark = game.stat.mark ?: return
        val texture = ResourceManager.getInstance().getTexture("ranking-${mark}-small") ?: return
        gradeSprite.textureRegion = texture
    }

    override fun onBreakStateChange(isBreak: Boolean) {
        isChildrenVisible = !isBreak
    }
}

package com.osudroid.game.replay

import com.reco1l.andengine.ui.UICard

class ReplayPlaybackControl : UICard() {
    val rateControl = ReplayPlaybackRate()
    val seekControl = ReplayPlaybackSeek()

    init {
        width = FillParent
        title = "Playback"

        content.apply {
            +seekControl
            +rateControl
        }
    }
}

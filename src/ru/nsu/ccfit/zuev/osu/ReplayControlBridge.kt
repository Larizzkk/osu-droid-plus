package ru.nsu.ccfit.zuev.osu

import com.osudroid.game.replay.ReplaySettingsPanel
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osuplusplus.GlobalManager

object ReplayControlBridge {

    @JvmStatic
    fun wireReplayPanel(panel: ReplaySettingsPanel, scene: GameScene) {
        val seek = panel.playbackControl.seekControl
        seek.onSeek = { time -> scene.seekReplay(time) }
        seek.onPauseToggle = { paused ->
            if (paused) {
                ru.nsu.ccfit.zuev.osu.game.GameHelper.setSpeedMultiplier(0f)
                GlobalManager.getInstance().songService?.pause()
            } else {
                ru.nsu.ccfit.zuev.osu.game.GameHelper.setSpeedMultiplier(
                    panel.playbackControl.rateControl.rate
                )
                GlobalManager.getInstance().songService?.play()
            }
        }

        panel.playbackControl.rateControl.onValueChanged = { rate ->
            ru.nsu.ccfit.zuev.osu.game.GameHelper.setSpeedMultiplier(rate)
            GlobalManager.getInstance().songService?.setSpeed(rate)
        }
    }

    @JvmStatic
    fun updateSeekPosition(panel: ReplaySettingsPanel?, currentSec: Float, minSec: Float, maxSec: Float) {
        panel?.playbackControl?.seekControl?.updateSeekPosition(currentSec, minSec, maxSec)
    }
}

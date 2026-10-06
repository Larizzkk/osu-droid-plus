package ru.nsu.ccfit.zuev.osu

import com.osudroid.game.replay.ReplaySettingsPanel
import ru.nsu.ccfit.zuev.osu.game.GameScene

object ReplayControlBridge {

    @JvmStatic
    fun wireReplayPanel(panel: ReplaySettingsPanel, scene: GameScene) {
        val seek = panel.playbackControl.seekControl
        // Mirror osu-droid: seek must run on the update thread (scene graph is not thread-safe).
        seek.onSeek = { time ->
            com.osudroid.utils.updateThread { scene.seekReplay(time) }
        }
        seek.onPauseToggle = { paused ->
            if (paused) {
                scene.pauseReplayPlayback()
            } else {
                scene.resumeReplayPlayback()
            }
        }

        // Mirror osu-droid: the panel only stores the rate; the update loop applies
        // modRate * panelRate on every frame (GameScene.onReplayRateChanged).
        panel.playbackControl.rateControl.onValueChanged = { rate ->
            scene.onReplayRateChanged(rate)
        }

        // Wire visual settings control
        panel.visualSettingsControl.onBackgroundBrightnessChanged = { brightness ->
            scene.onReplayBrightnessChanged(brightness)
        }

        // Parallax toggle (recreated background picks up the new scale factor).
        panel.visualSettingsControl.onParallaxChanged = { enabled ->
            scene.onReplayParallaxChanged(enabled)
        }

        // Skin hot-swap: reload skin resources, then let the scene refresh textures
        // that were captured at construction (cursor, trails, ghost batch). The whole
        // result handling runs on the update thread (same rule as seek): the scene
        // refresh touches the graph, and the panel result mutates AndEngine UI state
        // (select value -> button text) which must not race the frame either.
        panel.skinControl.onSkinChanged = { path ->
            com.osudroid.ui.v1.SettingsFragment.loadSkinForGameplay(path) { success ->
                com.osudroid.utils.updateThread {
                    if (success) {
                        scene.onReplaySkinChanged()
                    }
                    panel.skinControl.onSkinLoadResult(path, success)
                    // Every scene sprite has been re-bound (or the load failed and
                    // nothing was retired); the old skin's GL textures can be freed.
                    ru.nsu.ccfit.zuev.osuplusplus.ResourceManager.flushRetiredSkinTextures()
                }
            }
        }

        // Autoplay movement style / sub-settings: re-bake the AutoCursor queue on the
        // update thread and resume from the current gameplay time. The scene method
        // routes through Execution.updateThread itself, so no extra hop here.
        panel.movementControl.onMovementStyleChanged = { style ->
            scene.onReplayMovementStyleChanged(style)
        }
    }

    @JvmStatic
    fun updateSeekPosition(panel: ReplaySettingsPanel?, currentSec: Float, minSec: Float, maxSec: Float) {
        panel?.playbackControl?.seekControl?.updateSeekPosition(currentSec, minSec, maxSec)
    }
}

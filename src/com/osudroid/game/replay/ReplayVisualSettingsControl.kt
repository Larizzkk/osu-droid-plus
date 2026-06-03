package com.osudroid.game.replay

import com.reco1l.andengine.ui.UICard
import com.reco1l.andengine.ui.form.FormSlider
import kotlin.math.roundToInt
import ru.nsu.ccfit.zuev.osu.Config

class ReplayVisualSettingsControl : UICard() {
    var defaultBackgroundBrightness = Config.getBackgroundBrightness()
        set(value) {
            field = value
            val sliderValue = value * 100
            brightnessSlider.defaultValue = sliderValue
            brightnessSlider.value = sliderValue
        }

    private val brightnessSlider = FormSlider(defaultBackgroundBrightness * 100).apply {
        label = "Background Brightness"
        control.min = 0f
        control.max = 100f
        valueFormatter = { "${it.roundToInt()}%"}
        onValueChanged = { onBackgroundBrightnessChanged?.invoke(it / 100f) }
    }

    var onBackgroundBrightnessChanged: ((Float) -> Unit)? = null

    init {
        width = FillParent
        title = "Visual Settings"
        content += brightnessSlider
    }
}

package com.osudroid.game.replay

import com.reco1l.andengine.ui.UICard
import com.reco1l.andengine.ui.form.FormCheckbox
import com.reco1l.andengine.ui.form.FormSlider
import kotlin.math.roundToInt
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.helper.StringTable

/**
 * Controls visual settings of gameplay when replaying.
 */
class ReplayVisualSettingsControl : UICard() {
    /**
     * The default background brightness.
     *
     * Note that setting this value will also override the control's value.
     */
    var defaultBackgroundBrightness = Config.getBackgroundBrightness()
        set(value) {
            field = value
            val sliderValue = value * 100

            brightnessSlider.defaultValue = sliderValue
            brightnessSlider.value = sliderValue
        }

    private val brightnessSlider = FormSlider(defaultBackgroundBrightness * 100).apply {
        label = StringTable.get(com.osudroid.resources.R.string.opt_bgbrightness_title)
        control.min = 0f
        control.max = 100f
        valueFormatter = { "${it.roundToInt()}%"}
        onValueChanged = { onBackgroundBrightnessChanged?.invoke(it / 100f) }
    }

    private val parallaxToggle = object : FormCheckbox(Config.isParallaxEnabled()) {
        override fun onControlValueChanged() {
            // Skip the very first invocation: FormControl fires onControlValueChanged
            // while the control tree is inside its constructor.
            if (!isInitialized) {
                isInitialized = control.value
                super.onControlValueChanged()
                return
            }
            Config.setParallaxEnabled(control.value)
            onParallaxChanged?.invoke(control.value)
            super.onControlValueChanged()
        }
    }.apply {
        label = "Parallax"
    }

    private var isInitialized = false

    /**
     * Called when the background brightness was changed. The argument is the background brightness (from 0 to 1).
     */
    var onBackgroundBrightnessChanged: ((Float) -> Unit)? = null

    /**
     * Called when the parallax toggle changed. The gameplay background has to be
     * rebuilt so the overscan factor applies immediately.
     */
    var onParallaxChanged: ((Boolean) -> Unit)? = null

    init {
        width = FillParent
        title = "Visual Settings"

        content += brightnessSlider
        content += parallaxToggle
    }
}
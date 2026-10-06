package com.osudroid.game.replay

import com.reco1l.andengine.component.UIComponent
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.container.UILinearContainer
import com.reco1l.andengine.ui.UICard
import com.reco1l.andengine.ui.form.FormCheckbox
import com.reco1l.andengine.ui.form.FormSlider
import kotlin.math.roundToInt
import ru.nsu.ccfit.zuev.osu.Config

/**
 * Visual settings card for replay/autoplay (background brightness + parallax). Part of the
 * upstream-style vertical card stack in [ReplaySettingsPanel]; skin picking lives in
 * [ReplaySkinControl] and movement in [ReplayMovementControl].
 */
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

    var onBackgroundBrightnessChanged: ((Float) -> Unit)? = null
    var onParallaxChanged: ((Boolean) -> Unit)? = null

    init {
        width = UIComponent.FillParent
        title = "Visual settings"

        content += brightnessSlider
        content += parallaxToggle
    }
}

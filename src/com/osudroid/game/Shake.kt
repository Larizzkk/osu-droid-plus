package com.osudroid.game

import com.edlplan.framework.easing.Easing
import com.reco1l.andengine.component.UIComponent
import com.rian.andengine.modifier.ModifierType

/** Duration of a single shake, in seconds (osu!lazer's value for hit circles and sliders). */
const val DEFAULT_SHAKE_DURATION = 0.03f

/**
 * Shakes this [UIComponent] horizontally, mirroring osu!lazer's `DrawableExtensions.Shake`.
 *
 * The shake animates [UIComponent.translationX], so it does not interfere with code that
 * positions the component itself. Calling this while the component is already shaking restarts
 * the animation from its resting translation.
 *
 * @param magnitude The displacement of the shake, in pixels.
 * @param shakeDuration The duration of a single shake, in seconds. The whole animation takes 4
 * times as long.
 */
@JvmOverloads
fun UIComponent.shake(magnitude: Float, shakeDuration: Float = DEFAULT_SHAKE_DURATION) {
    finishModifiers(false, ModifierType.TranslateX)

    beginModifierSequence {
        translateToX(magnitude, shakeDuration / 2, Easing.OutSine)
        then().translateToX(-magnitude, shakeDuration, Easing.InOutSine)
        then().translateToX(magnitude, shakeDuration, Easing.InOutSine)
        then().translateToX(-magnitude, shakeDuration, Easing.InOutSine)
        then().translateToX(0f, shakeDuration / 2, Easing.InSine)
    }
}

package com.osudroid.game.replay

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.component.UIComponent
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.container.UILinearContainer
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.ui.Theme
import com.reco1l.andengine.ui.UICard
import com.reco1l.andengine.ui.UITextButton
import com.reco1l.andengine.ui.form.FormCheckbox
import com.reco1l.andengine.ui.form.FormSlider
import com.reco1l.framework.math.Vec4
import com.rian.andengine.modifier.ModifierType
import kotlin.math.roundToInt
import ru.nsu.ccfit.zuev.osu.Config

/**
 * Autoplay movement card (style + per-style sub-settings) used by the in-game settings panel
 * during autoplay and replay playback.
 *
 * Style changes are applied live through [onMovementStyleChanged]; sub-settings are persisted
 * immediately and reported with a `null` style so the scene can re-bake the mover queue.
 * Only the current style's sub-settings are shown.
 */
class ReplayMovementControl : UICard() {

    private val movementStyles = arrayOf(
        "linear", "bezier", "spline", "circular", "axis",
        "exgon", "aggressive", "momentum", "pippi", "flower"
    )

    var appliedMovementStyle: String =
        Config.getString("autoplayStyle", "linear")
        private set

    /**
     * Called when the user changes the autoplay movement style or any sub-setting of the
     * current movement. The argument is `null` when only sub-settings were edited.
     */
    var onMovementStyleChanged: ((String?) -> Unit)? = null

    private val movementListContainer = pickerListContainer()

    private val movementSubSettingsContainer = UILinearContainer().apply {
        width = UIComponent.FillParent
        orientation = Orientation.Vertical
    }

    init {
        width = UIComponent.FillParent
        title = "Movement"

        content += movementListContainer
        content += movementSubSettingsContainer

        rebuildMovementList()
        rebuildMovementSubSettings()
    }

    fun refresh() {
        appliedMovementStyle = Config.getString("autoplayStyle", "linear")
        rebuildMovementList()
        rebuildMovementSubSettings()
    }

    private fun movementLabel(style: String): String =
        style.replaceFirstChar { it.uppercase() }

    private fun rebuildMovementList() {
        movementListContainer.detachChildren()
        movementStyles.forEach { style ->
            movementListContainer += pickerOption(movementLabel(style), style == appliedMovementStyle) {
                handleMovementPick(style)
            }
        }
    }

    private fun handleMovementPick(style: String) {
        if (style == appliedMovementStyle) {
            return
        }
        appliedMovementStyle = style
        rebuildMovementList()

        // Apply immediately on this (update) thread: the scene handler re-inits
        // the AutoCursor queue synchronously.
        onMovementStyleChanged?.invoke(style)
        rebuildMovementSubSettings()
    }

    private fun rebuildMovementSubSettings() {
        movementSubSettingsContainer.detachChildren()
        when (appliedMovementStyle) {
            "linear" -> {
                movementSubSettingsContainer += movementCheckbox("linearWaitForPreempt", "Wait for preempt", true)
                movementSubSettingsContainer += movementSlider("linearReactionTime", "Reaction time", 10f, 500f, 100f)
                movementSubSettingsContainer += movementCheckbox("linearChoppyLongObjects", "Choppy long objects", false)
            }
            "bezier" ->
                movementSubSettingsContainer += movementSlider("bezierAggressiveness", "Aggressiveness", 1f, 200f, 60f)
            "circular" -> {
                movementSubSettingsContainer += movementSlider("circularRadiusMultiplier", "Radius multiplier", 10f, 300f, 100f)
                movementSubSettingsContainer += movementSlider("circularStreamTrigger", "Stream trigger", 0f, 500f, 130f)
            }
            "spline" -> {
                movementSubSettingsContainer += movementCheckbox("splineRotationalForce", "Rotational force", false)
                movementSubSettingsContainer += movementCheckbox("splineStreamHalfCircle", "Stream half circles", true)
                movementSubSettingsContainer += movementCheckbox("splineStreamWobble", "Stream wobble", true)
                movementSubSettingsContainer += movementSlider("splineWobbleScale", "Wobble scale", 1f, 200f, 67f)
            }
            "momentum" -> {
                movementSubSettingsContainer += movementCheckbox("momentumSkipStackAngles", "Skip stacked angles", false)
                movementSubSettingsContainer += movementCheckbox("momentumStreamRestrict", "Restrict stream angle", true)
                movementSubSettingsContainer += movementSlider("momentumStreamMult", "Stream angle", -1000f, 1000f, 70f)
                movementSubSettingsContainer += movementSlider("momentumDurationTrigger", "Duration trigger", 0f, 4000f, 500f)
                movementSubSettingsContainer += movementSlider("momentumDurationMult", "Duration multiplier", 0f, 800f, 200f)
                movementSubSettingsContainer += movementSlider("momentumRestrictAngle", "Restrict angle", 0f, 180f, 90f)
                movementSubSettingsContainer += movementSlider("momentumRestrictArea", "Restrict area", 0f, 180f, 40f)
                movementSubSettingsContainer += movementCheckbox("momentumRestrictInvert", "Invert restrict", true)
                movementSubSettingsContainer += movementSlider("momentumDistanceMult", "Distance (in)", -400f, 400f, 60f)
                movementSubSettingsContainer += movementSlider("momentumDistanceMultOut", "Distance (out)", -400f, 400f, 45f)
            }
            "pippi" -> {
                movementSubSettingsContainer += movementSlider("pippiRotationSpeed", "Rotation speed", 10f, 600f, 160f)
                movementSubSettingsContainer += movementSlider("pippiRadiusMultiplier", "Radius multiplier", 0f, 100f, 98f)
                movementSubSettingsContainer += movementSlider("pippiSpinnerRadius", "Spinner radius", 0f, 200f, 100f)
            }
            "flower" -> {
                movementSubSettingsContainer += movementSlider("flowerAngleOffset", "Angle offset", 0f, 360f, 90f)
                movementSubSettingsContainer += movementSlider("flowerDistanceMult", "Distance multiplier", 0f, 300f, 67f)
                movementSubSettingsContainer += movementSlider("flowerStreamAngleOffset", "Stream angle offset", 0f, 360f, 90f)
                movementSubSettingsContainer += movementSlider("flowerLongJump", "Long jump threshold", -1f, 1000f, -1f)
                movementSubSettingsContainer += movementSlider("flowerLongJumpMult", "Long jump multiplier", 0f, 300f, 70f)
                movementSubSettingsContainer += movementCheckbox("flowerLongJumpOnEqualPos", "Long jump on equal position", false)
            }
            "exgon" ->
                movementSubSettingsContainer += movementSlider("exgonDelay", "Delay", 10f, 500f, 50f)
            // axis / aggressive: no configurable sub-settings.
        }
    }

    /**
     * Builds a slider bound to a config key. The value is persisted on every frame while the
     * thumb moves, but the queue re-bake is deferred to [FormSlider]' stop-dragging callback so
     * that a 500+ segment movement queue is not rebuilt each frame.
     */
    private fun movementSlider(key: String, label: String, min: Float, max: Float, def: Float): FormSlider {
        val current = (Config.getRaw(key) as? Number)?.toFloat() ?: def
        return FormSlider(current).apply {
            width = UIComponent.FillParent
            this.label = label
            defaultValue = def
            control.min = min
            control.max = max
            valueFormatter = { "${it.roundToInt()}" }
            onValueChanged = { raw ->
                Config.setInt(key, raw.roundToInt())
            }
            control.onStopDragging = {
                onMovementStyleChanged?.invoke(null)
            }
        }
    }

    private fun movementCheckbox(key: String, label: String, def: Boolean): FormCheckbox {
        val raw = Config.getRaw(key)
        val current = when (raw) {
            is Boolean -> raw
            is String -> raw == "1" || raw == "true"
            is Number -> raw.toInt() != 0
            else -> def
        }
        return FormCheckbox(current).apply {
            width = UIComponent.FillParent
            this.label = label
            defaultValue = def
            onValueChanged = { checked ->
                Config.setBoolean(key, checked)
                onMovementStyleChanged?.invoke(null)
            }
        }
    }

    private fun pickerListContainer(): UILinearContainer = UILinearContainer().apply {
        width = UIComponent.FillParent
        orientation = Orientation.Vertical
        spacing = 4f
        padding = Vec4(12f, 0f)
    }

    private inline fun pickerOption(
        label: String,
        isActive: Boolean,
        crossinline onPick: () -> Unit
    ): UITextButton = object : UITextButton() {
        override var applyTheme: UIComponent.(Theme) -> Unit = { theme ->
            color = theme.accentColor
        }
    }.apply {
        width = UIComponent.FillParent
        alignment = Anchor.CenterLeft
        text = label
        isSelected = isActive
        background = UIBox().apply {
            cornerRadius = 12f
            applyTheme = {
                color = it.accentColor * 0.9f
                alpha = 0f
            }
        }
        foreground = UIBox().apply {
            cornerRadius = 12f
            applyTheme = {
                color = it.accentColor
                alpha = 0f
            }
        }
        if (isActive) {
            foreground?.clearModifiers(ModifierType.Alpha)
            foreground?.fadeTo(0.25f, 0f)
        }
        onActionUp = {
            onPick()
        }
    }
}

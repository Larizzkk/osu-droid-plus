package com.reco1l.framework

import com.edlplan.framework.easing.Easing
import ru.nsu.ccfit.zuev.osuplusplus.Config

/**
 * Central controller for the Enhanced Animations system.
 *
 * This system affects ONLY UI/UX elements (menu transitions, dialogs, settings panels,
 * song select animations, score panels, etc.).
 *
 * It does NOT affect gameplay in any way — hit objects, sliders, spinners, approach
 * circles, hit timing, and all gameplay-critical animations are completely untouched.
 *
 * When the master toggle [enabled] is ON, this object provides:
 *   - [speedMultiplier]: scales UI modifier durations (50%–200%)
 *   - [easing]: default easing curve applied to UI modifiers that have none
 *
 * Usage in UI modifier creation:
 *   val d = EnhancedAnimations.duration(baseDuration)
 *   val e = EnhancedAnimations.effectiveEasing(baseEasing)
 *
 * All methods return identity values when enhanced animations are OFF,
 * so callers can use them unconditionally without if-checks.
 */
object EnhancedAnimations {

    // ── Master toggle ──────────────────────────────────────

    @JvmStatic
    val enabled: Boolean
        get() = Config.getBoolean("enhancedAnimations", false)

    // ── Speed ──────────────────────────────────────────────

    /**
     * Duration multiplier for UI animations. 1.0 = normal speed, 0.5 = 2x faster, 2.0 = 2x slower/smoother.
     * Setting: animationSpeed (50–200, default 100)
     */
    val speedMultiplier: Float
        get() = if (enabled) Config.getInt("animationSpeed", 100) / 100f else 1f

    // ── Easing ─────────────────────────────────────────────

    /**
     * Default easing curve applied to UI modifiers that don't specify one.
     * Setting: animationEasing (default "OutQuint")
     */
    @JvmStatic
    val easing: Easing
        get() {
            if (!enabled) return Easing.None
            val name = Config.getString("animationEasing", "OutQuint")
            return try {
                Easing.valueOf(name)
            } catch (_: Exception) {
                Easing.OutQuint
            }
        }

    // ── Smooth transitions ─────────────────────────────────

    val smoothTransitions: Boolean
        get() = enabled && Config.getBoolean("smoothTransitions", true)

    // ── Per-category toggles ───────────────────────────────

    /**
     * Affect menu UI (song select, settings, score panels).
     * When OFF, menu animations use vanilla timing.
     */
    val menuAnimations: Boolean
        get() = enabled && Config.getBoolean("menuAnimations", true)

    // ── Helper methods (UI-only) ───────────────────────────

    /**
     * Scale a duration by the speed multiplier.
     * Use this for UI modifier durations ONLY.
     *
     * Example: Modifiers.fadeIn(EnhancedAnimations.duration(0.3f))
     */
    @JvmStatic
    fun duration(baseSec: Float): Float {
        return baseSec * speedMultiplier
    }

    /**
     * Get the effective easing for a modifier.
     * If the modifier already has an easing (not Easing.None), keep it.
     * Otherwise, apply the enhanced default easing.
     */
    @JvmStatic
    fun effectiveEasing(baseEasing: Easing): Easing {
        if (!enabled) return baseEasing
        return if (baseEasing == Easing.None) easing else baseEasing
    }
}

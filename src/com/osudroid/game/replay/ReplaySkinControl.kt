package com.osudroid.game.replay

import android.util.Log
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.component.UIComponent
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.container.UILinearContainer
import com.reco1l.andengine.modifier.ModifierType
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.text.UIText
import com.reco1l.andengine.ui.Theme
import com.reco1l.andengine.ui.UICard
import com.reco1l.andengine.ui.UITextButton
import com.reco1l.framework.math.Vec4
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osuplusplus.ResourceManager

/**
 * Skin picker card used by the in-game settings panel during replay/autoplay. Kept as an
 * inline list (NOT a UIDropdown): the dropdown's overlay wrapper does not receive touches
 * from the gameplay scene in this build, while everything inline lives inside the panel's
 * own scrollable container — the same touch pipeline as every other working control.
 */
class ReplaySkinControl : UICard() {

    var appliedSkinPath: String = normalizeSkinPath(Config.getSkinPath())
        private set

    private var isSkinLoading = false
    private var pendingSkinPath: String? = null

    /**
     * Called when the user picks a different skin. The receiver MUST invoke
     * [onSkinLoadResult] exactly once with the outcome so the control can track the
     * applied skin and roll back the button label on failure.
     */
    var onSkinChanged: ((String) -> Unit)? = null

    private val skinListContainer = pickerListContainer()

    init {
        width = UIComponent.FillParent
        title = "Skin"

        // The skin list is ALWAYS visible: no collapsible trigger button, the user
        // sees every entry inside the card directly (upstream settings style).
        content += skinListContainer
        rebuildSkinList()
    }

    /**
     * Re-syncs the list with the config (e.g. after a skin change made elsewhere).
     */
    fun refresh() {
        appliedSkinPath = normalizeSkinPath(Config.getSkinPath())
        rebuildSkinList()
    }

    /**
     * Reports the outcome of a skin hot-swap started through [onSkinChanged].
     * Must be called on the update thread.
     */
    fun onSkinLoadResult(path: String, success: Boolean) {
        // Clear the in-flight flag FIRST so a rollback routes through the pick
        // handler as a harmless "re-selected current skin".
        isSkinLoading = false
        if (success) {
            appliedSkinPath = normalizeSkinPath(path)
        }
        rebuildSkinList()

        // Apply a pick that arrived while this load was running (chained hot-swaps).
        val pending = pendingSkinPath
        pendingSkinPath = null
        if (pending != null && normalizeSkinPath(pending) != appliedSkinPath) {
            isSkinLoading = true
            onSkinChanged?.invoke(pending)
        }
    }

    private fun normalizeSkinPath(path: String): String =
        path.trimEnd('/').let { if (it.isEmpty()) "/" else it }

    private fun rebuildSkinList() {
        skinListContainer.detachChildren()

        // Default skin first, then user skins from Config.getSkins().
        val defaultPath = java.io.File(Config.getSkinTopPath()).path
        val entries = buildList {
            add(java.io.File(Config.getSkinTopPath()).name + " (Default)" to defaultPath)
            Config.getSkins().forEach { (name, path) -> add(name to path) }
        }

        val applied = appliedSkinPath
        entries.forEach { (name, path) ->
            val normalized = normalizeSkinPath(path)
            val isActive = normalized == applied
            skinListContainer += pickerOption(name, isActive) { handleSkinPick(path) }
        }
    }

    private fun handleSkinPick(rawPath: String) {
        val path = normalizeSkinPath(rawPath)
        if (isSkinLoading) {
            // A hot-swap is still running and its completion will CHANGE the applied
            // skin, so EVERY pick — including re-selecting the currently applied one
            // (a cancel request) — must be queued, not dropped.
            if (path != pendingSkinPath) {
                pendingSkinPath = path
            }
            return
        }
        if (path == appliedSkinPath) {
            return
        }
        isSkinLoading = true
        rebuildSkinList()
        onSkinChanged?.invoke(path)
    }

    // ── Shared picker styling (matches the panel's Form* controls) ─────────────

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

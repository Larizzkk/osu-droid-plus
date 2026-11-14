package ru.nsu.ccfit.zuev.osu.menu

import com.osudroid.resources.R.string
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.linearContainer
import com.reco1l.andengine.text
import com.reco1l.andengine.triangle
import com.reco1l.andengine.component.rotationCenter
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.container.UILinearContainer
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.text.UIText
import com.reco1l.andengine.ui.UISelect
import com.reco1l.andengine.ui.UITextButton
import com.reco1l.framework.Color4
import com.reco1l.framework.math.Vec4
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osuplusplus.R
import ru.nsu.ccfit.zuev.osu.ResourceManager
import java.util.function.Consumer

/**
 * Stable-style "Group / Sort" panel for the song select scene (top-right), matching
 * the decompiled PC client's song select:
 *
 *  - big labels "Group" (146,195,230) and "Sort" (174,210,139);
 *  - white dropdown boxes with dark text and a dark chevron;
 *  - crimson quick-tab strip under the row, the active tab is white with black text;
 *  - everything sits on the scene's dark header band, pinned to the top-right corner.
 *
 * The panel only reports changes; all state lives in [SongMenu].
 */
class SortGroupPanel(
    private val onGroupChanged: Consumer<SongMenu.GroupMode>,
    private val onSortChanged: Consumer<SongMenu.SortOrder>
) {

    companion object {
        private val GROUP_LABEL_COLOR = Color4(146, 195, 230)
        private val SORT_LABEL_COLOR = Color4(174, 210, 139)

        // Color.Crimson (220,20,60) at 0.85 alpha.
        private val TAB_COLOR = Color4(220, 20, 60, 217)
        private val TAB_ACTIVE_COLOR = Color4(255, 255, 255)
    }

    // Compact row: it sits on the dark header band and must not overlap the
    // beatmap list / cover art below it. The whole panel is confined to the
    // right half of the screen so the search bar and the cover art stay clear.
    private val selectWidth = Utils.toRes(112f)
    private val selectHeight = Utils.toRes(30f)

    private var currentGroup = SongMenu.GroupMode.None

    private lateinit var row1: UILinearContainer

    private lateinit var row2: UILinearContainer

    val root = object : UIContainer() {

        init {
            background = null
            padding = Vec4.Zero
        }

        override fun onManagedUpdate(pSecondsElapsed: Float) {
            // The rows only get their real widths on the first measure pass, after init.
            // Re-aligning here keeps the strip hugging the content and pinned to the
            // top-right corner instead of stretching across the whole header.
            if (::row1.isInitialized && ::row2.isInitialized &&
                row1.width > 0f && row2.width > 0f
            ) {
                // Confine the panel to the right half: it is pinned to the top-right
                // corner, so the rows are right-aligned inside that half instead of
                // stretching across the header and covering the search bar.
                val maxWidth = Config.getRES_WIDTH() * 0.5f
                width = minOf(maxOf(row1.width, row2.width), maxWidth)

                row1.x = (width - row1.width).coerceAtLeast(0f)
                row2.x = (width - row2.width).coerceAtLeast(0f)
                height = row2.y + row2.height
            }

            super.onManagedUpdate(pSecondsElapsed)
        }
    }

    private val groupSelect = object : UISelect<SongMenu.GroupMode>(emptyList()) {
        override fun onValueChanged() {
            // super() refreshes the button text from the selected option, otherwise the
            // dropdown keeps showing the placeholder forever.
            super.onValueChanged()
            value.firstOrNull()?.let(onGroupChanged::accept)
        }
    }

    private val sortSelect = object : UISelect<SongMenu.SortOrder>(emptyList()) {
        override fun onValueChanged() {
            super.onValueChanged()
            value.firstOrNull()?.let(onSortChanged::accept)
        }
    }

    private val tabs = mutableListOf<Pair<UITextButton, SongMenu.GroupMode>>()

    init {
        val gap = Utils.toRes(6f)

        groupSelect.options = SongMenu.GroupMode.entries.map {
            UISelect.Option(it, StringTable.get(groupLabelRes(it)))
        }
        sortSelect.options = SongMenu.SortOrder.entries.map {
            UISelect.Option(it, StringTable.get(sortLabelRes(it)))
        }

        styleSelect(groupSelect)
        styleSelect(sortSelect)

        groupSelect.width = selectWidth
        groupSelect.height = selectHeight
        sortSelect.width = selectWidth
        sortSelect.height = selectHeight

        root.origin = Anchor.TopRight
        root.x = Config.getRES_WIDTH().toFloat()
        root.y = 0f

        row1 = root.linearContainer {
            orientation = Orientation.Horizontal
            spacing = gap

            text {
                text = StringTable.get(R.string.menu_group_label)
                color = GROUP_LABEL_COLOR
                font = ResourceManager.getInstance().getFont("font")
                height = selectHeight
                alignment = Anchor.Center
            }
            +groupSelect

            text {
                text = StringTable.get(R.string.menu_sort_label)
                color = SORT_LABEL_COLOR
                font = ResourceManager.getInstance().getFont("font")
                height = selectHeight
                alignment = Anchor.Center
            }
            +sortSelect
        }

        row2 = root.linearContainer {
            orientation = Orientation.Horizontal
            spacing = Utils.toRes(2f)
            y = selectHeight + Utils.toRes(3f)

            +quickTab(StringTable.get(R.string.menu_group_collection), SongMenu.GroupMode.Collection)
            +quickTab(StringTable.get(R.string.menu_group_last_played), SongMenu.GroupMode.LastPlayed)
            +quickTab(StringTable.get(R.string.menu_group_artist), SongMenu.GroupMode.Artist)
            +quickTab(StringTable.get(R.string.menu_group_difficulty), SongMenu.GroupMode.Difficulty)
            +quickTab(StringTable.get(R.string.menu_group_none), SongMenu.GroupMode.None)
        }
    }

    /** Applies the current group mode without notifying back (initial sync). */
    fun setGroupMode(mode: SongMenu.GroupMode) {
        currentGroup = mode
        updateTabs()
        if (groupSelect.value != listOf(mode)) {
            groupSelect.value = listOf(mode)
        }
    }

    /** Applies the current sort order without notifying back (initial sync). */
    fun setSortOrder(order: SongMenu.SortOrder) {
        if (sortSelect.value != listOf(order)) {
            sortSelect.value = listOf(order)
        }
    }

    /**
     * Stable's dropdown: white box, dark text, dark chevron. The default theming tints
     * the button background with the accent color, so the white box lives on the select
     * itself and the button keeps a neutral [applyTheme].
     */
    private fun styleSelect(select: UISelect<*>) {
        select.background = UIBox().apply {
            color = Color4.White
            cornerRadius = Utils.toRes(4f)
        }

        select.button.apply {
            background = null
            padding = Vec4(Utils.toRes(6f), Utils.toRes(2f))
            color = Color4.Black
            applyTheme = {}
            trailingIcon = UIContainer().apply {
                padding = Vec4(Utils.toRes(6f), 0f)
                triangle {
                    width = Utils.toRes(10f)
                    height = Utils.toRes(6f)
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    rotationCenter = Anchor.Center
                    rotation = 180f
                    color = Color4(0.15f, 0.15f, 0.15f)
                }
            }
        }
    }

    private fun quickTab(label: String, mode: SongMenu.GroupMode) = UITextButton().apply {
        text = label
        padding = Vec4(Utils.toRes(5f), Utils.toRes(2f))
        background = UIBox().apply {
            cornerRadius = Utils.toRes(3f)
            color = TAB_COLOR
        }
        color = Color4.White
        // The state colors are owned by updateTabs(), not by the theme.
        applyTheme = {}
        tabs += this to mode
        onActionUp = {
            currentGroup = mode
            updateTabs()
            onGroupChanged.accept(mode)
        }
    }

    private fun updateTabs() {
        for ((button, mode) in tabs) {
            val active = mode == currentGroup
            (button.background as? UIBox)?.color = if (active) TAB_ACTIVE_COLOR else TAB_COLOR
            button.color = if (active) Color4.Black else Color4.White
        }
    }

    private fun groupLabelRes(mode: SongMenu.GroupMode) = when (mode) {
        SongMenu.GroupMode.None -> R.string.menu_group_none
        SongMenu.GroupMode.Title -> R.string.menu_group_title
        SongMenu.GroupMode.Artist -> R.string.menu_group_artist
        SongMenu.GroupMode.Creator -> R.string.menu_group_creator
        SongMenu.GroupMode.DateAdded -> R.string.menu_group_date
        SongMenu.GroupMode.LastPlayed -> R.string.menu_group_last_played
        SongMenu.GroupMode.Difficulty -> R.string.menu_group_difficulty
        SongMenu.GroupMode.Length -> R.string.menu_group_length
        SongMenu.GroupMode.Bpm -> R.string.menu_group_bpm
        SongMenu.GroupMode.Collection -> R.string.menu_group_collection
    }

    private fun sortLabelRes(order: SongMenu.SortOrder) = when (order) {
        SongMenu.SortOrder.Title -> string.menu_search_sort_title
        SongMenu.SortOrder.Artist -> string.menu_search_sort_artist
        SongMenu.SortOrder.Creator -> string.menu_search_sort_creator
        SongMenu.SortOrder.Date -> string.menu_search_sort_date
        SongMenu.SortOrder.Bpm -> string.menu_search_sort_bpm
        SongMenu.SortOrder.DroidStars -> string.menu_search_sort_droid_stars
        SongMenu.SortOrder.StandardStars -> string.menu_search_sort_standard_stars
        SongMenu.SortOrder.Length -> string.menu_search_sort_length
    }
}

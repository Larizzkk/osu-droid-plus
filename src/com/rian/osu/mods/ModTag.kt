package com.rian.osu.mods

/**
 * Represents Tag mod - multi-cursor autoplay where each cursor hits its own objects sequentially.
 */
class ModTag : Mod() {
    override val name = "Tag"
    override val acronym = "TG"
    override val description = "Multi-cursor autoplay where each cursor hits its own objects sequentially using different movement styles."
    override val type = ModType.Automation
    override val isValidForMultiplayer = false
    override val isValidForMultiplayerAsFreeMod = false
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModRelax::class, ModAutopilot::class, ModAutoplay::class, ModPerfect::class, ModSuddenDeath::class
    )
}

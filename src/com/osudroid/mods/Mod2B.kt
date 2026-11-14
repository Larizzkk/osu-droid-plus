package com.osudroid.mods

/**
 * Represents the 2B (two-button) mod.
 *
 * 2B style is played with two fingers and does not require holding the slider
 * or spinner lock, which is the same behaviour as the "Remove slider lock"
 * setting. It is a ranked mod, so scores played with it are submitted.
 */
class Mod2B : Mod() {
    override val name = "2B"
    override val acronym = "2B"
    override val description = "Play in two-button style: sliders and spinners no longer lock your input."
    override val type = ModType.Fun
    override val isRanked = true
}

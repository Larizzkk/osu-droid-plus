package com.osudroid.mods

/**
 * Represents the No Notelock mod.
 *
 * Every active object can be hit independently instead of waiting for the
 * earliest unjudged object. Long objects no longer hold the hit queue either,
 * so overlapping circles can be played in any order.
 */
class ModNoNotelock : Mod() {
    override val name = "No Notelock"
    override val acronym = "NL"
    override val description = "Hit any active object at any time. No note lock, no slider lock."
    override val type = ModType.Fun
    override val isRanked = true
}

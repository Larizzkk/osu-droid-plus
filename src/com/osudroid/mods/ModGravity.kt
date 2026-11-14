package com.osudroid.mods

import com.osudroid.mods.settings.EnumModSetting

/**
 * Represents the Gravity mod.
 *
 * Hit objects slide in from a configurable direction to their original position. Judgement is
 * unaffected: it always uses the beatmap position.
 */
class ModGravity : Mod() {
    override val name = "Gravity"
    override val acronym = "GV"
    override val description = "Objects fall from the sky!"
    override val type = ModType.Fun
    override val isRanked = true
    override val iconTextureNameSuffix = "gravity"

    /**
     * The direction from which objects appear.
     */
    var direction by EnumModSetting(
        name = "Direction",
        key = "direction",
        defaultValue = Direction.BottomToTop
    )

    /**
     * Converts the selected direction to the [AnimationDirection] used by gameplay code.
     */
    fun getAnimationDirection() = when (direction) {
        Direction.BottomToTop -> AnimationDirection.BottomToTop
        Direction.TopToBottom -> AnimationDirection.TopToBottom
        Direction.LeftToRight -> AnimationDirection.LeftToRight
        Direction.RightToLeft -> AnimationDirection.RightToLeft
    }

    enum class Direction {
        BottomToTop,
        TopToBottom,
        LeftToRight,
        RightToLeft
    }

    /**
     * Internal direction used by gameplay code.
     */
    enum class AnimationDirection {
        BottomToTop,
        TopToBottom,
        LeftToRight,
        RightToLeft
    }

    companion object {
        /**
         * Distance in screen pixels from which objects slide in.
         */
        const val RISE_DISTANCE = 400f

        /**
         * Computes the gravity visual offset for a given elapsed time.
         *
         * When [passedTime] is 0 the offset is at its maximum, and it shrinks to 0 as
         * [passedTime] reaches [timePreempt].
         *
         * @return A pair of screen space offsets as `[offsetX, offsetY]`.
         */
        @JvmStatic
        fun getOffset(passedTime: Float, timePreempt: Float, dir: AnimationDirection): FloatArray {
            if (passedTime < 0 || passedTime >= timePreempt || timePreempt <= 0) {
                return floatArrayOf(0f, 0f)
            }

            val distance = RISE_DISTANCE * (1f - passedTime / timePreempt)

            return when (dir) {
                AnimationDirection.BottomToTop -> floatArrayOf(0f, distance)
                AnimationDirection.TopToBottom -> floatArrayOf(0f, -distance)
                AnimationDirection.LeftToRight -> floatArrayOf(-distance, 0f)
                AnimationDirection.RightToLeft -> floatArrayOf(distance, 0f)
            }
        }
    }
}

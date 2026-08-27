package com.rian.osu.mods

import com.rian.osu.mods.settings.EnumModSetting

/**
 * Gravity mod — objects slide in from a configurable direction to their original position.
 *
 * Hit detection is unaffected — it uses the original position.
 */
class ModGravity : Mod() {
    override val name = "Gravity"
    override val acronym = "GV"
    override val description = "Objects fall from the sky!"
    override val type = ModType.Fun
    override val isRanked = true
    override val scoreMultiplier = 1.06f

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
     * Converts the selected direction to an [AnimationDirection] that the gameplay code uses.
     */
    fun getAnimationDirection(): AnimationDirection = when (direction) {
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
        /** Distance in screen pixels from which objects slide in. */
        const val RISE_DISTANCE = 400f

        /**
         * Computes the gravity visual offset for a given elapsed time.
         * Returns (offsetX, offsetY) based on direction and progress toward hit time.
         * When passedTime == 0, offset is at maximum. When passedTime == timePreempt, offset is 0.
         */
        @JvmStatic
        fun getOffset(passedTime: Float, timePreempt: Float, dir: AnimationDirection): FloatArray {
            if (passedTime < 0 || passedTime >= timePreempt || timePreempt <= 0) {
                return floatArrayOf(0f, 0f)
            }
            val d = RISE_DISTANCE * (1f - passedTime / timePreempt)
            return when (dir) {
                AnimationDirection.BottomToTop -> floatArrayOf(0f, d)
                AnimationDirection.TopToBottom -> floatArrayOf(0f, -d)
                AnimationDirection.LeftToRight -> floatArrayOf(-d, 0f)
                AnimationDirection.RightToLeft -> floatArrayOf(d, 0f)
            }
        }
    }
}

package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;

/**
 * Extended mover interface that accepts slider metadata.
 *
 * <p>Documentation reference:
 * <ul>
 *   <li>danser-go: {@code app/dance/movers/bezier.go}, {@code angleoffset.go}, {@code aggressive.go},
 *       {@code momentum.go}, {@code spline.go}
 *   <li>Go interface: {@code app/beatmap/objects/ILongObject}
 *   <li>Methods: {@code GetEndAngleMod}, {@code GetStartAngleMod}, {@code GetStackedPositionAtMod}
 * </ul>
 *
 * <p>Implementations should delegate to the legacy {@link CursorMover#setMovement(PointF, PointF, float, float)}
 * when slider metadata is unavailable (e.g., when {@link SliderMovementContext#startIsSlider} and
 * {@link SliderMovementContext#endIsSlider} are both false).
 */
public interface SliderAwareMover extends CursorMover {

    /**
     * Sets movement with full slider metadata.
     *
     * @param context the movement context containing positions, times, and slider metadata
     */
    void setMovement(SliderMovementContext context);
}

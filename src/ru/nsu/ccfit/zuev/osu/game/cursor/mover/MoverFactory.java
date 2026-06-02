package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import ru.nsu.ccfit.zuev.osu.game.cursor.AutoplayStyle;

/**
 * Factory for creating cursor movers based on style.
 * Ported to use 1:1 danser-go implementations.
 */
public class MoverFactory {
    
    public static CursorMover createMover(AutoplayStyle style) {
        switch (style) {
            case BEZIER:
                return new BezierMover(0);
            case AGGRESSIVE:
                return new AggressiveMover(0);
            case SPLINE:
                return new SplineMover(0);
            case CIRCULAR:
                return new HalfCircleMover(0);
            case AXIS:
                return new AxisMover(0);
            case EXGON:
                return new ExGonMover(0);
            case MOMENTUM:
                return new MomentumMover(0);
            case PIPPI:
                return new PippiMover(0);
            case FLOWER:
                return new AngleOffsetMover(0);
            case LINEAR:
            default:
                return new LinearMover(false, 0);
        }
    }
}

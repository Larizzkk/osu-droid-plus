package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;

/**
 * Immutable context carrying slider-aware metadata for a single movement segment.
 *
 * <p>This replaces the scattered boolean/float parameters and mirrors the data
 * danser-go extracts from {@code ILongObject} inside {@code SetObjects()}.
 *
 * <p>Documentation reference:
 * <ul>
 *   <li>danser-go: {@code app/dance/movers/bezier.go} lines 52-79
 *   <li>danser-go: {@code app/dance/movers/angleoffset.go} lines 51-110
 *   <li>danser-go: {@code app/dance/movers/aggressive.go} lines 42-54
 * </ul>
 */
public final class SliderMovementContext {

    public final PointF startPos;
    public final PointF endPos;
    public final float startTime;
    public final float endTime;
    public final boolean startIsSlider;
    public final boolean endIsSlider;
    public final float startAngle;
    public final float endAngle;
    public final float startDistance;
    public final float endDistance;
    /**
     * danser-go treats spinners as ILongObject too: movers get the spin motion
     * direction and 10ms travel distance at the spinner's start/end exactly like
     * for sliders. These flags mark that the angle/distance metadata comes from
     * a spinner boundary (startAngle = motion direction into the spin, endAngle
     * = the direction the spin leaves behind) rather than slider ball geometry.
     */
    public final boolean startIsSpinner;
    public final boolean endIsSpinner;

    private SliderMovementContext(
            PointF startPos, PointF endPos, float startTime, float endTime,
            boolean startIsSlider, boolean endIsSlider,
            float startAngle, float endAngle,
            float startDistance, float endDistance) {
        this(startPos, endPos, startTime, endTime,
            startIsSlider, endIsSlider, false, false,
            startAngle, endAngle, startDistance, endDistance);
    }

    private SliderMovementContext(
            PointF startPos, PointF endPos, float startTime, float endTime,
            boolean startIsSlider, boolean endIsSlider,
            boolean startIsSpinner, boolean endIsSpinner,
            float startAngle, float endAngle,
            float startDistance, float endDistance) {
        this.startPos = startPos;
        this.endPos = endPos;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startIsSlider = startIsSlider;
        this.endIsSlider = endIsSlider;
        this.startIsSpinner = startIsSpinner;
        this.endIsSpinner = endIsSpinner;
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        this.startDistance = startDistance;
        this.endDistance = endDistance;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Creates a non-slider context (legacy 2-point path). */
    public static SliderMovementContext of(PointF startPos, PointF endPos, float startTime, float endTime) {
        return builder()
                .startPos(startPos)
                .endPos(endPos)
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }

    public static class Builder {
        private PointF startPos = new PointF();
        private PointF endPos = new PointF();
        private float startTime;
        private float endTime;
        private boolean startIsSlider;
        private boolean endIsSlider;
        private boolean startIsSpinner;
        private boolean endIsSpinner;
        private float startAngle;
        private float endAngle;
        private float startDistance;
        private float endDistance;

        public Builder startPos(PointF p) {
            this.startPos = p != null ? new PointF(p.x, p.y) : new PointF();
            return this;
        }

        public Builder endPos(PointF p) {
            this.endPos = p != null ? new PointF(p.x, p.y) : new PointF();
            return this;
        }

        public Builder startTime(float t) {
            this.startTime = t;
            return this;
        }

        public Builder endTime(float t) {
            this.endTime = t;
            return this;
        }

        public Builder startIsSlider(boolean v) {
            this.startIsSlider = v;
            return this;
        }

        public Builder endIsSlider(boolean v) {
            this.endIsSlider = v;
            return this;
        }

        public Builder startIsSpinner(boolean v) {
            this.startIsSpinner = v;
            return this;
        }

        public Builder endIsSpinner(boolean v) {
            this.endIsSpinner = v;
            return this;
        }

        public Builder startAngle(float a) {
            this.startAngle = a;
            return this;
        }

        public Builder endAngle(float a) {
            this.endAngle = a;
            return this;
        }

        public Builder startDistance(float d) {
            this.startDistance = d;
            return this;
        }

        public Builder endDistance(float d) {
            this.endDistance = d;
            return this;
        }

        public SliderMovementContext build() {
            return new SliderMovementContext(
                    startPos, endPos, startTime, endTime,
                    startIsSlider, endIsSlider,
                    startIsSpinner, endIsSpinner,
                    startAngle, endAngle,
                    startDistance, endDistance
            );
        }
    }
}

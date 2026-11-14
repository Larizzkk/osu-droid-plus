package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.GameObject;
import ru.nsu.ccfit.zuev.osu.game.GameplaySlider;

/**
 * Resolves slider metadata from {@link GameObject} instances.
 *
 * <p>This replaces danser-go's {@code ILongObject} casting inside movers.
 * Each mover that needs slider angles calls this utility instead of
 * performing its own type checks.
 *
 * <p>Documentation reference:
 * <ul>
 *   <li>danser-go: {@code app/dance/movers/bezier.go} lines 52-79
 *   <li>danser-go: {@code app/dance/movers/angleoffset.go} lines 51-110
 *   <li>danser-go: {@code app/dance/movers/aggressive.go} lines 42-54
 *   <li>danser-go: {@code app/beatmap/objects/ILongObject}
 * </ul>
 */
public final class SliderMetadataResolver {

    private SliderMetadataResolver() {
    }

    /**
     * Resolves metadata for the START object of a movement segment.
     *
     * @param obj      the start object
     * @param startTimeMs the start object's end time (ms), used for stacked position lookup
     * @param referencePos the reference position to measure distance from (typically startPos)
     * @return metadata, never null
     */
    public static SliderEndMetadata resolveStart(GameObject obj, float startTimeMs, PointF referencePos) {
        if (obj instanceof GameplaySlider slider) {
            // danser-go bezier/angleoffset/aggressive use
            // s1.GetStackedPositionAtMod(mover.startTime - 10, diff).Dst(startPos)
            // where startPos is the previous object's end position (= referencePos here).
            PointF ctrl = slider.getStackedPositionAtTime(startTimeMs - 10f);
            float endAngle = slider.getSliderEndAngleRad();
            float distance = (float) Math.hypot(ctrl.x - referencePos.x, ctrl.y - referencePos.y);
            return new SliderEndMetadata(true, endAngle, distance);
        }
        return SliderEndMetadata.notSlider();
    }

    /**
     * Resolves metadata for the END object of a movement segment.
     *
     * @param obj      the end object
     * @param endTimeMs   the end object's start time (ms), used for stacked position lookup
     * @param referencePos the reference position to measure distance from (typically endPos)
     * @return metadata, never null
     */
    public static SliderStartMetadata resolveEnd(GameObject obj, float endTimeMs, PointF referencePos) {
        if (obj instanceof GameplaySlider slider) {
            // danser-go uses s2.GetStackedPositionAtMod(mover.endTime + 10, diff).Dst(endPos)
            // where endPos is the next object's start position (= referencePos here).
            PointF ctrl = slider.getStackedPositionAtTime(endTimeMs + 10f);
            float startAngle = slider.getSliderStartAngleRad();
            float distance = (float) Math.hypot(ctrl.x - referencePos.x, ctrl.y - referencePos.y);
            return new SliderStartMetadata(true, startAngle, distance);
        }
        return SliderStartMetadata.notSlider();
    }

    /**
     * Convenience: resolves both start and end metadata in one call.
     */
    public static SegmentMetadata resolveSegment(
            GameObject startObj, GameObject endObj,
            float startTimeMs, float endTimeMs,
            PointF startPos, PointF endPos) {
        SliderEndMetadata startMeta = resolveStart(startObj, startTimeMs, startPos);
        SliderStartMetadata endMeta = resolveEnd(endObj, endTimeMs, endPos);
        return new SegmentMetadata(startMeta, endMeta);
    }

    /**
     * Metadata for the START object of a segment.
     */
    public static final class SliderEndMetadata {
        public final boolean isSlider;
        public final float endAngle;
        public final float distanceToReference;

        private SliderEndMetadata(boolean isSlider, float endAngle, float distanceToReference) {
            this.isSlider = isSlider;
            this.endAngle = endAngle;
            this.distanceToReference = distanceToReference;
        }

        public static SliderEndMetadata notSlider() {
            return new SliderEndMetadata(false, 0f, 0f);
        }
    }

    /**
     * Metadata for the END object of a segment.
     */
    public static final class SliderStartMetadata {
        public final boolean isSlider;
        public final float startAngle;
        public final float distanceToReference;

        private SliderStartMetadata(boolean isSlider, float startAngle, float distanceToReference) {
            this.isSlider = isSlider;
            this.startAngle = startAngle;
            this.distanceToReference = distanceToReference;
        }

        public static SliderStartMetadata notSlider() {
            return new SliderStartMetadata(false, 0f, 0f);
        }
    }

    /**
     * Combined metadata for a movement segment.
     */
    public static final class SegmentMetadata {
        public final SliderEndMetadata start;
        public final SliderStartMetadata end;

        public SegmentMetadata(SliderEndMetadata start, SliderStartMetadata end) {
            this.start = start;
            this.end = end;
        }
    }
}

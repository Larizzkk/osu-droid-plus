package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.Constants;

/**
 * Port of danser-go app/dance/spinners: circle, heart, triangle, square, cube.
 *
 * All movers are PURE functions of gameplay time (no per-frame integration), so
 * rate mods and replay seeks stay consistent, and the position at the spinner's
 * start/end times exactly matches the segment entry/exit points baked in
 * AutoCursor.initQueue().
 *
 * danser-go computes spinner shapes in osu!px playfield space around the center
 * (256, 192). AutoCursor works in SCREEN pixels (hit objects come from
 * screenSpaceGameplayStackedPosition), so every position returned here is
 * converted from osu!px to screen space with the same transform used by
 * HitObject.convertPositionToRealCoordinates(). The 512x384 → MAP_ACTUAL scale
 * is uniform (4:3), so angles and the shape geometry are preserved exactly.
 *
 * danser-go: rpms = 0.00795 (spinners/mover.go) — revolutions per millisecond.
 */
public final class SpinnerMoverFactory {

    /** danser-go spinners/mover.go: const rpms = 0.00795 */
    public static final float RPMS = 0.00795f;
    /** danser-go: var center = vector.NewVec2f(256, 192) — in osu!px */
    private static final float CENTER_X = 256f;
    private static final float CENTER_Y = 192f;

    private SpinnerMoverFactory() {}

    public static SpinnerMover getMoverByName(String name) {
        if (name == null) return new CircleMover();
        switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "heart":    return new HeartMover();
            case "triangle": return new TriangleMover();
            case "square":   return new SquareMover();
            case "cube":     return new CubeMover();
            default:         return new CircleMover();
        }
    }

    /** Shared settings access per danser spinner settings block (dancenew.go: spinner). */
    private static float radius()   { return MoverSettings.getSpinnerRadius(); }

    /**
     * osu!px playfield coordinates → screen pixels, 1:1 with
     * com.rian.osu.beatmap.hitobject.HitObject.convertPositionToRealCoordinates().
     */
    private static PointF toScreen(float trackX, float trackY) {
        float xScale = Constants.MAP_ACTUAL_WIDTH / (float) Constants.MAP_WIDTH;
        float yScale = Constants.MAP_ACTUAL_HEIGHT / (float) Constants.MAP_HEIGHT;
        float xOffset = (Config.getRES_WIDTH() - Constants.MAP_ACTUAL_WIDTH) / 2f;
        float yOffset = (Config.getRES_HEIGHT() - Constants.MAP_ACTUAL_HEIGHT) / 2f;
        return new PointF(trackX * xScale + xOffset, trackY * yScale + yOffset);
    }

    public interface SpinnerMover {
        /** Position at gameplay time (ms), in SCREEN pixel space. */
        PointF getPositionAt(float timeMs, float startTimeMs);
    }

    // ── circle.go ──
    // NewVec2fRad(rpms*(t-start)*2π, Radius).Add(center)
    public static final class CircleMover implements SpinnerMover {
        @Override
        public PointF getPositionAt(float timeMs, float startTimeMs) {
            float rad = RPMS * (timeMs - startTimeMs) * 2f * (float) Math.PI;
            return toScreen(
                CENTER_X + radius() * (float) Math.cos(rad),
                CENTER_Y + radius() * (float) Math.sin(rad)
            );
        }
    }

    // ── heart.go ──
    // x = sin^3(rad); y = (13cos(rad) - 5cos(2rad) - 2cos(3rad) - cos(4rad)) / 16
    // vec(x, y) * (Radius, -Radius) + center
    public static final class HeartMover implements SpinnerMover {
        @Override
        public PointF getPositionAt(float timeMs, float startTimeMs) {
            float rad = RPMS * (timeMs - startTimeMs) * 2f * (float) Math.PI;
            float x = (float) Math.pow(Math.sin(rad), 3);
            float y = (13f * (float) Math.cos(rad)
                - 5f * (float) Math.cos(2f * rad)
                - 2f * (float) Math.cos(3f * rad)
                - (float) Math.cos(4f * rad)) / 16f;
            return toScreen(
                CENTER_X + x * radius(),
                CENTER_Y - y * radius()
            );
        }
    }

    // ── square.go ──
    // Rotating square: mat = RotZ((t-start)/2000 * 2π) * Scale(Radius)
    // Vertices (-1,-1),(1,-1),(1,1),(-1,1); the cursor walks the perimeter,
    // one vertex every 10 ms (startIndex = (t-start)/10 % 4), interpolated linearly.
    public static final class SquareMover implements SpinnerMover {
        private static final float[][] VERTS = {
            {-1f, -1f}, {1f, -1f}, {1f, 1f}, {-1f, 1f}
        };

        @Override
        public PointF getPositionAt(float timeMs, float startTimeMs) {
            float rel = timeMs - startTimeMs;
            // Rotate3DZ(angle) rotates CCW; apply scale afterwards (danser Muls order).
            float ang = rel / 2000f * 2f * (float) Math.PI;
            float cos = (float) Math.cos(ang), sin = (float) Math.sin(ang);
            float r = radius();

            long idx = (long) (Math.max(0f, rel) / 10f) % 4;
            float[] pt1 = VERTS[(int) idx];
            float[] pt2 = VERTS[(int) ((idx + 1) % 4)];

            // mat.Mul3x1(vert): rotate + scale
            float p1x = (pt1[0] * cos - pt1[1] * sin) * r;
            float p1y = (pt1[0] * sin + pt1[1] * cos) * r;
            float p2x = (pt2[0] * cos - pt2[1] * sin) * r;
            float p2y = (pt2[0] * sin + pt2[1] * cos) * r;

            float t = (Math.max(0f, rel) % 10f) / 10f;
            return toScreen(
                CENTER_X + (p2x - p1x) * t + p1x,
                CENTER_Y + (p2y - p1y) * t + p1y
            );
        }
    }

    // ── triangle.go ──
    // Same structure as square with triangle vertices; mgl32 Vec3 y is flipped
    // relative to screen (indices {±0.866, -0.5}, {0, 1}), applied 1:1.
    public static final class TriangleMover implements SpinnerMover {
        private static final float[][] VERTS = {
            {-0.86602540378f, -0.5f}, {0.86602540378f, -0.5f}, {0f, 1f}
        };

        @Override
        public PointF getPositionAt(float timeMs, float startTimeMs) {
            float rel = timeMs - startTimeMs;
            float ang = rel / 2000f * 2f * (float) Math.PI;
            float cos = (float) Math.cos(ang), sin = (float) Math.sin(ang);
            float r = radius();

            long idx = (long) (Math.max(0f, rel) / 10f) % 3;
            float[] pt1 = VERTS[(int) idx];
            float[] pt2 = VERTS[(int) ((idx + 1) % 3)];

            float p1x = (pt1[0] * cos - pt1[1] * sin) * r;
            float p1y = (pt1[0] * sin + pt1[1] * cos) * r;
            float p2x = (pt2[0] * cos - pt2[1] * sin) * r;
            float p2y = (pt2[0] * sin + pt2[1] * cos) * r;

            float t = (Math.max(0f, rel) % 10f) / 10f;
            return toScreen(
                CENTER_X + (p2x - p1x) * t + p1x,
                CENTER_Y + (p2y - p1y) * t + p1y
            );
        }
    }

    // ── cube.go ──
    // Wireframe cube rotating in 3D, projected to 2D. Vertices and edge walk
    // indices are 1:1 from danser-go cube.go (edge every 4 ms).
    public static final class CubeMover implements SpinnerMover {
        private static final float[][] V = {
            {-1, -1, -1}, {-1, 1, -1}, {1, 1, -1}, {1, -1, -1},
            {-1, -1, 1},  {-1, 1, 1},  {1, 1, 1},  {1, -1, 1}
        };
        private static final int[] EDGES = {
            0, 1, 2, 3, 0, 4, 5, 1, 5, 6, 2, 6, 7, 3, 7, 4
        };

        @Override
        public PointF getPositionAt(float timeMs, float startTimeMs) {
            float rel = timeMs - startTimeMs;
            float radY = (float) Math.sin(rel / 9000f * 2f * Math.PI) * 3.0f / 18f * (float) Math.PI;
            float radX = (float) Math.sin(rel / 5000f * 2f * Math.PI) * 3.0f / 18f * (float) Math.PI;
            float scale = (1f + (float) Math.sin(rel / 4500f * 2f * Math.PI) * 0.3f) * radius();

            long idx = (long) (Math.max(0f, rel) / 4f) % EDGES.length;
            int i1 = EDGES[(int) idx];
            int i2 = EDGES[(int) ((idx + 1) % EDGES.length)];

            float[] v1 = V[i1];
            float[] v2 = V[i2];

            float t = (Math.max(0f, rel) % 4f) / 4f;
            float px = (v2[0] - v1[0]) * t + v1[0];
            float py = (v2[1] - v1[1]) * t + v1[1];
            float pz = (v2[2] - v1[2]) * t + v1[2];

            // mat = RotY(radY) * RotX(radX) * Scale3D(scale)
            // RotX then RotY applied to the point (row-vector-free explicit math):
            float x1 = px;
            float y1 = py * (float) Math.cos(radX) - pz * (float) Math.sin(radX);
            float z1 = py * (float) Math.sin(radX) + pz * (float) Math.cos(radX);

            float x2 = x1 * (float) Math.cos(radY) + z1 * (float) Math.sin(radY);
            float y2 = y1;
            float z2 = -x1 * (float) Math.sin(radY) + z1 * (float) Math.cos(radY);

            x2 *= scale; y2 *= scale; z2 *= scale;

            // Fake perspective from danser: pt *= 1 + z/scale/10
            float persp = 1f + z2 / scale / 10f;
            x2 *= persp;
            y2 *= persp;

            return toScreen(CENTER_X + x2, CENTER_Y + y2);
        }
    }
}

package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.utils;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.CursorMover;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.AngleOffsetMover;
import ru.nsu.ccfit.zuev.osu.game.GameObject;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;
import ru.nsu.ccfit.zuev.osu.Config;

import java.util.List;
import java.util.ArrayList;

/**
 * Utility class for cursor movement operations
 * Provides helper methods for mover management and calculations
 */
public class MoverUtils {

    /**
     * Create a flower (angle offset) mover with default settings
     */
    public static AngleOffsetMover createFlowerMover() {
        return createFlowerMover(0);
    }

    /**
     * Create a flower (angle offset) mover with specific ID
     */
    public static AngleOffsetMover createFlowerMover(int id) {
        AngleOffsetMover mover = new AngleOffsetMover(id);
        return mover;
    }

    /**
     * Calculate distance between two points
     */
    public static float calculateDistance(PointF p1, PointF p2) {
        float dx = p1.x - p2.x;
        float dy = p1.y - p2.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Calculate angle between two points in radians
     */
    public static float calculateAngle(PointF from, PointF to) {
        return (float) Math.atan2(to.y - from.y, to.x - from.x);
    }

    /**
     * Convert angle from degrees to radians
     */
    public static float degToRad(float degrees) {
        return degrees * (float) Math.PI / 180.0f;
    }

    /**
     * Convert angle from radians to degrees
     */
    public static float radToDeg(float radians) {
        return radians * 180.0f / (float) Math.PI;
    }

    /**
     * Normalize angle to 0-2π range
     */
    public static float normalizeAngle(float angle) {
        angle %= (2 * Math.PI);
        if (angle < 0) {
            angle += 2 * Math.PI;
        }
        return angle;
    }

    /**
     * Calculate angle between three points (for stream detection)
     */
    public static float calculateAngleBetween(PointF p1, PointF p2, PointF p3) {
        Vector2f v1 = new Vector2f(p1.x - p2.x, p1.y - p2.y);
        Vector2f v2 = new Vector2f(p3.x - p2.x, p3.y - p2.y);

        return Vector2f.angleBetween32(v1, new Vector2f(0, 0), v2);
    }

    /**
     * Check if movement is a stream based on angle threshold
     */
    public static boolean isStream(PointF p1, PointF p2, PointF p3, float angleThreshold) {
        float angle = calculateAngleBetween(p1, p2, p3);
        return angle >= angleThreshold;
    }

    /**
     * Interpolate position between two points
     */
    public static PointF interpolate(PointF p1, PointF p2, float t) {
        return new PointF(
            p1.x + t * (p2.x - p1.x),
            p1.y + t * (p2.y - p1.y)
        );
    }

    /**
     * Create control points for Bezier curve with flower pattern
     */
    public static PointF[] createFlowerControlPoints(PointF start, PointF end, float angleOffset, float distanceMult) {
        float distance = calculateDistance(start, end);
        float scaledDistance = distance * distanceMult;

        // Calculate base angle
        float baseAngle = calculateAngle(start, end);
        float controlAngle1 = baseAngle + degToRad(180) + degToRad(angleOffset);
        float controlAngle2 = baseAngle - degToRad(angleOffset);

        // Create control points
        PointF control1 = new PointF(
            start.x + scaledDistance * (float) Math.cos(controlAngle1),
            start.y + scaledDistance * (float) Math.sin(controlAngle1)
        );

        PointF control2 = new PointF(
            end.x + scaledDistance * (float) Math.cos(controlAngle2),
            end.y + scaledDistance * (float) Math.sin(controlAngle2)
        );

        return new PointF[]{start, control1, control2, end};
    }

    /**
     * Generate flower pattern points for multiple segments
     */
    public static List<PointF> generateFlowerPath(PointF[] points, float angleOffset, float distanceMult) {
        List<PointF> path = new ArrayList<>();

        if (points.length < 2) {
            return path;
        }

        for (int i = 0; i < points.length - 1; i++) {
            PointF[] segmentPoints = createFlowerControlPoints(points[i], points[i + 1], angleOffset, distanceMult);

            // Add all points except the first (to avoid duplicates)
            if (i == 0) {
                for (PointF p : segmentPoints) {
                    path.add(p);
                }
            } else {
                for (int j = 1; j < segmentPoints.length; j++) {
                    path.add(segmentPoints[j]);
                }
            }
        }

        return path;
    }

    /**
     * Smooth movement using exponential moving average
     */
    public static PointF smoothMovement(PointF current, PointF target, float smoothing) {
        return new PointF(
            current.x + smoothing * (target.x - current.x),
            current.y + smoothing * (target.y - current.y)
        );
    }

    /**
     * Add easing to movement
     */
    public static float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }

    /**
     * Apply easing to position interpolation
     */
    public static PointF easeInterpolation(PointF start, PointF end, float t) {
        float easedT = easeInOutCubic(t);
        return interpolate(start, end, easedT);
    }

    /**
     * Calculate movement speed
     */
    public static float calculateSpeed(PointF p1, PointF p2, float timeDelta) {
        if (timeDelta <= 0) return 0;
        float distance = calculateDistance(p1, p2);
        return distance / timeDelta;
    }

    /**
     * Check if position is within movement bounds
     */
    public static boolean isWithinBounds(PointF pos, PointF min, PointF max) {
        return pos.x >= min.x && pos.x <= max.x && pos.y >= min.y && pos.y <= max.y;
    }

    /**
     * Clamp position to bounds
     */
    public static PointF clampToBounds(PointF pos, PointF min, PointF max) {
        return new PointF(
            Math.max(min.x, Math.min(max.x, pos.x)),
            Math.max(min.y, Math.min(max.y, pos.y))
        );
    }

    /**
     * Create circular movement pattern
     */
    public static PointF createCircularMovement(PointF center, float radius, float angle) {
        return new PointF(
            center.x + radius * (float) Math.cos(angle),
            center.y + radius * (float) Math.sin(angle)
        );
    }

    /**
     * Create spiral movement pattern
     */
    public static PointF createSpiralMovement(PointF center, float startRadius, float endRadius, float angle) {
        float radius = startRadius + (endRadius - startRadius) * (angle / (2 * (float) Math.PI));
        return createCircularMovement(center, radius, angle);
    }

    /**
     * Validate mover configuration
     */
    public static boolean validateMoverConfig(float angleOffset, float distanceMult, float streamAngleOffset) {
        return angleOffset >= 0 && angleOffset <= 360 &&
               distanceMult >= -4 && distanceMult <= 4 &&
               streamAngleOffset >= 0 && streamAngleOffset <= 360;
    }

    /**
     * Get default flower configuration
     */
    public static FlowerConfig getDefaultFlowerConfig() {
        return new FlowerConfig();
    }

    /**
     * Configuration class for flower mover settings
     */
    public static class FlowerConfig {
        public float angleOffset = 90f;
        public float distanceMult = 0.666f;
        public float streamAngleOffset = 90f;
        public long longJump = -1;
        public float longJumpMult = 0.7f;
        public boolean longJumpOnEqualPos = false;

        public boolean isValid() {
            return validateMoverConfig(angleOffset, distanceMult, streamAngleOffset);
        }

        public void loadFromConfig() {
            // Load from Config if available
            try {
                this.angleOffset = Config.getFloat("flower_angle_offset", 90f);
                this.distanceMult = Config.getFloat("flower_distance_mult", 0.666f);
                this.streamAngleOffset = Config.getFloat("flower_stream_angle_offset", 90f);
                this.longJump = -1L;
                this.longJumpMult = Config.getFloat("flower_long_jump_mult", 0.7f);
                this.longJumpOnEqualPos = Config.getBoolean("flower_long_jump_on_equal_pos", false);
            } catch (Exception e) {
                // Use defaults if config loading fails
            }
        }

        public void saveToConfig() {
            // Save to Config - using available methods
            try {
                // Config doesn't have setProperty, so we'll skip saving for now
                // In a real implementation, you would use the proper Config API
            } catch (Exception e) {
                // Ignore save errors
            }
        }
    }
}

package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.game.GameObject;

/**
 * Cursor movement utilities ensuring behavior parity with danser-go.
 *
 * <p>Documentation reference:
 * <ul>
 *   <li>danser-go: {@code app/dance/schedulers/generic.go}
 *   <li>danser-go: {@code app/dance/movers/*.go} (all movers use stacked positions)
 * </ul>
 */
public final class CursorMovementUtils {

    private CursorMovementUtils() {
    }

    /**
     * Clamps the given position to the nearest stacked burst position.
     *
     * <p>In danser-go, during stacked bursts all objects share the same stacked position.
     * The cursor should remain at that position instead of drifting away.
     *
     * @param pos        current cursor position
     * @param burstPos   stacked burst position (same for all objects in the burst)
     * @param threshold  maximum distance to snap (in pixels)
     * @return clamped position
     */
    public static PointF clampToBurst(PointF pos, PointF burstPos, float threshold) {
        float dx = pos.x - burstPos.x;
        float dy = pos.y - burstPos.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist <= threshold) {
            return new PointF(pos.x, pos.y);
        }

        float scale = threshold / dist;
        return new PointF(
                burstPos.x + dx * scale,
                burstPos.y + dy * scale
        );
    }

    /**
     * Ensures the cursor always has a movement target, mirroring danser-go's behavior.
     *
     * <p>In danser-go, the scheduler never lets the cursor sit idle. When the current
     * movement finishes, the cursor immediately starts the next one. This method
     * provides a fallback target when the mover is finished but no new object is available.
     *
     * @param currentPos  current cursor position
     * @param targetPos   current target position (object position)
     * @param isFinished  whether the current movement is finished
     * @return position to move toward
     */
    public static PointF ensureAlwaysMoving(PointF currentPos, PointF targetPos, boolean isFinished) {
        if (!isFinished) {
            return targetPos;
        }

        float dx = targetPos.x - currentPos.x;
        float dy = targetPos.y - currentPos.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < 0.5f) {
            return new PointF(targetPos.x, targetPos.y);
        }

        float moveSpeed = 0.3f;
        return new PointF(
                currentPos.x + dx * moveSpeed,
                currentPos.y + dy * moveSpeed
        );
    }

    /**
     * Applies cursor freedom (subtle drift) without breaking stacked burst behavior.
     *
     * <p>This mirrors danser-go's "cursor freedom" feature: tiny natural wobble
     * that disappears when the cursor is on a stacked burst.
     *
     * @param basePos     base position from mover
     * @param burstPos    stacked burst position, or null if not in a burst
     * @param driftX      current drift X
     * @param driftY      current drift Y
     * @param maxDrift    maximum drift radius in pixels
     * @return position with drift applied
     */
    public static PointF applyCursorFreedom(PointF basePos, PointF burstPos,
                                            float driftX, float driftY, float maxDrift) {
        if (burstPos != null) {
            float dx = basePos.x - burstPos.x;
            float dy = basePos.y - burstPos.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist < maxDrift) {
                return new PointF(basePos.x + driftX * 0.1f, basePos.y + driftY * 0.1f);
            }
        }

        return new PointF(basePos.x + driftX, basePos.y + driftY);
    }

    /**
     * Computes the stacked burst position from a list of objects.
     *
     * <p>In danser-go, all objects in a burst share the same stacked position.
     * This utility extracts that position from the first object in the burst.
     *
     * @param objects array of objects in the burst (must have at least one element)
     * @return stacked burst position
     */
    public static PointF getBurstPosition(GameObject[] objects) {
        if (objects == null || objects.length == 0 || objects[0] == null) {
            return new PointF(0, 0);
        }
        return new PointF(objects[0].getPosition());
    }
}

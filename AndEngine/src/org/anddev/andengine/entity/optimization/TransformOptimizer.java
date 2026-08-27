package org.anddev.andengine.entity.optimization;

import javax.microedition.khronos.opengles.GL10;

/**
 * GPU rendering optimization for Entity transform pipeline.
 *
 * Merges rotation and scale center translations when centers are equal,
 * and provides a fast path for translation-only entities (the common case).
 *
 * Impact: Eliminates 2-6 glTranslatef calls per entity when rotation/scale centers match.
 */
public final class TransformOptimizer {

    private TransformOptimizer() {}

    /**
     * Returns true if the entity has no rotation and unit scale.
     * Used to skip the full transformation pipeline.
     */
    public static boolean isTranslationOnly(float rotation, float scaleX, float scaleY) {
        return rotation == 0 && scaleX == 1 && scaleY == 1;
    }

    /**
     * Returns true if rotation and scale centers are identical.
     * When true, we can merge the rotation+scale block into a single
     * translate-rotate-scale-translate sequence, saving 2 glTranslatef calls.
     */
    public static boolean centersAreEqual(
            float rotCX, float rotCY, float sclCX, float sclCY) {
        return rotCX == sclCX && rotCY == sclCY;
    }

    /**
     * Optimized combined rotation+scale apply for the common case where
     * both rotation and scale are active and centers are equal.
     *
     * Saves 2 glTranslatef calls compared to separate applyRotation + applyScale.
     *
     * Original (6 GL calls):
     *   glTranslatef(rotCX, rotCY, 0)
     *   glRotatef(rot, 0, 0, 1)
     *   glTranslatef(-rotCX, -rotCY, 0)
     *   glTranslatef(sclCX, sclCY, 0)
     *   glScalef(sx, sy, 1)
     *   glTranslatef(-sclCX, -sclCY, 0)
     *
     * Optimized (4 GL calls when centers equal):
     *   glTranslatef(cx, cy, 0)
     *   glRotatef(rot, 0, 0, 1)
     *   glScalef(sx, sy, 1)
     *   glTranslatef(-cx, -cy, 0)
     */
    public static void applyRotationAndScale(
            GL10 gl,
            float rotation, float scaleX, float scaleY,
            float rotCX, float rotCY, float sclCX, float sclCY) {

        if (rotation == 0) {
            // Rotation is identity — only scale matters
            if (scaleX != 1 || scaleY != 1) {
                gl.glTranslatef(sclCX, sclCY, 0);
                gl.glScalef(scaleX, scaleY, 1);
                gl.glTranslatef(-sclCX, -sclCY, 0);
            }
            return;
        }

        if (scaleX == 1 && scaleY == 1) {
            // Scale is identity — only rotation matters
            gl.glTranslatef(rotCX, rotCY, 0);
            gl.glRotatef(rotation, 0, 0, 1);
            gl.glTranslatef(-rotCX, -rotCY, 0);
            return;
        }

        // Both rotation and scale active
        if (centersAreEqual(rotCX, rotCY, sclCX, sclCY)) {
            // Merged path — saves 2 glTranslatef calls
            gl.glTranslatef(rotCX, rotCY, 0);
            gl.glRotatef(rotation, 0, 0, 1);
            gl.glScalef(scaleX, scaleY, 1);
            gl.glTranslatef(-rotCX, -rotCY, 0);
        } else {
            // Different centers — must apply separately
            gl.glTranslatef(rotCX, rotCY, 0);
            gl.glRotatef(rotation, 0, 0, 1);
            gl.glTranslatef(-rotCX, -rotCY, 0);
            gl.glTranslatef(sclCX, sclCY, 0);
            gl.glScalef(scaleX, scaleY, 1);
            gl.glTranslatef(-sclCX, -sclCY, 0);
        }
    }
}

package org.anddev.andengine.entity.sprite.batch;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.opengl.texture.ITexture;
import org.anddev.andengine.opengl.util.GLHelper;
import org.anddev.andengine.opengl.util.FastFloatBuffer;

/**
 * A {@link DynamicSpriteBatch} extension with per-sprite RGBA colors.
 *
 * The color buffer holds 4 floats (RGBA) per vertex, 6 vertices per rectangle
 * (same rectangle vertex order as {@link SpriteBatchVertexBuffer}).
 *
 * Colors are written by the subclass via {@link #putColor} for every rectangle
 * added in {@link #onUpdateSpriteBatch()}, so the GL upload only covers the
 * used range.
 *
 * Pattern taken from the edlplan TextureQuadBatch which already uses
 * GL_COLOR_ARRAY inside this project.
 */
public abstract class ColoredSpriteBatch extends DynamicSpriteBatch {

    /** 4 floats per vertex (RGBA). */
    private static final int COLOR_FLOATS_PER_VERTEX = 4;
    /** 6 vertices per rectangle (two triangles). */
    private static final int VERTICES_PER_RECT = 6;
    private static final int COLOR_FLOATS_PER_RECT = VERTICES_PER_RECT * COLOR_FLOATS_PER_VERTEX;

    /** Direct native-order RGBA buffer: 4 floats per vertex, 6 vertices per rect. */
    private final FloatBuffer mColorBuffer;

    /** How many rectangles were colored since the last reset. */
    private int mColoredRectCount;

    /** Hardware buffer id (0 = not created yet). */
    private int mColorBufferId = 0;

    public ColoredSpriteBatch(final ITexture pTexture, final int pCapacity) {
        super(pTexture, pCapacity);
        // GL pointer calls (glColorPointer) require a direct native-order Buffer;
        // a heap buffer from FloatBuffer.wrap() throws
        // "IllegalArgumentException: Must use a native order direct Buffer".
        mColorBuffer = ByteBuffer
            .allocateDirect(pCapacity * COLOR_FLOATS_PER_RECT * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
    }

    /**
     * Call at the start of {@link #onUpdateSpriteBatch()}.
     */
    protected final void resetColors() {
        mColoredRectCount = 0;
    }

    /**
     * Writes the color of one rectangle (all 6 vertices) at the given rect
     * index. Must be called for every rectangle added in
     * {@link #onUpdateSpriteBatch()}, with increasing indices starting at 0.
     */
    protected final void putColor(final int pRectIndex, final float pR, final float pG, final float pB, final float pA) {
        final int base = pRectIndex * COLOR_FLOATS_PER_RECT;
        mColorBuffer.position(base);
        for (int v = 0; v < VERTICES_PER_RECT; v++) {
            mColorBuffer.put(pR);
            mColorBuffer.put(pG);
            mColorBuffer.put(pB);
            mColorBuffer.put(pA);
        }
        if (pRectIndex + 1 > mColoredRectCount) {
            mColoredRectCount = pRectIndex + 1;
        }
    }

    @Override
    protected void doDraw(final GL10 pGL, final Camera pCamera) {
        this.onInitDraw(pGL);

        // DynamicSpriteBatch.begin() runs onUpdateSpriteBatch() and submit().
        this.begin(pGL);

        this.onApplyVertices(pGL);
        this.applyTextureRegionAndColor(pGL);

        // mVertices was computed in submit() as mIndex * 6.
        pGL.glDrawArrays(GL10.GL_TRIANGLES, 0, this.mVertices);
        pGL.glDisableClientState(GL10.GL_COLOR_ARRAY);

        this.end(pGL);
    }

    /**
     * Replicates the private SpriteBatch.onApplyTextureRegion and enables the
     * color array afterwards.
     */
    private void applyTextureRegionAndColor(final GL10 pGL) {
        if (GLHelper.EXTENSIONS_VERTEXBUFFEROBJECTS) {
            final GL11 gl11 = (GL11) pGL;

            this.mSpriteBatchTextureRegionBuffer.selectOnHardware(gl11);

            this.mTexture.bind(pGL);
            GLHelper.texCoordZeroPointer(gl11);

            if (mColorBufferId == 0) {
                final int[] ids = new int[1];
                gl11.glGenBuffers(1, ids, 0);
                mColorBufferId = ids[0];
            }
            final int floatCount = Math.max(1, mColoredRectCount * COLOR_FLOATS_PER_RECT);
            mColorBuffer.position(0);
            mColorBuffer.limit(floatCount);
            gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, mColorBufferId);
            gl11.glBufferData(GL11.GL_ARRAY_BUFFER, floatCount * 4, mColorBuffer, GL11.GL_DYNAMIC_DRAW);
            // glVertexPointer-style calls on a bound GL buffer take the data from
            // client memory when a Buffer argument is passed on some drivers; pass
            // the bound buffer id via glColorPointer with offset 0 instead.
            pGL.glEnableClientState(GL10.GL_COLOR_ARRAY);
            pGL.glColorPointer(COLOR_FLOATS_PER_VERTEX, GL10.GL_FLOAT, 0, mColorBuffer);
            gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
        } else {
            this.mTexture.bind(pGL);
            GLHelper.texCoordPointer(pGL, this.mSpriteBatchTextureRegionBuffer.getFloatBuffer());

            pGL.glEnableClientState(GL10.GL_COLOR_ARRAY);
            mColorBuffer.position(0);
            mColorBuffer.limit(mColorBuffer.capacity());
            pGL.glColorPointer(COLOR_FLOATS_PER_VERTEX, GL10.GL_FLOAT, 0, mColorBuffer);
        }
    }
}

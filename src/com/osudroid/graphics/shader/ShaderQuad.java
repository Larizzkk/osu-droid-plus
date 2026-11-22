package com.osudroid.graphics.shader;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * The only geometry the shader effects need: one triangle strip made of four
 * corners. It is uploaded once as a client-side direct buffer and reused for
 * every cursor, so the effect adds no per-frame allocations.
 */
public final class ShaderQuad {

    private static final int FLOATS_PER_VERTEX = 2;

    private final FloatBuffer vertices;

    public ShaderQuad() {
        // aPosition is in the range [-1, 1]; the vertex shader expands it to the
        // final pixel size of the effect.
        float[] data = {
            -1f, -1f,
             1f, -1f,
            -1f,  1f,
             1f,  1f
        };

        vertices = ByteBuffer
            .allocateDirect(data.length * Float.BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        vertices.put(data).position(0);
    }

    public void draw(int positionAttribute) {
        vertices.position(0);
        GLES20.glEnableVertexAttribArray(positionAttribute);
        GLES20.glVertexAttribPointer(
            positionAttribute,
            FLOATS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            FLOATS_PER_VERTEX * Float.BYTES,
            vertices
        );
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(positionAttribute);
    }
}

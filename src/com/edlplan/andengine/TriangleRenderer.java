package com.edlplan.andengine;

import com.edlplan.framework.utils.FloatArraySlice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class TriangleRenderer {

    private static TriangleRenderer triangleRenderer = new TriangleRenderer();
    FloatBuffer buffer;

    public static TriangleRenderer get() {
        return triangleRenderer;
    }

    public synchronized void renderTriangles(FloatArraySlice ver, GL10 pGL) {
        final int len = ver.length;
        final int off = ver.offset;
        if (len <= 0) return;
        if (buffer == null || buffer.capacity() < len) {
            ByteBuffer bb = ByteBuffer.allocateDirect((len + 64) * 4);
            bb.order(ByteOrder.nativeOrder());
            buffer = bb.asFloatBuffer();
        }
        buffer.clear();
        buffer.put(ver.ary, off, len);
        buffer.flip();

        pGL.glVertexPointer(2, GL10.GL_FLOAT, 0, buffer);
        pGL.glDrawArrays(GL10.GL_TRIANGLES, 0, len / 2);
    }

}

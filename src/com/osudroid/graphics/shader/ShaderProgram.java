package com.osudroid.graphics.shader;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Small, allocation-free GLES2 shader program wrapper used by the shader
 * overlay effects. It keeps uniform locations and the attribute location in a
 * cache so the draw path never performs a string lookup.
 */
public final class ShaderProgram {

    private static final String TAG = "ShaderProgram";

    private final int programId;
    private final int positionAttribute;
    private final Map<String, Integer> uniformLocations = new HashMap<>();

    private ShaderProgram(int programId, int positionAttribute) {
        this.programId = programId;
        this.positionAttribute = positionAttribute;
    }

    public static ShaderProgram create(String vertexSource, String fragmentSource) {
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return null;
        }

        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragmentShader == 0) {
            GLES20.glDeleteShader(vertexShader);
            return null;
        }

        int program = GLES20.glCreateProgram();
        if (program == 0) {
            GLES20.glDeleteShader(vertexShader);
            GLES20.glDeleteShader(fragmentShader);
            return null;
        }

        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        int[] status = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e(TAG, "Program link failed: " + GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            GLES20.glDeleteShader(vertexShader);
            GLES20.glDeleteShader(fragmentShader);
            return null;
        }

        // Shaders are linked into the program, the source objects are no longer
        // needed. Deleting them immediately releases driver-side memory.
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        int position = GLES20.glGetAttribLocation(program, "aPosition");
        if (position < 0) {
            Log.e(TAG, "Required attribute aPosition is missing");
            GLES20.glDeleteProgram(program);
            return null;
        }

        return new ShaderProgram(program, position);
    }

    private static int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            return 0;
        }

        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);

        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            String typeName = type == GLES20.GL_VERTEX_SHADER ? "vertex" : "fragment";
            Log.e(TAG, typeName + " shader compile failed: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    public void use() {
        GLES20.glUseProgram(programId);
    }

    public int getPositionAttribute() {
        return positionAttribute;
    }

    public int uniform(String name) {
        Integer cached = uniformLocations.get(name);
        if (cached != null) {
            return cached;
        }
        int location = GLES20.glGetUniformLocation(programId, name);
        uniformLocations.put(name, location);
        return location;
    }

    public void setUniform1f(String name, float value) {
        GLES20.glUniform1f(uniform(name), value);
    }

    public void setUniform2f(String name, float x, float y) {
        GLES20.glUniform2f(uniform(name), x, y);
    }

    public void setUniform1i(String name, int value) {
        GLES20.glUniform1i(uniform(name), value);
    }

    public void setUniform2fv(String name, int count, FloatBuffer values) {
        GLES20.glUniform2fv(uniform(name), count, values);
    }

    public void setUniform1fv(String name, int count, FloatBuffer values) {
        GLES20.glUniform1fv(uniform(name), count, values);
    }

    public void delete() {
        GLES20.glDeleteProgram(programId);
        uniformLocations.clear();
    }
}

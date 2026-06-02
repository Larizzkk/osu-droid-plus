package ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves;

import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.mutils.MUtils;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

import java.util.Arrays;

/**
 * Port of danser-go Spline
 */
public class Spline implements Curve {
    private float[] sections;
    private Curve[] path;
    private float length;

    public Spline(Curve[] curves) {
        sections = new float[curves.length + 1];
        length = 0.0f;

        for (int i = 0; i < curves.length; i++) {
            length += curves[i].getLength();
            sections[i + 1] = length;
        }

        this.path = curves;
    }

    public Spline(Curve[] curves, float[] weights) {
        if (weights.length != curves.length) {
            throw new IllegalArgumentException("incorrect number of weights");
        }

        sections = new float[curves.length + 1];
        length = 0.0f;

        for (int i = 0; i < weights.length; i++) {
            length += weights[i];
            sections[i + 1] = length;
        }

        this.path = curves;
    }

    @Override
    public Vector2f pointAt(float t) {
        float desiredWidth = length * MUtils.clamp(t, 0.0f, 1.0f);

        int index = Arrays.binarySearch(sections, 1, sections.length, desiredWidth);
        if (index < 0) {
            index = -(index + 1);
        }
        index -= 1;
        index = Math.max(0, Math.min(index, path.length - 1));

        if (sections[index + 1] - sections[index] == 0) {
            return path[index].pointAt(0);
        }

        return path[index].pointAt((desiredWidth - sections[index]) / (sections[index + 1] - sections[index]));
    }

    @Override
    public float getLength() {
        return length;
    }

    @Override
    public float getStartAngle() {
        if (path.length > 0) {
            return path[0].getStartAngle();
        }
        return 0.0f;
    }

    @Override
    public float getEndAngle() {
        if (path.length > 0) {
            return path[path.length - 1].getEndAngle();
        }
        return 0.0f;
    }
}

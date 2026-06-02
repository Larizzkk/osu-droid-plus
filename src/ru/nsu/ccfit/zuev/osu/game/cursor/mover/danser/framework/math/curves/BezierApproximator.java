package ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves;

import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Port of danser-go BezierApproximator
 * Based on https://github.com/wieku/danser-go/blob/master/framework/math/curves/bezierapproximator.go
 */
public class BezierApproximator {
    private static final float BEZIER_QUANTIZATION = 0.5f;
    private static final float BEZIER_QUANTIZATIONSQ = BEZIER_QUANTIZATION * BEZIER_QUANTIZATION;

    private int count;
    private Vector2f[] controlPoints;
    private Vector2f[] subdivisionBuffer1;
    private Vector2f[] subdivisionBuffer2;

    public BezierApproximator(Vector2f[] controlPoints) {
        this.count = controlPoints.length;
        this.controlPoints = controlPoints;
        this.subdivisionBuffer1 = new Vector2f[count];
        this.subdivisionBuffer2 = new Vector2f[count * 2 - 1];
    }

    public static boolean isFlatEnough(Vector2f[] controlPoints) {
        for (int i = 1; i < controlPoints.length - 1; i++) {
            if (controlPoints[i - 1].sub(controlPoints[i].scl(2)).add(controlPoints[i + 1]).lenSq() > BEZIER_QUANTIZATIONSQ) {
                return false;
            }
        }
        return true;
    }

    public void subdivide(Vector2f[] controlPoints, Vector2f[] l, Vector2f[] r) {
        Vector2f[] midpoints = subdivisionBuffer1;

        for (int i = 0; i < count; i++) {
            midpoints[i] = controlPoints[i];
        }

        for (int i = 0; i < count; i++) {
            l[i] = midpoints[0];
            r[count - i - 1] = midpoints[count - i - 1];

            for (int j = 0; j < count - i - 1; j++) {
                midpoints[j] = midpoints[j].add(midpoints[j + 1]).scl(0.5f);
            }
        }
    }

    public void approximate(Vector2f[] controlPoints, List<Vector2f> output) {
        Vector2f[] l = subdivisionBuffer2;
        Vector2f[] r = subdivisionBuffer1;

        subdivide(controlPoints, l, r);

        for (int i = 0; i < count - 1; i++) {
            l[count + i] = r[i + 1];
        }

        output.add(controlPoints[0]);

        for (int i = 1; i < count - 1; i++) {
            int index = 2 * i;
            Vector2f p = l[index - 1].add(l[index].scl(2.0f)).add(l[index + 1]).scl(0.25f);
            output.add(p);
        }
    }

    public List<Vector2f> createBezier() {
        List<Vector2f> output = new ArrayList<>();

        if (count == 0) {
            return output;
        }

        Stack<Vector2f[]> toFlatten = new Stack<>();
        Stack<Vector2f[]> freeBuffers = new Stack<>();

        Vector2f[] nCP = new Vector2f[controlPoints.length];
        System.arraycopy(controlPoints, 0, nCP, 0, controlPoints.length);

        toFlatten.push(nCP);

        Vector2f[] leftChild = subdivisionBuffer2;

        while (!toFlatten.isEmpty()) {
            Vector2f[] parent = toFlatten.pop();
            if (isFlatEnough(parent)) {
                approximate(parent, output);
                freeBuffers.push(parent);
                continue;
            }

            Vector2f[] rightChild;
            if (!freeBuffers.isEmpty()) {
                rightChild = freeBuffers.pop();
            } else {
                rightChild = new Vector2f[count];
            }

            subdivide(parent, leftChild, rightChild);

            for (int i = 0; i < count; i++) {
                parent[i] = leftChild[i];
            }

            toFlatten.push(rightChild);
            toFlatten.push(parent);
        }

        output.add(controlPoints[count - 1]);
        return output;
    }
}

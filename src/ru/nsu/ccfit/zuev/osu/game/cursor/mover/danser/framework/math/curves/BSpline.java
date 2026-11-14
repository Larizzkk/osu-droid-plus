package ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves;

import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of danser-go BSpline solver
 */
public class BSpline {
    
    public static List<Bezier> solveBSpline(Vector2f[] points1) {
        int pointsLen = points1.length;
        if (pointsLen < 4) return new ArrayList<>();

        List<Vector2f> points = new ArrayList<>();
        points.add(points1[0]);
        for (int i = 2; i < pointsLen - 2; i++) {
            points.add(points1[i]);
        }
        points.add(points1[pointsLen - 1]);
        points.add(points1[1]);
        points.add(points1[pointsLen - 2]);

        int n = points.size() - 2;

        Vector2f[] d = new Vector2f[n];
        d[0] = points.get(n).sub(points.get(0));
        d[n - 1] = points.get(n + 1).sub(points.get(n - 1)).scl(-1);

        Vector2f[] A = new Vector2f[points.size()];
        float[] Bi = new float[points.size()];

        Bi[1] = -0.25f;
        A[1] = points.get(2).sub(points.get(0)).sub(d[0]).scl(1.0f / 4.0f);
        for (int i = 2; i < n - 1; i++) {
            Bi[i] = -1.0f / (4.0f + Bi[i - 1]);
            A[i] = points.get(i + 1).sub(points.get(i - 1)).sub(A[i - 1]).scl(-1.0f * Bi[i]);
        }

        for (int i = n - 2; i > 0; i--) {
            d[i] = A[i].add(d[i + 1].scl(Bi[i]));
        }

        List<Vector2f> bezierPoints = new ArrayList<>();
        bezierPoints.add(points.get(0));
        bezierPoints.add(points.get(0).add(d[0]));

        for (int i = 1; i < n - 1; i++) {
            bezierPoints.add(points.get(i).sub(d[i]));
            bezierPoints.add(points.get(i));
            bezierPoints.add(points.get(i).add(d[i]));
        }

        bezierPoints.add(points.get(n - 1).sub(d[n - 1]));
        bezierPoints.add(points.get(n - 1));

        List<Bezier> beziers = new ArrayList<>();
        for (int i = 0; i < bezierPoints.size() - 3; i += 3) {
            Vector2f[] bPoints = new Vector2f[]{
                bezierPoints.get(i),
                bezierPoints.get(i + 1),
                bezierPoints.get(i + 2),
                bezierPoints.get(i + 3)
            };
            beziers.add(new Bezier(bPoints, false));
        }

        return beziers;
    }
}

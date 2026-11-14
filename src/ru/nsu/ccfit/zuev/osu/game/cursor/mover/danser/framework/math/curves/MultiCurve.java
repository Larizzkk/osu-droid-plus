package ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.curves;

import ru.nsu.ccfit.zuev.osu.game.cursor.mover.danser.framework.math.vector.Vector2f;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MultiCurve implementation exactly from danser-go
 * Based on https://github.com/wieku/danser-go/blob/master/framework/math/curves/multicurve.go
 */
public class MultiCurve implements Curve {
    
    private float[] sections;
    private LinearCurve[] lines;
    private float length;
    private Vector2f firstPoint;
    private Vector2f[] points;
    
    public MultiCurve(Vector2f[] points) {
        this.points = points;
        this.firstPoint = points[0].copy();
        List<LinearCurve> lineList = new ArrayList<>();
        for (int i = 0; i < points.length - 1; i++) {
            lineList.add(new LinearCurve(points[i], points[i + 1]));
        }
        this.lines = lineList.toArray(new LinearCurve[0]);
        
        this.sections = new float[lines.length + 1];
        this.length = 0;
        for (int i = 0; i < lines.length; i++) {
            this.length += lines[i].getLength();
            this.sections[i + 1] = this.length;
        }
    }
    
    @Override
    public Vector2f pointAt(float t) {
        if (lines.length == 0 || length == 0) {
            return firstPoint;
        }
        
        float desiredWidth = length * Math.max(0, Math.min(1, t));
        
        int index = Arrays.binarySearch(sections, 1, sections.length, desiredWidth);
        if (index < 0) {
            index = -(index + 1);
        }
        index -= 1;
        index = Math.max(0, Math.min(index, lines.length - 1));
        
        if (sections[index + 1] - sections[index] == 0) {
            return lines[index].getStart();
        }
        
        return lines[index].pointAt((desiredWidth - sections[index]) / (sections[index + 1] - sections[index]));
    }
    
    @Override
    public float getStartAngle() {
        if (points.length < 2) {
            return 0;
        }
        return points[0].angleRV(points[1]);
    }
    
    @Override
    public float getEndAngle() {
        if (points.length < 2) {
            return 0;
        }
        return points[points.length - 1].angleRV(points[points.length - 2]);
    }
    
    @Override
    public float getLength() {
        return length;
    }
}

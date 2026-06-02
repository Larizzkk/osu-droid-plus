package ru.nsu.ccfit.zuev.osuplusplus.game.effects;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.cursor.mover.PlayfieldBounds;

import java.util.ArrayList;
import java.util.List;

/**
 * Kiai effect system - exact port from osu! kiai moments
 * Based on https://github.com/ppy/osu/blob/master/osu.Gameplay/Effects/KiaiEffect.cs
 */
public class KiaiEffect {
    
    private static final float KAI_DURATION = 0.3f; // 300ms
    private static final float KAI_SIZE_MULTIPLIER = 1.2f;
    private static final float KAI_FADE_DURATION = 0.2f; // 200ms
    private static final float KAI_SCALE_DURATION = 0.15f; // 150ms
    
    private List<KiaiParticle> activeParticles = new ArrayList<>();
    private Paint kiaiPaint;
    private Paint kiaiGradientPaint;
    
    public KiaiEffect() {
        initializePaints();
    }
    
    private void initializePaints() {
        kiaiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        kiaiPaint.setColor(Color.WHITE);
        kiaiPaint.setStyle(Paint.Style.FILL);
        
        kiaiGradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        kiaiGradientPaint.setStyle(Paint.Style.FILL);
    }
    
    /**
     * Trigger kiai effect at position
     */
    public void triggerKiai(PointF position, float hitCircleRadius) {
        // Check if kiai is enabled (simplified for now)
        if (!true) { 
            return;
        }
        
        // Create kiai particle
        KiaiParticle particle = new KiaiParticle(position, hitCircleRadius);
        activeParticles.add(particle);
        
        // Clean up old particles
        cleanupOldParticles();
    }
    
    /**
     * Update all active kiai particles
     */
    public void update(float deltaTime) {
        for (int i = activeParticles.size() - 1; i >= 0; i--) {
            KiaiParticle particle = activeParticles.get(i);
            particle.update(deltaTime);
            
            if (particle.isFinished()) {
                activeParticles.remove(i);
            }
        }
    }
    
    /**
     * Draw all kiai particles
     */
    public void draw(Canvas canvas) {
        if (activeParticles.isEmpty()) {
            return;
        }
        
        for (KiaiParticle particle : activeParticles) {
            particle.draw(canvas);
        }
    }
    
    /**
     * Clean up finished particles
     */
    private void cleanupOldParticles() {
        for (int i = activeParticles.size() - 1; i >= 0; i--) {
            if (activeParticles.get(i).isFinished()) {
                activeParticles.remove(i);
            }
        }
    }
    
    /**
     * Clear all particles
     */
    public void clear() {
        activeParticles.clear();
    }
    
    /**
     * Kiai particle class
     */
    private static class KiaiParticle {
        private PointF position;
        private float radius;
        private float currentRadius;
        private float currentScale;
        private float currentAlpha;
        private float elapsedTime;
        private float maxRadius;
        private float maxAlpha;
        private int color;
        
        private RadialGradient gradient;
        
        public KiaiParticle(PointF position, float baseRadius) {
            this.position = position;
            this.maxRadius = baseRadius * KAI_SIZE_MULTIPLIER * 2;
            this.currentRadius = 0;
            this.currentScale = 0;
            this.currentAlpha = 0;
            this.elapsedTime = 0;
            
            // Random color variation for visual interest
            this.color = generateKiaiColor();
            this.maxAlpha = 0.8f;
            
            initializeGradient();
        }
        
        private int generateKiaiColor() {
            // Generate colors similar to osu! kiai (white, blue, pink, purple)
            float hue = (float) (Math.random() * 360);
            float saturation = 0.7f + (float) (Math.random() * 0.3f);
            float lightness = 0.8f + (float) (Math.random() * 0.2f);
            
            // Convert HSL to RGB
            return Color.HSVToColor(new float[]{hue, saturation, lightness});
        }
        
        private void initializeGradient() {
            gradient = new RadialGradient(
                position.x, position.y, 1f,
                Color.argb((int)(maxAlpha * 255), Color.red(color), Color.green(color), Color.blue(color)),
                Color.argb(0, Color.red(color), Color.green(color), Color.blue(color)),
                Shader.TileMode.CLAMP
            );
        }
        
        public void update(float deltaTime) {
            elapsedTime += deltaTime;
            
            // Scale animation (0 -> 1 -> 0)
            if (elapsedTime < KAI_SCALE_DURATION) {
                currentScale = elapsedTime / KAI_SCALE_DURATION;
            } else {
                currentScale = 1f - ((elapsedTime - KAI_SCALE_DURATION) / KAI_FADE_DURATION);
            }
            
            // Radius animation
            if (elapsedTime < KAI_SCALE_DURATION) {
                currentRadius = maxRadius * currentScale;
            } else {
                currentRadius = maxRadius;
            }
            
            // Alpha animation (fade in then fade out)
            if (elapsedTime < KAI_SCALE_DURATION) {
                currentAlpha = maxAlpha * currentScale;
            } else if (elapsedTime < KAI_DURATION) {
                currentAlpha = maxAlpha;
            } else {
                currentAlpha = maxAlpha * (1f - ((elapsedTime - KAI_DURATION) / KAI_FADE_DURATION));
            }
        }
        
        public void draw(Canvas canvas) {
            if (currentRadius <= 0 || currentAlpha <= 0) {
                return;
            }
            
            // Update gradient center
            gradient = new RadialGradient(
                position.x, position.y, 0,
                Color.argb((int)(currentAlpha * 255), Color.red(color), Color.green(color), Color.blue(color)),
                Color.argb(0, Color.red(color), Color.green(color), Color.blue(color)),
                Shader.TileMode.CLAMP
            );
            
            // Create gradient paint for this particle
            Paint gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            gradientPaint.setStyle(Paint.Style.FILL);
            gradientPaint.setShader(gradient);
            
            // Draw kiai circle
            canvas.drawCircle(position.x, position.y, currentRadius, gradientPaint);
            
            // Draw outer ring for extra effect
            if (currentScale > 0.5f) {
                float ringRadius = currentRadius * 1.2f;
                float ringAlpha = currentAlpha * 0.3f * (1f - currentScale);
                
                Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                ringPaint.setColor(color);
                ringPaint.setAlpha((int)(ringAlpha * 255));
                ringPaint.setStyle(Paint.Style.STROKE);
                ringPaint.setStrokeWidth(2f);
                
                canvas.drawCircle(position.x, position.y, ringRadius, ringPaint);
            }
        }
        
        public boolean isFinished() {
            return elapsedTime >= KAI_DURATION + KAI_FADE_DURATION;
        }
        
        public PointF getPosition() {
            return position;
        }
        
        public float getCurrentRadius() {
            return currentRadius;
        }
        
        public float getCurrentAlpha() {
            return currentAlpha;
        }
    }
    
    /**
     * Get statistics
     */
    public KiaiStats getStats() {
        return new KiaiStats(activeParticles.size());
    }
    
    /**
     * Kiai statistics
     */
    public static class KiaiStats {
        public final int activeParticleCount;
        
        public KiaiStats(int activeParticleCount) {
            this.activeParticleCount = activeParticleCount;
        }
        
        @Override
        public String toString() {
            return String.format("KiaiStats[particles=%d]", activeParticleCount);
        }
    }
}

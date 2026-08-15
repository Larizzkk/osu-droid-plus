package ru.nsu.ccfit.zuev.osuplusplus.game.effects;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.PointF;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.util.GLHelper;
import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplusplus.ResourceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Improved Glow Effect Manager using proper techniques for AndEngine
 * Based on best practices for OpenGL ES 2.0 glow effects
 */
public class GlowEffectManager {
    
    // Singleton instance
    private static GlowEffectManager instance;
    
    // Glow effect configurations
    public enum GlowType {
        CURSOR_GLOW,
        TRAIL_GLOW,
        OBJECT_GLOW,
        HIT_OBJECT_GLOW
    }
    
    // Glow layers for different intensities
    private static class GlowLayer {
        public Sprite sprite;
        public float scale;
        public float alpha;
        public boolean additive;
        
        public GlowLayer(Sprite sprite, float scale, float alpha, boolean additive) {
            this.sprite = sprite;
            this.scale = scale;
            this.alpha = alpha;
            this.additive = additive;
        }
    }
    
    // List of active glow effects
    private List<GlowEffect> activeEffects;
    
    // Paint for canvas-based glow
    private Paint glowPaint;
    
    private GlowEffectManager() {
        activeEffects = new ArrayList<>();
        initializePaint();
    }
    
    public static GlowEffectManager getInstance() {
        if (instance == null) {
            instance = new GlowEffectManager();
        }
        return instance;
    }
    
    private void initializePaint() {
        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setDither(true);
        glowPaint.setFilterBitmap(true);
    }
    
    /**
     * Create a proper glow effect using multiple layers with additive blending
     */
    public Entity createGlowEntity(GlowType type, TextureRegion texture) {
        Entity glowContainer = new Entity();
        
        float intensity = Config.getGlowIntensity();
        boolean additiveBlending = Config.isAdditiveBlendingEnabled();
        
        switch (type) {
            case CURSOR_GLOW:
                if (!Config.isCursorGlowEnabled()) return null;
                createCursorGlow(glowContainer, texture, intensity, additiveBlending);
                break;
                
            case TRAIL_GLOW:
                if (!Config.isTrailGlowEnabled()) return null;
                createTrailGlow(glowContainer, texture, intensity, additiveBlending);
                break;
                
            case OBJECT_GLOW:
                if (!Config.isObjectGlowEnabled()) return null;
                createObjectGlow(glowContainer, texture, intensity, additiveBlending);
                break;
                
            case HIT_OBJECT_GLOW:
                createHitObjectGlow(glowContainer, texture, intensity, additiveBlending);
                break;
        }
        
        return glowContainer;
    }
    
    private void createCursorGlow(Entity container, TextureRegion texture, float intensity, boolean additive) {
        // Create multi-layer glow for cursor
        float[] scales = {1.5f, 2.2f, 3.0f};
        float[] alphas = {0.6f, 0.3f, 0.15f};
        
        for (int i = 0; i < scales.length; i++) {
            Sprite glowSprite = new Sprite(0, 0, texture);
            glowSprite.setScale(scales[i]);
            glowSprite.setAlpha(alphas[i] * intensity);
            
            if (additive) {
                glowSprite.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
            } else {
                glowSprite.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
            }
            
            container.attachChild(glowSprite);
        }
    }
    
    private void createTrailGlow(Entity container, TextureRegion texture, float intensity, boolean additive) {
        // Create smooth glow for trail
        float[] scales = {1.2f, 1.8f, 2.5f};
        float[] alphas = {0.8f, 0.4f, 0.2f};
        
        for (int i = 0; i < scales.length; i++) {
            Sprite glowSprite = new Sprite(0, 0, texture);
            glowSprite.setScale(scales[i]);
            glowSprite.setAlpha(alphas[i] * intensity);
            
            // Trail always uses additive blending for better effect
            glowSprite.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
            
            container.attachChild(glowSprite);
        }
    }
    
    private void createObjectGlow(Entity container, TextureRegion texture, float intensity, boolean additive) {
        // Create subtle glow for objects
        float[] scales = {1.3f, 1.9f};
        float[] alphas = {0.4f, 0.2f};
        
        for (int i = 0; i < scales.length; i++) {
            Sprite glowSprite = new Sprite(0, 0, texture);
            glowSprite.setScale(scales[i]);
            glowSprite.setAlpha(alphas[i] * intensity);
            
            if (additive) {
                glowSprite.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
            } else {
                glowSprite.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
            }
            
            container.attachChild(glowSprite);
        }
    }
    
    private void createHitObjectGlow(Entity container, TextureRegion texture, float intensity, boolean additive) {
        // Create strong glow for hit objects
        float[] scales = {1.4f, 2.0f, 2.8f};
        float[] alphas = {0.7f, 0.35f, 0.18f};
        
        for (int i = 0; i < scales.length; i++) {
            Sprite glowSprite = new Sprite(0, 0, texture);
            glowSprite.setScale(scales[i]);
            glowSprite.setAlpha(alphas[i] * intensity);
            
            // Hit objects always use additive blending
            glowSprite.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
            
            container.attachChild(glowSprite);
        }
    }
    
    /**
     * Create canvas-based glow effect for custom rendering
     */
    public void drawCanvasGlow(Canvas canvas, PointF position, float radius, int color, float intensity) {
        // Create radial gradient for smooth glow
        RadialGradient gradient = new RadialGradient(
            position.x, position.y, radius * 2f,
            Color.argb((int)(255 * intensity), Color.red(color), Color.green(color), Color.blue(color)),
            Color.argb(0, Color.red(color), Color.green(color), Color.blue(color)),
            Shader.TileMode.CLAMP
        );
        
        glowPaint.setShader(gradient);
        glowPaint.setAlpha((int) (255 * intensity));
        
        canvas.drawCircle(position.x, position.y, radius * 2f, glowPaint);
        
        // Clear shader for next use
        glowPaint.setShader(null);
    }
    
    /**
     * Create optimized glow for trail system
     */
    public void drawTrailGlow(Canvas canvas, PointF position, float size, int color, float progress, float intensity) {
        // Use progressive glow based on trail progress
        float glowRadius = size * (1.5f + progress * 1.5f);
        float alpha = (0.6f * progress * intensity);
        
        RadialGradient gradient = new RadialGradient(
            position.x, position.y, glowRadius,
            Color.argb((int)(255 * alpha), Color.red(color), Color.green(color), Color.blue(color)),
            Color.argb(0, Color.red(color), Color.green(color), Color.blue(color)),
            Shader.TileMode.CLAMP
        );
        
        glowPaint.setShader(gradient);
        glowPaint.setAlpha((int) (255 * alpha));
        
        canvas.drawCircle(position.x, position.y, glowRadius, glowPaint);
        
        glowPaint.setShader(null);
    }
    
    /**
     * Update glow settings based on config
     */
    public void updateSettings() {
        // This method can be called when settings change
        // Implementation depends on specific needs
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        activeEffects.clear();
        instance = null;
    }
    
    /**
     * Glow effect data structure
     */
    public static class GlowEffect {
        public GlowType type;
        public PointF position;
        public float radius;
        public int color;
        public float intensity;
        public float duration;
        public float currentTime;
        
        public GlowEffect(GlowType type, PointF position, float radius, int color, float intensity, float duration) {
            this.type = type;
            this.position = new PointF(position);
            this.radius = radius;
            this.color = color;
            this.intensity = intensity;
            this.duration = duration;
            this.currentTime = 0f;
        }
        
        public boolean update(float deltaTime) {
            currentTime += deltaTime;
            return currentTime < duration;
        }
        
        public float getProgress() {
            return currentTime / duration;
        }
        
        public float getCurrentIntensity() {
            return intensity * (1f - getProgress());
        }
    }
}

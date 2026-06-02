package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.effects;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import java.util.ArrayList;
import java.util.Random;

/**
 * VisualEffectsManager - Enhanced visual effects for better gameplay experience
 * Includes particle effects, screen shake, color transitions, and more
 */
public class VisualEffectsManager {
    
    // Particle system for hit effects
    private static class Particle {
        public PointF position;
        public PointF velocity;
        public float life;
        public float maxLife;
        public float size;
        public int color;
        public float alpha;
        public ParticleType type;
        
        public Particle(PointF pos, PointF vel, float life, float size, int color, ParticleType type) {
            this.position = new PointF(pos);
            this.velocity = new PointF(vel);
            this.life = life;
            this.maxLife = life;
            this.size = size;
            this.color = color;
            this.alpha = 1f;
            this.type = type;
        }
    }
    
    public enum ParticleType {
        HIT_BURST,
        SLIDER_FOLLOW,
        SPINNER_GLOW,
        KIAI_FLASH,
        COMBO_BOOST,
        PERFECT_HIT,
        MISS_EFFECT
    }
    
    private final ArrayList<Particle> particles;
    private final Random random;
    private final Paint particlePaint;
    private final Paint glowPaint;
    
    // Screen shake effect
    private float shakeIntensity = 0f;
    private float shakeDuration = 0f;
    private PointF shakeOffset = new PointF();
    
    // Color transition effects
    private int currentColor = Color.WHITE;
    private int targetColor = Color.WHITE;
    private float colorTransition = 0f;
    private float colorTransitionSpeed = 0.1f;
    
    // Combo effects
    private int combo = 0;
    private float comboEffectIntensity = 0f;
    private long lastComboTime = 0;
    
    // Performance settings
    private boolean enableParticles = true;
    private boolean enableScreenShake = true;
    private boolean enableColorTransitions = true;
    private int maxParticles = 100;
    
    public VisualEffectsManager() {
        this.particles = new ArrayList<>();
        this.random = new Random();
        this.particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.particlePaint.setStyle(Paint.Style.FILL);
        this.glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.glowPaint.setStyle(Paint.Style.FILL);
    }
    
    /**
     * Update all visual effects
     */
    public void update(float deltaTime) {
        // Update particles
        if (enableParticles) {
            updateParticles(deltaTime);
        }
        
        // Update screen shake
        if (enableScreenShake) {
            updateScreenShake(deltaTime);
        }
        
        // Update color transitions
        if (enableColorTransitions) {
            updateColorTransitions(deltaTime);
        }
        
        // Update combo effects
        updateComboEffects(deltaTime);
    }
    
    /**
     * Draw all visual effects
     */
    public void draw(Canvas canvas) {
        // Draw particles
        if (enableParticles) {
            drawParticles(canvas);
        }
        
        // Draw combo effects
        drawComboEffects(canvas);
    }
    
    /**
     * Create hit burst effect at position
     */
    public void createHitBurst(PointF position, int color, float intensity) {
        if (!enableParticles) return;
        
        int particleCount = (int) (10 * intensity);
        for (int i = 0; i < particleCount && particles.size() < maxParticles; i++) {
            float angle = (float) (Math.PI * 2 * i / particleCount);
            float speed = 50f + random.nextFloat() * 100f;
            
            PointF velocity = new PointF(
                (float) Math.cos(angle) * speed,
                (float) Math.sin(angle) * speed
            );
            
            float size = 3f + random.nextFloat() * 5f;
            float life = 0.3f + random.nextFloat() * 0.3f;
            
            particles.add(new Particle(position, velocity, life, size, color, ParticleType.HIT_BURST));
        }
        
        // Add screen shake for powerful hits
        if (intensity > 0.8f && enableScreenShake) {
            addScreenShake(2f * intensity, 0.1f);
        }
    }
    
    /**
     * Create slider follow effect
     */
    public void createSliderFollow(PointF position, int color) {
        if (!enableParticles) return;
        
        PointF velocity = new PointF(
            (random.nextFloat() - 0.5f) * 20f,
            (random.nextFloat() - 0.5f) * 20f
        );
        
        float size = 2f + random.nextFloat() * 3f;
        float life = 0.2f + random.nextFloat() * 0.2f;
        
        particles.add(new Particle(position, velocity, life, size, color, ParticleType.SLIDER_FOLLOW));
    }
    
    /**
     * Create kiai flash effect
     */
    public void createKiaiFlash(PointF position, float radius) {
        if (!enableParticles) return;
        
        int particleCount = 20;
        for (int i = 0; i < particleCount && particles.size() < maxParticles; i++) {
            float angle = (float) (Math.PI * 2 * i / particleCount);
            float speed = 30f + random.nextFloat() * 50f;
            
            PointF velocity = new PointF(
                (float) Math.cos(angle) * speed,
                (float) Math.sin(angle) * speed
            );
            
            float size = 4f + random.nextFloat() * 6f;
            float life = 0.4f + random.nextFloat() * 0.3f;
            
            // Rainbow colors for kiai
            int color = Color.HSVToColor(new float[]{random.nextFloat() * 360f, 1f, 1f});
            
            particles.add(new Particle(position, velocity, life, size, color, ParticleType.KIAI_FLASH));
        }
        
        // Add screen shake for kiai moments
        if (enableScreenShake) {
            addScreenShake(1f, 0.15f);
        }
    }
    
    /**
     * Create combo boost effect
     */
    public void createComboBoost(int comboCount) {
        if (!enableParticles || comboCount < 10) return;
        
        // Create expanding ring effect
        PointF center = new PointF(256, 192); // Screen center
        
        int particleCount = Math.min(comboCount / 5, 30);
        for (int i = 0; i < particleCount && particles.size() < maxParticles; i++) {
            float angle = (float) (Math.PI * 2 * i / particleCount);
            float speed = 100f + random.nextFloat() * 50f;
            
            PointF velocity = new PointF(
                (float) Math.cos(angle) * speed,
                (float) Math.sin(angle) * speed
            );
            
            float size = 2f + random.nextFloat() * 4f;
            float life = 0.5f + random.nextFloat() * 0.3f;
            
            // Gold color for combo
            int color = Color.rgb(255, 215, 0);
            
            particles.add(new Particle(center, velocity, life, size, color, ParticleType.COMBO_BOOST));
        }
        
        // Add screen shake for high combos
        if (comboCount > 50 && enableScreenShake) {
            addScreenShake(1.5f, 0.2f);
        }
    }
    
    /**
     * Create perfect hit effect
     */
    public void createPerfectHit(PointF position) {
        if (!enableParticles) return;
        
        // Create star burst effect
        int particleCount = 8;
        for (int i = 0; i < particleCount && particles.size() < maxParticles; i++) {
            float angle = (float) (Math.PI * 2 * i / particleCount);
            float speed = 80f + random.nextFloat() * 40f;
            
            PointF velocity = new PointF(
                (float) Math.cos(angle) * speed,
                (float) Math.sin(angle) * speed
            );
            
            float size = 3f + random.nextFloat() * 3f;
            float life = 0.3f + random.nextFloat() * 0.2f;
            
            // White-gold color for perfect hits
            int color = Color.rgb(255, 255, 200);
            
            particles.add(new Particle(position, velocity, life, size, color, ParticleType.PERFECT_HIT));
        }
    }
    
    /**
     * Add screen shake effect
     */
    public void addScreenShake(float intensity, float duration) {
        if (!enableScreenShake) return;
        
        this.shakeIntensity = Math.max(this.shakeIntensity, intensity);
        this.shakeDuration = Math.max(this.shakeDuration, duration);
    }
    
    /**
     * Set color transition
     */
    public void setColorTransition(int targetColor, float speed) {
        if (!enableColorTransitions) return;
        
        this.targetColor = targetColor;
        this.colorTransitionSpeed = speed;
        this.colorTransition = 0f;
    }
    
    /**
     * Update combo counter
     */
    public void updateCombo(int combo) {
        this.combo = combo;
        this.lastComboTime = System.currentTimeMillis();
        
        if (combo > 0 && combo % 10 == 0) {
            createComboBoost(combo);
        }
    }
    
    /**
     * Get screen shake offset
     */
    public PointF getScreenShakeOffset() {
        return new PointF(shakeOffset.x, shakeOffset.y);
    }
    
    /**
     * Get current color with transition applied
     */
    public int getCurrentColor() {
        if (colorTransition >= 1f) {
            return targetColor;
        }
        
        // Linear interpolation between colors
        float ratio = colorTransition;
        int a1 = Color.alpha(currentColor);
        int r1 = Color.red(currentColor);
        int g1 = Color.green(currentColor);
        int b1 = Color.blue(currentColor);
        
        int a2 = Color.alpha(targetColor);
        int r2 = Color.red(targetColor);
        int g2 = Color.green(targetColor);
        int b2 = Color.blue(targetColor);
        
        int a = (int) (a1 + (a2 - a1) * ratio);
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);
        
        return Color.argb(a, r, g, b);
    }
    
    private void updateParticles(float deltaTime) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle particle = particles.get(i);
            
            // Update position
            particle.position.x += particle.velocity.x * deltaTime;
            particle.position.y += particle.velocity.y * deltaTime;
            
            // Apply gravity to some particle types
            if (particle.type == ParticleType.HIT_BURST || particle.type == ParticleType.PERFECT_HIT) {
                particle.velocity.y += 200f * deltaTime; // Gravity
            }
            
            // Apply drag
            particle.velocity.x *= 0.98f;
            particle.velocity.y *= 0.98f;
            
            // Update life
            particle.life -= deltaTime;
            particle.alpha = particle.life / particle.maxLife;
            
            // Remove dead particles
            if (particle.life <= 0) {
                particles.remove(i);
            }
        }
    }
    
    private void updateScreenShake(float deltaTime) {
        if (shakeDuration > 0) {
            shakeDuration -= deltaTime;
            
            if (shakeDuration > 0) {
                shakeOffset.x = (random.nextFloat() - 0.5f) * shakeIntensity * 10f;
                shakeOffset.y = (random.nextFloat() - 0.5f) * shakeIntensity * 10f;
            } else {
                shakeOffset.set(0, 0);
                shakeIntensity = 0f;
            }
        }
    }
    
    private void updateColorTransitions(float deltaTime) {
        if (colorTransition < 1f) {
            colorTransition = Math.min(1f, colorTransition + colorTransitionSpeed * deltaTime);
            
            if (colorTransition >= 1f) {
                currentColor = targetColor;
            }
        }
    }
    
    private void updateComboEffects(float deltaTime) {
        // Decay combo effect intensity
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastComboTime > 1000) { // 1 second decay
            comboEffectIntensity = Math.max(0, comboEffectIntensity - deltaTime);
        }
    }
    
    private void drawParticles(Canvas canvas) {
        for (Particle particle : particles) {
            particlePaint.setColor(particle.color);
            particlePaint.setAlpha((int) (particle.alpha * 255));
            
            // Different rendering styles for different particle types
            switch (particle.type) {
                case KIAI_FLASH:
                    // Glowing effect for kiai
                    drawGlowingParticle(canvas, particle);
                    break;
                case COMBO_BOOST:
                    // Ring effect for combo
                    drawRingParticle(canvas, particle);
                    break;
                default:
                    // Regular particle
                    canvas.drawCircle(particle.position.x, particle.position.y, 
                                   particle.size * particle.alpha, particlePaint);
                    break;
            }
        }
    }
    
    private void drawGlowingParticle(Canvas canvas, Particle particle) {
        // Create radial gradient for glow effect
        RadialGradient gradient = new RadialGradient(
            particle.position.x, particle.position.y, particle.size,
            Color.argb((int)(particle.alpha * 128), Color.red(particle.color), 
                       Color.green(particle.color), Color.blue(particle.color)),
            Color.argb(0, Color.red(particle.color), Color.green(particle.color), Color.blue(particle.color)),
            Shader.TileMode.CLAMP
        );
        
        glowPaint.setShader(gradient);
        glowPaint.setAlpha((int) (particle.alpha * 255));
        
        canvas.drawCircle(particle.position.x, particle.position.y, 
                       particle.size * 2f, glowPaint);
    }
    
    private void drawRingParticle(Canvas canvas, Particle particle) {
        particlePaint.setStyle(Paint.Style.STROKE);
        particlePaint.setStrokeWidth(2f * particle.alpha);
        
        canvas.drawCircle(particle.position.x, particle.position.y, 
                       particle.size * (2f - particle.alpha), particlePaint);
        
        particlePaint.setStyle(Paint.Style.FILL);
    }
    
    private void drawComboEffects(Canvas canvas) {
        if (comboEffectIntensity > 0 && combo > 0) {
            // Draw combo counter effect
            String comboText = String.valueOf(combo);
            comboEffectIntensity *= 0.95f; // Decay
            
            // Draw glowing combo text
            particlePaint.setColor(Color.rgb(255, 215, 0));
            particlePaint.setAlpha((int) (comboEffectIntensity * 255));
            particlePaint.setTextSize(48f);
            
            float textX = 256f - particlePaint.measureText(comboText) / 2f;
            float textY = 100f;
            
            canvas.drawText(comboText, textX, textY, particlePaint);
        }
    }
    
    /**
     * Clear all effects
     */
    public void clear() {
        particles.clear();
        shakeOffset.set(0, 0);
        shakeIntensity = 0f;
        shakeDuration = 0f;
        combo = 0;
        comboEffectIntensity = 0f;
    }
    
    /**
     * Set performance settings
     */
    public void setPerformanceSettings(boolean enableParticles, boolean enableScreenShake, 
                                     boolean enableColorTransitions, int maxParticles) {
        this.enableParticles = enableParticles;
        this.enableScreenShake = enableScreenShake;
        this.enableColorTransitions = enableColorTransitions;
        this.maxParticles = maxParticles;
        
        // Clear excess particles if needed
        while (particles.size() > maxParticles) {
            particles.remove(particles.size() - 1);
        }
    }
    
    /**
     * Get performance statistics
     */
    public String getPerformanceStats() {
        return String.format("Particles: %d/%d, Shake: %.2f, Transition: %.2f", 
                           particles.size(), maxParticles, shakeIntensity, colorTransition);
    }
}

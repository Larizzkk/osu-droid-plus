package ru.nsu.ccfit.zuev.osuplusplus.game.effects;

import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.modifier.*;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.modifier.ease.*;
import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osuplusplus.utils.AnimationUtils;
import ru.nsu.ccfit.zuev.osuplusplus.utils.GeometryUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Enhanced particle effects with sound integration
 * Based on osu!lazer's star fountain and particle systems
 */
public class EnhancedParticleEffects {
    
    private static final Random random = new Random();
    private static AudioEffectsIntegrator audioManager;
    
    /**
     * Star fountain effect with sound
     */
    public static class StarFountain {
        private List<StarParticle> particles = new ArrayList<>();
        private PointF position;
        private boolean isActive = false;
        private float shootDirection = 0;
        private long lastShootTime = 0;
        private static final long SHOOT_DURATION = 800;
        private static final int PARTICLES_PER_SECOND = 240;
        
        public StarFountain(float x, float y) {
            this.position = new PointF(x, y);
        }
        
        public void shoot(int direction) {
            shootDirection = direction;
            isActive = true;
            lastShootTime = System.currentTimeMillis();
            
            // Play shoot sound
            if (audioManager != null) {
                audioManager.playUIClick();
            }
        }
        
        public void update(float deltaTime) {
            if (!isActive) return;
            
            long currentTime = System.currentTimeMillis();
            long timeSinceShoot = currentTime - lastShootTime;
            
            if (timeSinceShoot > SHOOT_DURATION) {
                isActive = false;
                // Stop loop sound
                return;
            }
            
            // Generate particles
            float progress = 1.0f - (float) timeSinceShoot / SHOOT_DURATION;
            generateParticles(progress);
            
            // Update existing particles
            updateParticles(deltaTime);
        }
        
        private void generateParticles(float progress) {
            int particlesToGenerate = (int) (PARTICLES_PER_SECOND * 0.016f); // 60 FPS
            
            for (int i = 0; i < particlesToGenerate; i++) {
                StarParticle particle = createStarParticle(progress);
                particles.add(particle);
            }
        }
        
        private StarParticle createStarParticle(float progress) {
            StarParticle particle = new StarParticle();
            
            // Starting position
            particle.x = position.x;
            particle.y = position.y + 50;
            
            // Velocity based on direction and progress
            float baseVelocity = -1400 + random.nextFloat() * 100;
            float xVelocity = shootDirection * 500 * progress + (random.nextFloat() - 0.5f) * 60;
            
            particle.vx = xVelocity;
            particle.vy = baseVelocity;
            
            // Duration
            particle.duration = 300 + random.nextInt(700);
            particle.time = 0;
            
            // Visual properties
            particle.startScale = 0.5f;
            particle.endScale = 2.2f + random.nextFloat() * 0.4f;
            particle.rotation = random.nextFloat() * 360;
            particle.rotationSpeed = (random.nextFloat() - 0.5f) * 180;
            
            // Color (golden stars)
            particle.r = 1.0f;
            particle.g = 0.8f + random.nextFloat() * 0.2f;
            particle.b = 0.2f + random.nextFloat() * 0.3f;
            
            return particle;
        }
        
        private void updateParticles(float deltaTime) {
            for (int i = particles.size() - 1; i >= 0; i--) {
                StarParticle particle = particles.get(i);
                particle.time += deltaTime * 1000;
                
                if (particle.time >= particle.duration) {
                    particles.remove(i);
                    continue;
                }
                
                // Update position
                particle.x += particle.vx * deltaTime;
                particle.y += particle.vy * deltaTime;
                particle.vy += 800 * deltaTime; // Gravity
                
                // Update rotation
                particle.rotation += particle.rotationSpeed * deltaTime;
            }
        }
        
        public List<StarParticle> getParticles() {
            return particles;
        }
        
        public boolean isActive() {
            return isActive;
        }
    }
    
    /**
     * Star particle
     */
    public static class StarParticle {
        public float x, y;
        public float vx, vy;
        public float duration;
        public float time;
        public float startScale, endScale;
        public float rotation, rotationSpeed;
        public float r, g, b;
        public float alpha = 1.0f;
        
        public float getCurrentScale() {
            float progress = time / duration;
            return startScale + (endScale - startScale) * progress;
        }
        
        public float getCurrentAlpha() {
            float progress = time / duration;
            if (progress < 0.8f) {
                return 1.0f;
            } else {
                return 1.0f - (progress - 0.8f) / 0.2f;
            }
        }
    }
    
    /**
     * Logo animation with sound
     */
    public static class LogoAnimation {
        private Sprite logoSprite;
        private float animationProgress = 0;
        private boolean isAnimating = false;
        private float pulseIntensity = 0;
        
        public LogoAnimation(Sprite logoSprite) {
            this.logoSprite = logoSprite;
        }
        
        public void startAnimation() {
            isAnimating = true;
            animationProgress = 0;
            
            // Play logo sound
            if (audioManager != null) {
                audioManager.playUIClick();
            }
            
            // Apply initial animation
            logoSprite.setScale(0.1f);
            logoSprite.setAlpha(0);
            
            // Create entrance animation
            SequenceEntityModifier entrance = new SequenceEntityModifier(
                AnimationUtils.createScale(0.1f, 0.1f, 1.2f, 1.2f, 0.6f, AnimationUtils.EasingType.BACK_OUT),
                AnimationUtils.createScale(1.2f, 1.2f, 0.9f, 0.9f, 0.3f, AnimationUtils.EasingType.BOUNCE_OUT),
                AnimationUtils.createScale(0.9f, 0.9f, 1.0f, 1.0f, 0.3f, AnimationUtils.EasingType.SINE_IN_OUT),
                AnimationUtils.createFadeIn(0.8f, AnimationUtils.EasingType.SINE_OUT)
            );
            
            logoSprite.registerEntityModifier(entrance);
            
            // Start continuous pulse
            LoopEntityModifier pulse = AnimationUtils.createPulse(logoSprite, 0.05f, 2.0f);
            logoSprite.registerEntityModifier(pulse);
        }
        
        public void update(float deltaTime) {
            if (!isAnimating) return;
            
            animationProgress += deltaTime;
            
            // Update pulse intensity based on time
            pulseIntensity = (float) Math.sin(animationProgress * Math.PI * 2) * 0.5f + 0.5f;
            
            // Play outro sound
            if (audioManager != null) {
                audioManager.playUIBack();
            }
            
            // Create exit animation
            SequenceEntityModifier exit = new SequenceEntityModifier(
                AnimationUtils.createScale(1.0f, 1.0f, 1.1f, 1.1f, 0.2f, AnimationUtils.EasingType.SINE_OUT),
                AnimationUtils.createScale(1.1f, 1.1f, 0, 0, 0.5f, AnimationUtils.EasingType.BACK_IN),
                AnimationUtils.createFadeOut(0.5f, AnimationUtils.EasingType.SINE_IN)
            );
            
            logoSprite.registerEntityModifier(exit);
        }
        
        public float getPulseIntensity() {
            return pulseIntensity;
        }
    }
    
    /**
     * Combo burst effect with sound
     */
    public static class ComboBurst {
        private List<BurstParticle> particles = new ArrayList<>();
        private PointF center;
        private int combo;
        
        public ComboBurst(float x, float y, int combo) {
            this.center = new PointF(x, y);
            this.combo = combo;
        }
        
        public void createBurst() {
            int particleCount = Math.min(combo / 10, 20);
            
            // Play combo sound
            if (audioManager != null) {
                audioManager.playUIClick();
            }
            
            for (int i = 0; i < particleCount; i++) {
                BurstParticle particle = createBurstParticle(i, particleCount);
                particles.add(particle);
            }
        }
        
        private BurstParticle createBurstParticle(int index, int total) {
            BurstParticle particle = new BurstParticle();
            
            // Position in circle
            float angle = (float) index / total * 360;
            float radius = 20 + random.nextFloat() * 30;
            
            particle.x = center.x + (float) Math.cos(Math.toRadians(angle)) * radius;
            particle.y = center.y + (float) Math.sin(Math.toRadians(angle)) * radius;
            
            // Velocity outward
            float speed = 200 + random.nextFloat() * 300;
            particle.vx = (float) Math.cos(Math.toRadians(angle)) * speed;
            particle.vy = (float) Math.sin(Math.toRadians(angle)) * speed;
            
            // Duration
            particle.duration = 800 + random.nextInt(400);
            particle.time = 0;
            
            // Visual properties
            particle.size = 5 + random.nextFloat() * 10;
            particle.color = getComboColor(combo);
            
            return particle;
        }
        
        private float[] getComboColor(int combo) {
            // Color based on combo milestones
            if (combo >= 1000) {
                return new float[]{1.0f, 0.2f, 0.8f}; // Purple
            } else if (combo >= 500) {
                return new float[]{1.0f, 0.6f, 0.2f}; // Orange
            } else if (combo >= 200) {
                return new float[]{0.2f, 1.0f, 0.6f}; // Cyan
            } else if (combo >= 100) {
                return new float[]{1.0f, 1.0f, 0.2f}; // Yellow
            } else {
                return new float[]{1.0f, 1.0f, 1.0f}; // White
            }
        }
        
        public void update(float deltaTime) {
            for (int i = particles.size() - 1; i >= 0; i--) {
                BurstParticle particle = particles.get(i);
                particle.time += deltaTime * 1000;
                
                if (particle.time >= particle.duration) {
                    particles.remove(i);
                    continue;
                }
                
                // Update position
                particle.x += particle.vx * deltaTime;
                particle.y += particle.vy * deltaTime;
                
                // Apply friction
                particle.vx *= 0.98f;
                particle.vy *= 0.98f;
                
                // Apply gravity
                particle.vy += 100 * deltaTime;
            }
        }
        
        public List<BurstParticle> getParticles() {
            return particles;
        }
    }
    
    /**
     * Burst particle
     */
    public static class BurstParticle {
        public float x, y;
        public float vx, vy;
        public float duration;
        public float time;
        public float size;
        public float[] color;
        
        public float getCurrentAlpha() {
            float progress = time / duration;
            return 1.0f - progress;
        }
        
        public float getCurrentSize() {
            float progress = time / duration;
            return size * (1.0f + progress * 0.5f);
        }
    }
    
    /**
     * Hit effect with sound
     */
    public static class HitEffect {
        private List<HitParticle> particles = new ArrayList<>();
        private PointF position;
        private boolean isWhistle, isFinish, isClap;
        
        public HitEffect(float x, float y, boolean whistle, boolean finish, boolean clap) {
            this.position = new PointF(x, y);
            this.isWhistle = whistle;
            this.isFinish = finish;
            this.isClap = clap;
            
            createHitParticles();
        }
        
        private void createHitParticles() {
            // Play hit sounds
            if (audioManager != null) {
                audioManager.playUIClick();
            }
            
            // Create visual particles
            int baseParticles = 8;
            if (isFinish) baseParticles += 4;
            if (isClap) baseParticles += 3;
            if (isWhistle) baseParticles += 2;
            
            for (int i = 0; i < baseParticles; i++) {
                HitParticle particle = createHitParticle(i, baseParticles);
                particles.add(particle);
            }
        }
        
        private HitParticle createHitParticle(int index, int total) {
            HitParticle particle = new HitParticle();
            
            // Random position around hit point
            float angle = random.nextFloat() * 360;
            float radius = random.nextFloat() * 20;
            
            particle.x = position.x + (float) Math.cos(Math.toRadians(angle)) * radius;
            particle.y = position.y + (float) Math.sin(Math.toRadians(angle)) * radius;
            
            // Random velocity
            float speed = 50 + random.nextFloat() * 150;
            particle.vx = (float) Math.cos(Math.toRadians(angle)) * speed;
            particle.vy = (float) Math.sin(Math.toRadians(angle)) * speed;
            
            // Duration
            particle.duration = 200 + random.nextInt(300);
            particle.time = 0;
            
            // Visual properties
            particle.size = 3 + random.nextFloat() * 5;
            particle.type = random.nextFloat() < 0.5f ? HitParticle.Type.CIRCLE : HitParticle.Type.STAR;
            
            return particle;
        }
        
        public void update(float deltaTime) {
            for (int i = particles.size() - 1; i >= 0; i--) {
                HitParticle particle = particles.get(i);
                particle.time += deltaTime * 1000;
                
                if (particle.time >= particle.duration) {
                    particles.remove(i);
                    continue;
                }
                
                // Update position
                particle.x += particle.vx * deltaTime;
                particle.y += particle.vy * deltaTime;
                
                // Apply friction
                particle.vx *= 0.95f;
                particle.vy *= 0.95f;
            }
        }
        
        public List<HitParticle> getParticles() {
            return particles;
        }
    }
    
    /**
     * Hit particle
     */
    public static class HitParticle {
        public float x, y;
        public float vx, vy;
        public float duration;
        public float time;
        public float size;
        public Type type;
        
        public enum Type {
            CIRCLE, STAR
        }
        
        public float getCurrentAlpha() {
            float progress = time / duration;
            return 1.0f - progress;
        }
        
        public float getCurrentSize() {
            float progress = time / duration;
            return size * (1.0f - progress * 0.5f);
        }
    }
    
    /**
     * Initialize audio manager
     */
    public static void initialize(android.content.Context context) {
        audioManager = AudioEffectsIntegrator.getInstance(context);
    }
    
    /**
     * Create star fountain at position
     */
    public static StarFountain createStarFountain(float x, float y) {
        return new StarFountain(x, y);
    }
    
    /**
     * Create logo animation
     */
    public static LogoAnimation createLogoAnimation(Sprite logoSprite) {
        return new LogoAnimation(logoSprite);
    }
    
    /**
     * Create combo burst
     */
    public static ComboBurst createComboBurst(float x, float y, int combo) {
        return new ComboBurst(x, y, combo);
    }
    
    /**
     * Create hit effect
     */
    public static HitEffect createHitEffect(float x, float y, boolean whistle, boolean finish, boolean clap) {
        return new HitEffect(x, y, whistle, finish, clap);
    }
    
    /**
     * Utility methods for common effects
     */
    public static void createStarExplosion(float x, float y, int count) {
        // Play explosion sound
        if (audioManager != null) {
            audioManager.playUIClick();
        }
        
        // Create particles
        for (int i = 0; i < count; i++) {
            float angle = (float) i / count * 360;
            float speed = 200 + random.nextFloat() * 200;
            
            // Create star particle moving outward
            StarParticle particle = new StarParticle();
            particle.x = x;
            particle.y = y;
            particle.vx = (float) Math.cos(Math.toRadians(angle)) * speed;
            particle.vy = (float) Math.sin(Math.toRadians(angle)) * speed;
            particle.duration = 500 + random.nextInt(500);
            particle.startScale = 0.5f;
            particle.endScale = 1.5f;
            particle.r = 1.0f;
            particle.g = 0.8f;
            particle.b = 0.2f;
        }
    }
    
    /**
     * Create trail effect
     */
    public static void createTrail(PointF[] path, float particleSpacing) {
        if (path == null || path.length < 2) return;
        
        // Play trail sound
        if (audioManager != null && path.length > 0) {
            audioManager.playUIClick();
        }
        
        // Create particles along path
        for (int i = 0; i < path.length; i++) {
            // Create trail particle
            HitParticle particle = new HitParticle();
            particle.x = path[i].x;
            particle.y = path[i].y;
            particle.vx = 0;
            particle.vy = 20; // Float upward
            particle.duration = 1000;
            particle.size = 2;
            particle.type = HitParticle.Type.CIRCLE;
        }
    }
}

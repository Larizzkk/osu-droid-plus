package ru.nsu.ccfit.zuev.osuplusplus.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Advanced particle system utilities
 * Based on osu!lazer's particle systems
 */
public class ParticleUtils {
    
    private static final Random random = new Random();
    
    // Particle types
    public enum ParticleType {
        CIRCLE, SQUARE, STAR, HEART, DIAMOND, TRIANGLE, SNOWFLAKE, PETAL, LEAF
    }
    
    // Particle behaviors
    public enum ParticleBehavior {
        FALL, FLOAT, EXPLODE, SPIRAL, FADE, BOUNCE, ORBIT
    }
    
    /**
     * Individual particle
     */
    public static class Particle {
        public PointF position;
        public PointF velocity;
        public PointF acceleration;
        public float size;
        public float initialSize;
        public int color;
        public float alpha;
        public float rotation;
        public float rotationSpeed;
        public float life;
        public float maxLife;
        public ParticleType type;
        public ParticleBehavior behavior;
        public boolean active;
        
        public Particle() {
            position = new PointF();
            velocity = new PointF();
            acceleration = new PointF();
            active = false;
        }
        
        public void reset() {
            position.set(0, 0);
            velocity.set(0, 0);
            acceleration.set(0, 0);
            size = 0;
            alpha = 0;
            rotation = 0;
            life = 0;
            active = false;
        }
    }
    
    /**
     * Particle emitter
     */
    public static class ParticleEmitter {
        private final List<Particle> particles;
        private final int maxParticles;
        private final PointF position;
        private final Paint paint;
        
        // Emitter properties
        public ParticleType particleType = ParticleType.CIRCLE;
        public ParticleBehavior behavior = ParticleBehavior.FALL;
        public float emissionRate = 10.0f;
        public float emissionTimer = 0.0f;
        public float particleLife = 2.0f;
        public float particleSize = 5.0f;
        public float sizeVariation = 0.5f;
        public float speed = 100.0f;
        public float speedVariation = 0.5f;
        public int color = 0xFFFFFFFF;
        public boolean colorRandom = false;
        public boolean gravityEnabled = true;
        public float gravity = 200.0f;
        public boolean windEnabled = false;
        public float windForce = 50.0f;
        public boolean fadeEnabled = true;
        public boolean rotationEnabled = false;
        public float rotationSpeed = 180.0f;
        public boolean active = true;
        
        public ParticleEmitter(int maxParticles) {
            this.maxParticles = maxParticles;
            this.particles = new ArrayList<>(maxParticles);
            this.position = new PointF();
            this.paint = new Paint();
            this.paint.setAntiAlias(true);
            
            // Initialize particles
            for (int i = 0; i < maxParticles; i++) {
                particles.add(new Particle());
            }
        }
        
        /**
         * Emit new particles
         */
        public void emit(int count) {
            if (!active) return;
            
            for (int i = 0; i < count; i++) {
                Particle particle = findInactiveParticle();
                if (particle != null) {
                    initializeParticle(particle);
                }
            }
        }
        
        /**
         * Find inactive particle
         */
        private Particle findInactiveParticle() {
            for (Particle particle : particles) {
                if (!particle.active) {
                    return particle;
                }
            }
            return null;
        }
        
        /**
         * Initialize particle properties
         */
        private void initializeParticle(Particle particle) {
            // Position
            particle.position.set(
                position.x + (random.nextFloat() - 0.5f) * 20,
                position.y + (random.nextFloat() - 0.5f) * 20
            );
            
            // Velocity based on behavior
            switch (behavior) {
                case FALL:
                    particle.velocity.set(
                        (random.nextFloat() - 0.5f) * speed,
                        random.nextFloat() * speed * 0.5f
                    );
                    break;
                    
                case FLOAT:
                    particle.velocity.set(
                        (random.nextFloat() - 0.5f) * speed * 0.3f,
                        -random.nextFloat() * speed * 0.5f
                    );
                    break;
                    
                case EXPLODE:
                    float angle = random.nextFloat() * 360;
                    float speedVar = speed * (1 + (random.nextFloat() - 0.5f) * speedVariation);
                    particle.velocity.set(
                        (float) Math.cos(Math.toRadians(angle)) * speedVar,
                        (float) Math.sin(Math.toRadians(angle)) * speedVar
                    );
                    break;
                    
                case SPIRAL:
                    particle.velocity.set(speed, 0);
                    break;
                    
                case BOUNCE:
                    particle.velocity.set(
                        (random.nextFloat() - 0.5f) * speed * 2,
                        -random.nextFloat() * speed
                    );
                    break;
                    
                case ORBIT:
                    float orbitAngle = random.nextFloat() * 360;
                    float orbitRadius = 50 + random.nextFloat() * 50;
                    particle.velocity.set(
                        (float) Math.cos(Math.toRadians(orbitAngle)) * orbitRadius,
                        (float) Math.sin(Math.toRadians(orbitAngle)) * orbitRadius
                    );
                    break;
                    
                default:
                    particle.velocity.set(
                        (random.nextFloat() - 0.5f) * speed,
                        (random.nextFloat() - 0.5f) * speed
                    );
                    break;
            }
            
            // Acceleration
            particle.acceleration.set(0, gravityEnabled ? gravity : 0);
            
            // Size
            particle.initialSize = particleSize * (1 + (random.nextFloat() - 0.5f) * sizeVariation);
            particle.size = particle.initialSize;
            
            // Color
            if (colorRandom) {
                particle.color = getRandomColor();
            } else {
                particle.color = color;
            }
            
            // Life
            particle.maxLife = particleLife * (1 + (random.nextFloat() - 0.5f) * 0.5f);
            particle.life = particle.maxLife;
            
            // Rotation
            particle.rotation = random.nextFloat() * 360;
            particle.rotationSpeed = rotationEnabled ? rotationSpeed * (random.nextFloat() - 0.5f) : 0;
            
            // Type and behavior
            particle.type = particleType;
            particle.behavior = behavior;
            particle.alpha = 1.0f;
            particle.active = true;
        }
        
        /**
         * Update particles
         */
        public void update(float deltaTime) {
            if (!active) return;
            
            // Update emission
            emissionTimer += deltaTime;
            float emissionInterval = 1.0f / emissionRate;
            
            while (emissionTimer >= emissionInterval) {
                emit(1);
                emissionTimer -= emissionInterval;
            }
            
            // Update particles
            for (Particle particle : particles) {
                if (!particle.active) continue;
                
                // Update life
                particle.life -= deltaTime;
                if (particle.life <= 0) {
                    particle.active = false;
                    continue;
                }
                
                // Update physics
                particle.velocity.x += particle.acceleration.x * deltaTime;
                particle.velocity.y += particle.acceleration.y * deltaTime;
                particle.position.x += particle.velocity.x * deltaTime;
                particle.position.y += particle.velocity.y * deltaTime;
                
                // Apply wind
                if (windEnabled) {
                    particle.velocity.x += windForce * deltaTime;
                }
                
                // Update behavior-specific properties
                updateParticleBehavior(particle, deltaTime);
                
                // Update alpha
                if (fadeEnabled) {
                    particle.alpha = particle.life / particle.maxLife;
                }
                
                // Update size
                particle.size = particle.initialSize * (0.5f + 0.5f * particle.alpha);
                
                // Update rotation
                particle.rotation += particle.rotationSpeed * deltaTime;
            }
        }
        
        /**
         * Update particle behavior
         */
        private void updateParticleBehavior(Particle particle, float deltaTime) {
            switch (particle.behavior) {
                case SPIRAL:
                    float spiralAngle = particle.life * 360;
                    float spiralRadius = particle.life * 50;
                    particle.position.x = position.x + (float) Math.cos(Math.toRadians(spiralAngle)) * spiralRadius;
                    particle.position.y = position.y + (float) Math.sin(Math.toRadians(spiralAngle)) * spiralRadius;
                    break;
                    
                case ORBIT:
                    float orbitAngle = particle.life * 180;
                    float orbitRadius = 30 + particle.life * 30;
                    particle.position.x = position.x + (float) Math.cos(Math.toRadians(orbitAngle)) * orbitRadius;
                    particle.position.y = position.y + (float) Math.sin(Math.toRadians(orbitAngle)) * orbitRadius;
                    break;
                    
                case BOUNCE:
                    if (particle.position.y > 500) { // Ground level
                        particle.position.y = 500;
                        particle.velocity.y *= -0.7f; // Bounce with damping
                    }
                    break;
                    
                case FADE:
                    particle.alpha *= 0.98f; // Gradual fade
                    break;
            }
        }
        
        /**
         * Draw particles
         */
        public void draw(Canvas canvas) {
            if (!active) return;
            
            for (Particle particle : particles) {
                if (!particle.active) continue;
                
                paint.setColor(particle.color);
                paint.setAlpha((int) (particle.alpha * 255));
                
                drawParticle(canvas, particle);
            }
        }
        
        /**
         * Draw individual particle
         */
        private void drawParticle(Canvas canvas, Particle particle) {
            canvas.save();
            canvas.translate(particle.position.x, particle.position.y);
            canvas.rotate(particle.rotation);
            
            switch (particle.type) {
                case CIRCLE:
                    canvas.drawCircle(0, 0, particle.size, paint);
                    break;
                    
                case SQUARE:
                    canvas.drawRect(-particle.size, -particle.size, particle.size, particle.size, paint);
                    break;
                    
                case STAR:
                    drawStar(canvas, 0, 0, particle.size, particle.size * 0.5f, 5, paint);
                    break;
                    
                case HEART:
                    drawHeart(canvas, 0, 0, particle.size, paint);
                    break;
                    
                case DIAMOND:
                    drawDiamond(canvas, 0, 0, particle.size, paint);
                    break;
                    
                case TRIANGLE:
                    drawTriangle(canvas, 0, 0, particle.size, paint);
                    break;
                    
                case SNOWFLAKE:
                    drawSnowflake(canvas, 0, 0, particle.size, paint);
                    break;
                    
                case PETAL:
                    drawPetal(canvas, 0, 0, particle.size, paint);
                    break;
                    
                case LEAF:
                    drawLeaf(canvas, 0, 0, particle.size, paint);
                    break;
            }
            
            canvas.restore();
        }
        
        /**
         * Set emitter position
         */
        public void setPosition(float x, float y) {
            position.set(x, y);
        }
        
        /**
         * Get active particle count
         */
        public int getActiveParticleCount() {
            int count = 0;
            for (Particle particle : particles) {
                if (particle.active) count++;
            }
            return count;
        }
        
        /**
         * Clear all particles
         */
        public void clear() {
            for (Particle particle : particles) {
                particle.active = false;
            }
            emissionTimer = 0;
        }
    }
    
    /**
     * Draw star shape
     */
    private static void drawStar(Canvas canvas, float cx, float cy, float outerRadius, float innerRadius, int points, Paint paint) {
        float angle = (float) Math.PI / points;
        float[] x = new float[points * 2];
        float[] y = new float[points * 2];
        
        for (int i = 0; i < points * 2; i++) {
            float radius = (i % 2 == 0) ? outerRadius : innerRadius;
            float currentAngle = i * angle - (float) Math.PI / 2;
            x[i] = cx + (float) Math.cos(currentAngle) * radius;
            y[i] = cy + (float) Math.sin(currentAngle) * radius;
        }
        
        // Simple triangle drawing instead of vertices
                android.graphics.Path path = new android.graphics.Path();
                path.moveTo(cx, cy - outerRadius);
                for (int i = 0; i < points * 2; i++) {
                    float radius = (i % 2 == 0) ? outerRadius : innerRadius;
                    float currentAngle = i * angle - (float) Math.PI / 2;
                    float px = cx + (float) Math.cos(currentAngle) * radius;
                    float py = cy + (float) Math.sin(currentAngle) * radius;
                    path.lineTo(px, py);
                }
                path.close();
                canvas.drawPath(path, paint);
    }
    
    /**
     * Draw heart shape
     */
    private static void drawHeart(Canvas canvas, float cx, float cy, float size, Paint paint) {
        // Simple heart using circles and triangle
        canvas.drawCircle(cx - size * 0.3f, cy - size * 0.3f, size * 0.5f, paint);
        canvas.drawCircle(cx + size * 0.3f, cy - size * 0.3f, size * 0.5f, paint);
        
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(cx, cy + size * 0.5f);
        path.lineTo(cx - size * 0.5f, cy);
        path.lineTo(cx + size * 0.5f, cy);
        path.close();
        canvas.drawPath(path, paint);
    }
    
    /**
     * Draw diamond shape
     */
    private static void drawDiamond(Canvas canvas, float cx, float cy, float size, Paint paint) {
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(cx, cy - size);
        path.lineTo(cx + size, cy);
        path.lineTo(cx, cy + size);
        path.lineTo(cx - size, cy);
        path.close();
        canvas.drawPath(path, paint);
    }
    
    /**
     * Draw triangle shape
     */
    private static void drawTriangle(Canvas canvas, float cx, float cy, float size, Paint paint) {
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(cx, cy - size);
        path.lineTo(cx - size, cy + size);
        path.lineTo(cx + size, cy + size);
        path.close();
        canvas.drawPath(path, paint);
    }
    
    /**
     * Draw snowflake shape
     */
    private static void drawSnowflake(Canvas canvas, float cx, float cy, float size, Paint paint) {
        // Draw 6 lines
        for (int i = 0; i < 6; i++) {
            float angle = i * 60;
            float x = cx + (float) Math.cos(Math.toRadians(angle)) * size;
            float y = cy + (float) Math.sin(Math.toRadians(angle)) * size;
            canvas.drawLine(cx, cy, x, y, paint);
            
            // Add small branches
            float branchX1 = cx + (float) Math.cos(Math.toRadians(angle - 30)) * size * 0.5f;
            float branchY1 = cy + (float) Math.sin(Math.toRadians(angle - 30)) * size * 0.5f;
            float branchX2 = cx + (float) Math.cos(Math.toRadians(angle + 30)) * size * 0.5f;
            float branchY2 = cy + (float) Math.sin(Math.toRadians(angle + 30)) * size * 0.5f;
            
            canvas.drawLine(x, y, branchX1, branchY1, paint);
            canvas.drawLine(x, y, branchX2, branchY2, paint);
        }
    }
    
    /**
     * Draw petal shape
     */
    private static void drawPetal(Canvas canvas, float cx, float cy, float size, Paint paint) {
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(cx, cy - size);
        path.quadTo(cx + size, cy - size * 0.5f, cx + size * 0.5f, cy);
        path.quadTo(cx + size, cy + size * 0.5f, cx, cy + size);
        path.quadTo(cx - size, cy + size * 0.5f, cx - size * 0.5f, cy);
        path.quadTo(cx - size, cy - size * 0.5f, cx, cy - size);
        canvas.drawPath(path, paint);
    }
    
    /**
     * Draw leaf shape
     */
    private static void drawLeaf(Canvas canvas, float cx, float cy, float size, Paint paint) {
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(cx, cy - size);
        path.quadTo(cx + size * 0.5f, cy - size * 0.5f, cx + size * 0.3f, cy);
        path.quadTo(cx + size * 0.5f, cy + size * 0.5f, cx, cy + size);
        path.quadTo(cx - size * 0.5f, cy + size * 0.5f, cx - size * 0.3f, cy);
        path.quadTo(cx - size * 0.5f, cy - size * 0.5f, cx, cy - size);
        canvas.drawPath(path, paint);
    }
    
    /**
     * Get random color
     */
    private static int getRandomColor() {
        return Color.argb(255, 
                         random.nextInt(256), 
                         random.nextInt(256), 
                         random.nextInt(256));
    }
    
    /**
     * Convert float array to float array (helper)
     */
    private static float[] toFloatArray(float[] array) {
        return array;
    }
    
    /**
     * Create explosion effect
     */
    public static ParticleEmitter createExplosion(float x, float y, int particleCount, int color) {
        ParticleEmitter emitter = new ParticleEmitter(particleCount);
        emitter.setPosition(x, y);
        emitter.behavior = ParticleBehavior.EXPLODE;
        emitter.emissionRate = particleCount;
        emitter.particleLife = 1.0f;
        emitter.particleSize = 3.0f;
        emitter.speed = 200.0f;
        emitter.color = color;
        emitter.emit(particleCount);
        return emitter;
    }
    
    /**
     * Create fountain effect
     */
    public static ParticleEmitter createFountain(float x, float y) {
        ParticleEmitter emitter = new ParticleEmitter(100);
        emitter.setPosition(x, y);
        emitter.behavior = ParticleBehavior.FALL;
        emitter.emissionRate = 20.0f;
        emitter.particleLife = 3.0f;
        emitter.particleSize = 4.0f;
        emitter.speed = 150.0f;
        emitter.gravityEnabled = true;
        emitter.gravity = 300.0f;
        return emitter;
    }
    
    /**
     * Create spiral effect
     */
    public static ParticleEmitter createSpiral(float x, float y) {
        ParticleEmitter emitter = new ParticleEmitter(50);
        emitter.setPosition(x, y);
        emitter.behavior = ParticleBehavior.SPIRAL;
        emitter.emissionRate = 10.0f;
        emitter.particleLife = 2.0f;
        emitter.particleSize = 3.0f;
        emitter.rotationEnabled = true;
        return emitter;
    }
}

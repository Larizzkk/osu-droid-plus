package ru.nsu.ccfit.zuev.osuplusplus.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import java.util.ArrayList;
import java.util.List;

/**
 * Main game utilities manager
 * Integrates all utility systems for enhanced gameplay
 */
public class GameUtils {
    
    private static GameUtils instance;
    private final Context context;
    
    // Utility systems
    private final PerformanceUtils performanceMonitor;
    private final AudioUtils audioManager;
    private final SeasonalUtils seasonalSystem;
    private final ParticleUtils particleSystem;
    private final FormatUtils formatHelper;
    private final GeometryUtils geometryHelper;
    
    // Particle emitters
    private final List<ParticleUtils.ParticleEmitter> particleEmitters;
    
    // Game state
    private boolean initialized = false;
    private float gameTime = 0.0f;
    private boolean paused = false;
    
    private GameUtils(Context context) {
        this.context = context;
        this.performanceMonitor = new PerformanceUtils();
        this.audioManager = new AudioUtils();
        this.seasonalSystem = new SeasonalUtils();
        this.particleSystem = new ParticleUtils();
        this.formatHelper = new FormatUtils();
        this.geometryHelper = new GeometryUtils();
        this.particleEmitters = new ArrayList<>();
    }
    
    /**
     * Get singleton instance
     */
    public static GameUtils getInstance(Context context) {
        if (instance == null) {
            instance = new GameUtils(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Initialize all systems
     */
    public void initialize() {
        if (initialized) return;
        
        // Initialize seasonal system
        SeasonalUtils.initialize();
        
        // Initialize audio system
        if (!AudioUtils.isAudioFormatSupported()) {
            android.util.Log.w("GameUtils", "Audio format not fully supported");
        }
        
        // Reset performance stats
        PerformanceUtils.resetStats();
        
        initialized = true;
        android.util.Log.i("GameUtils", "Game utilities initialized");
    }
    
    /**
     * Update all systems
     */
    public void update(float deltaTime) {
        if (!initialized || paused) return;
        
        gameTime += deltaTime;
        
        // Update performance monitoring
        PerformanceUtils.beginFrame();
        
        // Update particle systems
        updateParticleSystems(deltaTime);
        
        // Clean up audio cache periodically
        if (gameTime % 5.0f < deltaTime) {
            AudioUtils.cleanupAudioCache();
        }
        
        // Suggest garbage collection if needed
        if (gameTime % 10.0f < deltaTime) {
            PerformanceUtils.performGCIfNeeded();
        }
    }
    
    /**
     * Update particle systems
     */
    private void updateParticleSystems(float deltaTime) {
        for (int i = particleEmitters.size() - 1; i >= 0; i--) {
            ParticleUtils.ParticleEmitter emitter = particleEmitters.get(i);
            emitter.update(deltaTime);
            
            // Remove inactive emitters
            if (!emitter.active && emitter.getActiveParticleCount() == 0) {
                particleEmitters.remove(i);
            }
        }
    }
    
    /**
     * Render all systems
     */
    public void render(Canvas canvas) {
        if (!initialized) return;
        
        // Draw particle systems
        for (ParticleUtils.ParticleEmitter emitter : particleEmitters) {
            emitter.draw(canvas);
        }
    }
    
    /**
     * Add particle emitter
     */
    public void addParticleEmitter(ParticleUtils.ParticleEmitter emitter) {
        if (emitter != null) {
            particleEmitters.add(emitter);
        }
    }
    
    /**
     * Create explosion effect
     */
    public void createExplosion(float x, float y, int particleCount, int color) {
        ParticleUtils.ParticleEmitter explosion = ParticleUtils.createExplosion(x, y, particleCount, color);
        addParticleEmitter(explosion);
    }
    
    /**
     * Create seasonal particles
     */
    public void createSeasonalParticles(float x, float y) {
        if (!SeasonalUtils.shouldShowSeasonalParticles()) return;
        
        String particleType = SeasonalUtils.getSeasonalParticleType();
        if (particleType == null) return;
        
        ParticleUtils.ParticleType type;
        ParticleUtils.ParticleBehavior behavior;
        int color;
        
        switch (particleType) {
            case "snow":
                type = ParticleUtils.ParticleType.SNOWFLAKE;
                behavior = ParticleUtils.ParticleBehavior.FALL;
                color = 0xFFFFFFFF;
                break;
            case "pumpkin":
                type = ParticleUtils.ParticleType.CIRCLE;
                behavior = ParticleUtils.ParticleBehavior.FLOAT;
                color = 0xFF8B008B;
                break;
            case "heart":
                type = ParticleUtils.ParticleType.HEART;
                behavior = ParticleUtils.ParticleBehavior.FLOAT;
                color = 0xFFFF69B4;
                break;
            case "petal":
                type = ParticleUtils.ParticleType.PETAL;
                behavior = ParticleUtils.ParticleBehavior.FALL;
                color = 0xFFFFB6C1;
                break;
            case "leaf":
                type = ParticleUtils.ParticleType.LEAF;
                behavior = ParticleUtils.ParticleBehavior.FALL;
                color = 0xFF8B4513;
                break;
            default:
                return;
        }
        
        ParticleUtils.ParticleEmitter emitter = new ParticleUtils.ParticleEmitter(50);
        emitter.setPosition(x, y);
        emitter.particleType = type;
        emitter.behavior = behavior;
        emitter.color = color;
        emitter.emissionRate = 10.0f;
        emitter.particleLife = 3.0f;
        emitter.particleSize = 4.0f;
        emitter.gravityEnabled = true;
        emitter.windEnabled = true;
        
        addParticleEmitter(emitter);
    }
    
    /**
     * Get performance quality
     */
    public PerformanceUtils.PerformanceQuality getPerformanceQuality() {
        return PerformanceUtils.getPerformanceQuality();
    }
    
    /**
     * Get suggested quality settings
     */
    public PerformanceUtils.QualitySettings getSuggestedQualitySettings() {
        return PerformanceUtils.suggestQualitySettings();
    }
    
    /**
     * Apply quality settings
     */
    public void applyQualitySettings(PerformanceUtils.QualitySettings settings) {
        // Apply settings to game systems
        SeasonalUtils.setSeasonalEffectsEnabled(settings.enableEffects);
        
        // Clear existing particles if disabled
        if (!settings.enableParticles) {
            particleEmitters.clear();
        }
        
        android.util.Log.i("GameUtils", "Applied quality settings: " + settings.toString());
    }
    
    /**
     * Get formatted accuracy
     */
    public String formatAccuracy(double accuracy) {
        return FormatUtils.formatAccuracy(accuracy);
    }
    
    /**
     * Get formatted star rating
     */
    public String formatStarRating(double starRating) {
        return FormatUtils.formatStarRating(starRating);
    }
    
    /**
     * Get formatted rank
     */
    public String formatRank(int rank) {
        return FormatUtils.formatRank(rank);
    }
    
    /**
     * Get formatted time
     */
    public String formatTime(int seconds) {
        return FormatUtils.formatTime(seconds);
    }
    
    /**
     * Get formatted score
     */
    public String formatScore(long score) {
        return FormatUtils.formatScore(score);
    }
    
    /**
     * Rotate point around origin
     */
    public PointF rotatePoint(PointF point, PointF origin, float angle) {
        return GeometryUtils.rotatePointAroundOrigin(point, origin, angle);
    }
    
    /**
     * Calculate distance between points
     */
    public float calculateDistance(PointF p1, PointF p2) {
        return GeometryUtils.distance(p1, p2);
    }
    
    /**
     * Check if point is in circle
     */
    public boolean isPointInCircle(PointF point, PointF center, float radius) {
        return GeometryUtils.pointInCircle(point, center, radius);
    }
    
    /**
     * Apply audio filter
     */
    public void applyAudioFilter(float[] audioData, AudioUtils.FilterParams params) {
        AudioUtils.applyFilter(audioData, params);
    }
    
    /**
     * Get audio quality
     */
    public AudioUtils.AudioQuality getAudioQuality() {
        return AudioUtils.getAudioQuality();
    }
    
    /**
     * Get seasonal colors
     */
    public int[] getSeasonalColors() {
        return SeasonalUtils.getSeasonalColors();
    }
    
    /**
     * Get seasonal greeting
     */
    public String getSeasonalGreeting() {
        return SeasonalUtils.getSeasonalGreeting();
    }
    
    /**
     * Get seasonal music modifier
     */
    public float getSeasonalMusicModifier() {
        return SeasonalUtils.getSeasonalMusicModifier();
    }
    
    /**
     * Get performance report
     */
    public String getPerformanceReport() {
        return PerformanceUtils.getPerformanceReport();
    }
    
    /**
     * Get detailed stats
     */
    public String getDetailedStats() {
        return PerformanceUtils.getDetailedStats();
    }
    
    /**
     * Check if performance is good
     */
    public boolean isPerformanceGood() {
        return PerformanceUtils.isPerformanceGood();
    }
    
    /**
     * Get current FPS
     */
    public float getCurrentFPS() {
        return PerformanceUtils.getCurrentFPS();
    }
    
    /**
     * Get average FPS
     */
    public float getAverageFPS() {
        return PerformanceUtils.getAverageFPS();
    }
    
    /**
     * Get frame drop rate
     */
    public float getFrameDropRate() {
        return PerformanceUtils.getFrameDropRate();
    }
    
    /**
     * Get memory usage
     */
    public float getMemoryUsageMB() {
        return PerformanceUtils.getMemoryUsageMB();
    }
    
    /**
     * Pause/Resume game
     */
    public void setPaused(boolean paused) {
        this.paused = paused;
        if (paused) {
            // Clear particles when paused
            particleEmitters.clear();
        }
    }
    
    /**
     * Check if game is paused
     */
    public boolean isPaused() {
        return paused;
    }
    
    /**
     * Get game time
     */
    public float getGameTime() {
        return gameTime;
    }
    
    /**
     * Reset all systems
     */
    public void reset() {
        gameTime = 0.0f;
        particleEmitters.clear();
        PerformanceUtils.resetStats();
        SeasonalUtils.updateSeasonalEvent();
        android.util.Log.i("GameUtils", "Game utilities reset");
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        particleEmitters.clear();
        AudioUtils.cleanupAudioCache();
        PerformanceUtils.resetStats();
        android.util.Log.i("GameUtils", "Game utilities cleaned up");
    }
    
    /**
     * Get system information
     */
    public String getSystemInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== Game Utils System Info ===\n");
        info.append("Initialized: ").append(initialized).append("\n");
        info.append("Game Time: ").append(String.format("%.2f", gameTime)).append("s\n");
        info.append("Paused: ").append(paused).append("\n");
        info.append("Active Emitters: ").append(particleEmitters.size()).append("\n");
        info.append("Seasonal Event: ").append(SeasonalUtils.getCurrentEvent().getName()).append("\n");
        info.append("Audio Quality: ").append(AudioUtils.getAudioQuality()).append("\n");
        info.append("Performance Quality: ").append(PerformanceUtils.getPerformanceQuality().getDescription()).append("\n");
        
        return info.toString();
    }
    
    /**
     * Create hit effect at position
     */
    public void createHitEffect(float x, float y, int color) {
        createExplosion(x, y, 15, color);
        
        // Add seasonal particles if enabled
        createSeasonalParticles(x, y);
    }
    
    /**
     * Create combo effect
     */
    public void createComboEffect(float x, float y, int combo) {
        int particleCount = Math.min(10 + combo / 5, 50);
        int color = getComboColor(combo);
        createExplosion(x, y, particleCount, color);
    }
    
    /**
     * Get combo color based on combo count
     */
    private int getComboColor(int combo) {
        if (combo >= 1000) return 0xFFFFD700; // Gold
        if (combo >= 500) return 0xFFC0C0C0; // Silver
        if (combo >= 100) return 0xFFCD7F32; // Bronze
        if (combo >= 50) return 0xFF4169E1; // Blue
        if (combo >= 20) return 0xFF32CD32; // Green
        return 0xFFFFFFFF; // White
    }
    
    /**
     * Create burst effect
     */
    public void createBurstEffect(float x, float y, int particleCount) {
        ParticleUtils.ParticleEmitter burst = new ParticleUtils.ParticleEmitter(particleCount);
        burst.setPosition(x, y);
        burst.behavior = ParticleUtils.ParticleBehavior.EXPLODE;
        burst.emissionRate = particleCount;
        burst.particleLife = 1.5f;
        burst.particleSize = 3.0f;
        burst.speed = 300.0f;
        burst.colorRandom = true;
        burst.fadeEnabled = true;
        burst.rotationEnabled = true;
        burst.emit(particleCount);
        
        addParticleEmitter(burst);
    }
    
    /**
     * Create trail effect
     */
    public void createTrailEffect(PointF[] points, int color) {
        if (points == null || points.length < 2) return;
        
        for (int i = 0; i < points.length - 1; i++) {
            PointF p1 = points[i];
            PointF p2 = points[i + 1];
            
            ParticleUtils.ParticleEmitter trail = new ParticleUtils.ParticleEmitter(5);
            trail.setPosition(p1.x, p1.y);
            trail.behavior = ParticleUtils.ParticleBehavior.FADE;
            trail.emissionRate = 10.0f;
            trail.particleLife = 0.5f;
            trail.particleSize = 2.0f;
            trail.color = color;
            trail.emit(5);
            
            addParticleEmitter(trail);
        }
    }
}

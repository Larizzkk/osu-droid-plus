package ru.nsu.ccfit.zuev.osuplusplus.utils;

import android.os.SystemClock;
import java.util.ArrayList;
import java.util.List;

/**
 * Performance monitoring and optimization utilities
 * Based on osu!lazer's performance monitoring systems
 */
public class PerformanceUtils {
    
    private static class FrameTimeSample {
        public long timestamp;
        public long frameTime;
        public boolean dropped;
        
        public FrameTimeSample(long timestamp, long frameTime, boolean dropped) {
            this.timestamp = timestamp;
            this.frameTime = frameTime;
            this.dropped = dropped;
        }
    }
    
    // Frame time tracking
    private static final List<FrameTimeSample> frameTimeHistory = new ArrayList<>();
    private static final int MAX_FRAME_SAMPLES = 300; // 5 seconds at 60 FPS
    private static long lastFrameTime = 0;
    private static int droppedFrames = 0;
    
    // Performance metrics
    private static float averageFPS = 60.0f;
    private static float worstFPS = 60.0f;
    private static float currentFPS = 60.0f;
    private static float frameDropRate = 0.0f;
    
    // Memory tracking
    private static long lastMemoryCheck = 0;
    private static float memoryUsageMB = 0.0f;
    private static float maxMemoryUsageMB = 0.0f;
    
    // Performance thresholds
    private static final long TARGET_FRAME_TIME = 16666666L; // 60 FPS in nanoseconds
    private static final long DROPPED_FRAME_THRESHOLD = 33333333L; // 30 FPS in nanoseconds
    private static final long MEMORY_CHECK_INTERVAL = 1000000000L; // 1 second in nanoseconds
    
    /**
     * Call this method at the beginning of each frame
     */
    public static void beginFrame() {
        long currentTime = System.nanoTime();
        
        if (lastFrameTime > 0) {
            long frameTime = currentTime - lastFrameTime;
            boolean dropped = frameTime > DROPPED_FRAME_THRESHOLD;
            
            if (dropped) {
                droppedFrames++;
            }
            
            // Add to history
            frameTimeHistory.add(new FrameTimeSample(currentTime, frameTime, dropped));
            
            // Limit history size
            while (frameTimeHistory.size() > MAX_FRAME_SAMPLES) {
                frameTimeHistory.remove(0);
            }
            
            // Update metrics
            updatePerformanceMetrics();
        }
        
        lastFrameTime = currentTime;
        
        // Check memory usage periodically
        if (currentTime - lastMemoryCheck > MEMORY_CHECK_INTERVAL) {
            updateMemoryMetrics();
            lastMemoryCheck = currentTime;
        }
    }
    
    /**
     * Update performance metrics based on frame history
     */
    private static void updatePerformanceMetrics() {
        if (frameTimeHistory.isEmpty()) {
            return;
        }
        
        long totalFrameTime = 0;
        long maxFrameTime = 0;
        int droppedCount = 0;
        
        for (FrameTimeSample sample : frameTimeHistory) {
            totalFrameTime += sample.frameTime;
            maxFrameTime = Math.max(maxFrameTime, sample.frameTime);
            if (sample.dropped) {
                droppedCount++;
            }
        }
        
        long averageFrameTime = totalFrameTime / frameTimeHistory.size();
        
        // Calculate FPS values
        averageFPS = 1000000000.0f / averageFrameTime;
        currentFPS = 1000000000.0f / frameTimeHistory.get(frameTimeHistory.size() - 1).frameTime;
        worstFPS = 1000000000.0f / maxFrameTime;
        frameDropRate = (float) droppedCount / frameTimeHistory.size() * 100.0f;
    }
    
    /**
     * Update memory usage metrics
     */
    private static void updateMemoryMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        memoryUsageMB = usedMemory / (1024.0f * 1024.0f);
        maxMemoryUsageMB = Math.max(maxMemoryUsageMB, memoryUsageMB);
    }
    
    /**
     * Get current FPS
     */
    public static float getCurrentFPS() {
        return currentFPS;
    }
    
    /**
     * Get average FPS over the last few seconds
     */
    public static float getAverageFPS() {
        return averageFPS;
    }
    
    /**
     * Get worst FPS in the recent frame history
     */
    public static float getWorstFPS() {
        return worstFPS;
    }
    
    /**
     * Get frame drop rate as percentage
     */
    public static float getFrameDropRate() {
        return frameDropRate;
    }
    
    /**
     * Get current memory usage in MB
     */
    public static float getMemoryUsageMB() {
        return memoryUsageMB;
    }
    
    /**
     * Get maximum memory usage recorded in MB
     */
    public static float getMaxMemoryUsageMB() {
        return maxMemoryUsageMB;
    }
    
    /**
     * Get total dropped frames count
     */
    public static int getDroppedFrames() {
        return droppedFrames;
    }
    
    /**
     * Reset performance statistics
     */
    public static void resetStats() {
        frameTimeHistory.clear();
        lastFrameTime = 0;
        droppedFrames = 0;
        averageFPS = 60.0f;
        worstFPS = 60.0f;
        currentFPS = 60.0f;
        frameDropRate = 0.0f;
        memoryUsageMB = 0.0f;
        maxMemoryUsageMB = 0.0f;
        lastMemoryCheck = 0;
    }
    
    /**
     * Get performance report string
     */
    public static String getPerformanceReport() {
        return String.format(
            "FPS: %.1f (avg: %.1f, worst: %.1f) | Drops: %.1f%% | Memory: %.1f MB (max: %.1f MB)",
            currentFPS, averageFPS, worstFPS, frameDropRate, memoryUsageMB, maxMemoryUsageMB
        );
    }
    
    /**
     * Check if performance is good
     */
    public static boolean isPerformanceGood() {
        return averageFPS >= 55.0f && frameDropRate < 5.0f;
    }
    
    /**
     * Check if performance is acceptable
     */
    public static boolean isPerformanceAcceptable() {
        return averageFPS >= 45.0f && frameDropRate < 15.0f;
    }
    
    /**
     * Get performance quality level
     */
    public static PerformanceQuality getPerformanceQuality() {
        if (averageFPS >= 58.0f && frameDropRate < 2.0f) {
            return PerformanceQuality.EXCELLENT;
        } else if (averageFPS >= 55.0f && frameDropRate < 5.0f) {
            return PerformanceQuality.GOOD;
        } else if (averageFPS >= 50.0f && frameDropRate < 10.0f) {
            return PerformanceQuality.ACCEPTABLE;
        } else if (averageFPS >= 40.0f && frameDropRate < 20.0f) {
            return PerformanceQuality.POOR;
        } else {
            return PerformanceQuality.VERY_POOR;
        }
    }
    
    /**
     * Performance quality levels
     */
    public enum PerformanceQuality {
        EXCELLENT("Excellent"),
        GOOD("Good"),
        ACCEPTABLE("Acceptable"),
        POOR("Poor"),
        VERY_POOR("Very Poor");
        
        private final String description;
        
        PerformanceQuality(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Suggest quality settings based on performance
     */
    public static QualitySettings suggestQualitySettings() {
        PerformanceQuality quality = getPerformanceQuality();
        
        switch (quality) {
            case EXCELLENT:
                return new QualitySettings(true, true, true, true, 1.0f);
            case GOOD:
                return new QualitySettings(true, true, true, false, 0.9f);
            case ACCEPTABLE:
                return new QualitySettings(true, true, false, false, 0.8f);
            case POOR:
                return new QualitySettings(true, false, false, false, 0.7f);
            case VERY_POOR:
                return new QualitySettings(false, false, false, false, 0.6f);
            default:
                return new QualitySettings(true, true, true, true, 1.0f);
        }
    }
    
    /**
     * Quality settings suggestion
     */
    public static class QualitySettings {
        public final boolean enableParticles;
        public final boolean enableEffects;
        public final boolean enableHighQualityTextures;
        public final boolean enableAdvancedShaders;
        public final float renderScale;
        
        public QualitySettings(boolean enableParticles, boolean enableEffects, 
                            boolean enableHighQualityTextures, boolean enableAdvancedShaders,
                            float renderScale) {
            this.enableParticles = enableParticles;
            this.enableEffects = enableEffects;
            this.enableHighQualityTextures = enableHighQualityTextures;
            this.enableAdvancedShaders = enableAdvancedShaders;
            this.renderScale = renderScale;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Particles: %s, Effects: %s, HQ Textures: %s, Advanced Shaders: %s, Scale: %.1f",
                enableParticles, enableEffects, enableHighQualityTextures, enableAdvancedShaders, renderScale
            );
        }
    }
    
    /**
     * Garbage collection helper
     */
    public static void performGCIfNeeded() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // If memory usage is above 80% of max memory, suggest GC
        if (usedMemory > maxMemory * 0.8) {
            System.gc();
        }
    }
    
    /**
     * Get detailed performance statistics
     */
    public static String getDetailedStats() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return String.format(
            "=== Performance Stats ===\n" +
            "Current FPS: %.1f\n" +
            "Average FPS: %.1f\n" +
            "Worst FPS: %.1f\n" +
            "Frame Drop Rate: %.1f%%\n" +
            "Dropped Frames: %d\n" +
            "Memory Usage: %.1f MB / %.1f MB (%.1f%%)\n" +
            "Max Memory Usage: %.1f MB\n" +
            "Performance Quality: %s\n",
            currentFPS, averageFPS, worstFPS, frameDropRate, droppedFrames,
            usedMemory / (1024.0f * 1024.0f), maxMemory / (1024.0f * 1024.0f),
            (usedMemory * 100.0f) / maxMemory, maxMemoryUsageMB,
            getPerformanceQuality().getDescription()
        );
    }
}

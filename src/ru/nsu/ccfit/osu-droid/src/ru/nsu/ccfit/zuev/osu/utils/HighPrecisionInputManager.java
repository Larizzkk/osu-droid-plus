package ru.nsu.ccfit.zuev.osuplusplus.utils;

import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import org.anddev.andengine.engine.options.TouchOptions;
import ru.nsu.ccfit.zuev.osu.Config;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High precision input manager
 * Optimizes touch processing and prevents stack overflow
 */
public class HighPrecisionInputManager {
    
    // Input optimization settings
    private static final AtomicBoolean highPrecisionEnabled = new AtomicBoolean(true);
    private static final AtomicBoolean historicalEventsEnabled = new AtomicBoolean(true);
    private static final AtomicBoolean rawPointerEnabled = new AtomicBoolean(true);
    
    // Performance metrics
    private static final AtomicInteger touchEventCount = new AtomicInteger(0);
    private static final AtomicInteger processedEventCount = new AtomicInteger(0);
    private static final AtomicInteger droppedEventCount = new AtomicInteger(0);
    private static final AtomicLong lastEventTime = new AtomicLong(0);
    
    // Optimization levels
    private static final AtomicInteger optimizationLevel = new AtomicInteger(3); // 1-5
    private static final float[] OPTIMIZATION_THRESHOLDS = {0.5f, 1.0f, 2.0f, 5.0f, 10.0f}; // ms
    
    // Input validation
    private static final float MIN_TOUCH_DISTANCE = 2.0f;
    private static final long MIN_TOUCH_INTERVAL = 4; // ms
    private static final float MAX_TOUCH_VELOCITY = 10000.0f; // pixels/sec
    
    // Stack protection
    private static final AtomicInteger maxRecursionDepth = new AtomicInteger(0);
    private static final int MAX_RECURSION_DEPTH = 50;
    
    // Touch event filtering
    private static final AtomicBoolean inputFilteringEnabled = new AtomicBoolean(true);
    private static final AtomicBoolean velocityFilteringEnabled = new AtomicBoolean(true);
    
    // Performance monitoring
    private static final AtomicBoolean performanceMonitoringEnabled = new AtomicBoolean(true);
    private static final float TARGET_PROCESSING_TIME = 1.0f; // ms
    private static final AtomicLong averageProcessingTime = new AtomicLong(0);
    
    // Main thread handler
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    /**
     * Initialize high precision input
     */
    public static void initialize() {
        // Apply user settings first
        boolean userEnabled = Config.isHighPrecisionInput();
        highPrecisionEnabled.set(userEnabled);
        
        // Set default optimization level based on device capabilities
        optimizationLevel.set(calculateOptimalOptimizationLevel());
        
        // Start performance monitoring
        if (performanceMonitoringEnabled.get()) {
            startPerformanceMonitoring();
        }
        
        // Initialize memory management
        MemoryManagementUtils.initialize();
        InputOptimizationUtils.initialize();
    }
    
    /**
     * Calculate optimal optimization level
     */
    private static int calculateOptimalOptimizationLevel() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        
        // Adjust optimization level based on available memory
        if (maxMemory < 64 * 1024 * 1024) { // < 64MB
            return 5; // Maximum optimization
        } else if (maxMemory < 128 * 1024 * 1024) { // < 128MB
            return 4; // High optimization
        } else if (maxMemory < 256 * 1024 * 1024) { // < 256MB
            return 3; // Medium optimization
        } else if (maxMemory < 512 * 1024 * 1024) { // < 512MB
            return 2; // Low optimization
        } else {
            return 1; // Minimal optimization
        }
    }
    
    /**
     * Apply high precision touch options
     */
    public static TouchOptions applyHighPrecisionOptions(TouchOptions touchOptions) {
        // Check user setting first
        boolean userEnabled = Config.isHighPrecisionInput();
        if (!userEnabled) {
            return touchOptions;
        }
        
        // Also check internal state
        if (!highPrecisionEnabled.get()) {
            return touchOptions;
        }
        
        touchOptions.setRunOnUpdateThread(true);
        touchOptions.setProcessHistoricalEvents(historicalEventsEnabled.get());
        touchOptions.setUseRawPointer(rawPointerEnabled.get());
        
        return touchOptions;
    }
    
    /**
     * Process touch event with optimization
     */
    public static boolean processTouchEvent(MotionEvent event) {
        // Check user setting first
        boolean userEnabled = Config.isHighPrecisionInput();
        if (!userEnabled) {
            return true; // Let original processing handle it
        }
        
        if (!highPrecisionEnabled.get()) {
            return true; // Let original processing handle it
        }
        
        long startTime = System.nanoTime();
        
        try {
            // Check recursion depth
            int currentDepth = Thread.currentThread().getStackTrace().length;
            if (currentDepth > MAX_RECURSION_DEPTH) {
                droppedEventCount.incrementAndGet();
                return false; // Drop event to prevent stack overflow
            }
            
            // Update max recursion depth
            int currentMax = maxRecursionDepth.get();
            while (currentMax < currentDepth && !maxRecursionDepth.compareAndSet(currentMax, currentDepth)) {
                currentMax = maxRecursionDepth.get();
            }
            
            // Validate event
            if (!isValidTouchEvent(event)) {
                droppedEventCount.incrementAndGet();
                return false;
            }
            
            // Apply optimization filters
            if (!shouldProcessEvent(event)) {
                droppedEventCount.incrementAndGet();
                return false;
            }
            
            // Process event
            touchEventCount.incrementAndGet();
            processedEventCount.incrementAndGet();
            
            // Update processing time metrics
            long processingTime = System.nanoTime() - startTime;
            updateProcessingTimeMetrics(processingTime);
            
            return true;
            
        } catch (StackOverflowError e) {
            handleStackOverflow(event, e);
            return false;
        } catch (Exception e) {
            handleProcessingError(event, e);
            return false;
        }
    }
    
    /**
     * Validate touch event
     */
    private static boolean isValidTouchEvent(MotionEvent event) {
        if (event == null) {
            return false;
        }
        
        // Check event time
        long currentTime = System.currentTimeMillis();
        long eventTime = event.getEventTime();
        long timeSinceLastEvent = currentTime - lastEventTime.get();
        
        if (timeSinceLastEvent < MIN_TOUCH_INTERVAL) {
            return false;
        }
        
        // Check touch distance for move events
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (!isValidTouchMovement(event)) {
                return false;
            }
        }
        
        // Check touch velocity
        if (velocityFilteringEnabled.get() && !isValidTouchVelocity(event)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if touch movement is valid
     */
    private static boolean isValidTouchMovement(MotionEvent event) {
        if (event.getHistorySize() < 2) {
            return true;
        }
        
        int lastIndex = event.getHistorySize() - 1;
        int prevIndex = lastIndex - 1;
        
        float dx = event.getHistoricalX(lastIndex) - event.getHistoricalX(prevIndex);
        float dy = event.getHistoricalY(lastIndex) - event.getHistoricalY(prevIndex);
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        return distance >= MIN_TOUCH_DISTANCE;
    }
    
    /**
     * Check if touch velocity is valid
     */
    private static boolean isValidTouchVelocity(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_MOVE || event.getHistorySize() < 2) {
            return true;
        }
        
        int lastIndex = event.getHistorySize() - 1;
        int prevIndex = lastIndex - 1;
        
        long timeDiff = event.getHistoricalEventTime(lastIndex) - event.getHistoricalEventTime(prevIndex);
        if (timeDiff <= 0) {
            return true;
        }
        
        float dx = event.getHistoricalX(lastIndex) - event.getHistoricalX(prevIndex);
        float dy = event.getHistoricalY(lastIndex) - event.getHistoricalY(prevIndex);
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        float velocity = distance / (timeDiff / 1000.0f); // pixels per second
        
        return velocity <= MAX_TOUCH_VELOCITY;
    }
    
    /**
     * Check if event should be processed based on optimization level
     */
    private static boolean shouldProcessEvent(MotionEvent event) {
        int level = optimizationLevel.get();
        float threshold = OPTIMIZATION_THRESHOLDS[level - 1];
        
        // For move events, apply filtering based on optimization level
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastEvent = currentTime - lastEventTime.get();
            
            switch (level) {
                case 5: // Maximum optimization - process every 10th move
                    return touchEventCount.get() % 10 == 0 && timeSinceLastEvent > threshold;
                case 4: // High optimization - process every 5th move
                    return touchEventCount.get() % 5 == 0 && timeSinceLastEvent > threshold;
                case 3: // Medium optimization - process every 3rd move
                    return touchEventCount.get() % 3 == 0 && timeSinceLastEvent > threshold;
                case 2: // Low optimization - process every 2nd move
                    return touchEventCount.get() % 2 == 0 && timeSinceLastEvent > threshold;
                case 1: // Minimal optimization - process all moves
                default:
                    return timeSinceLastEvent > threshold;
            }
        }
        
        return true;
    }
    
    /**
     * Handle stack overflow
     */
    private static void handleStackOverflow(MotionEvent event, StackOverflowError error) {
        // Force immediate cleanup
        MemoryManagementUtils.forceGC();
        // Stack cleanup is now handled internally
        
        // Lower optimization level
        optimizationLevel.set(5); // Maximum optimization
        
        // Disable high precision temporarily
        highPrecisionEnabled.set(false);
        
        // Re-enable after delay
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                highPrecisionEnabled.set(true);
                optimizationLevel.set(calculateOptimalOptimizationLevel());
            }
        }, 2000);
        
        System.err.println("STACK OVERFLOW in touch processing");
    }
    
    /**
     * Handle processing error
     */
    private static void handleProcessingError(MotionEvent event, Exception error) {
        // Lower optimization level
        int currentLevel = optimizationLevel.get();
        optimizationLevel.set(Math.min(5, currentLevel + 1));
        
        System.err.println("Error processing touch event: " + error.getMessage());
    }
    
    /**
     * Update processing time metrics
     */
    private static void updateProcessingTimeMetrics(long processingTimeNanos) {
        long processingTimeMicros = processingTimeNanos / 1000;
        
        // Exponential moving average
        long alpha = 100; // Smoothing factor
        long currentAvg = averageProcessingTime.get();
        long newAvg = (currentAvg * (alpha - 1) + processingTimeMicros) / alpha;
        averageProcessingTime.set(newAvg);
        
        // Auto-adjust optimization level
        float processingTimeMs = processingTimeMicros / 1000.0f;
        if (processingTimeMs > TARGET_PROCESSING_TIME * 2) {
            // Increase optimization (lower number = higher optimization)
            int currentLevel = optimizationLevel.get();
            optimizationLevel.set(Math.min(5, currentLevel + 1));
        } else if (processingTimeMs < TARGET_PROCESSING_TIME * 0.5) {
            // Decrease optimization
            int currentLevel = optimizationLevel.get();
            optimizationLevel.set(Math.max(1, currentLevel - 1));
        }
    }
    
    /**
     * Start performance monitoring
     */
    private static void startPerformanceMonitoring() {
        new Thread(() -> {
            while (true) {
                try {
                    monitorPerformance();
                    Thread.sleep(1000); // Monitor every second
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "TouchPerformanceMonitor").start();
    }
    
    /**
     * Monitor performance and adjust settings
     */
    private static void monitorPerformance() {
        if (!performanceMonitoringEnabled.get()) {
            return;
        }
        
        // Check memory usage
        MemoryManagementUtils.MemoryStatus memoryStatus = MemoryManagementUtils.getMemoryStatus();
        
        // Adjust settings based on memory status
        switch (memoryStatus) {
            case EMERGENCY:
                highPrecisionEnabled.set(false);
                historicalEventsEnabled.set(false);
                rawPointerEnabled.set(false);
                optimizationLevel.set(5);
                break;
            case CRITICAL:
                optimizationLevel.set(4);
                break;
            case WARNING:
                optimizationLevel.set(3);
                break;
            case NORMAL:
                // Use calculated optimal level
                optimizationLevel.set(calculateOptimalOptimizationLevel());
                break;
        }
        
        // Check processing time
        float avgProcessingTime = averageProcessingTime.get() / 1000.0f;
        if (avgProcessingTime > TARGET_PROCESSING_TIME * 3) {
            // Processing is very slow, increase optimization
            int currentLevel = optimizationLevel.get();
            optimizationLevel.set(Math.min(5, currentLevel + 1));
        }
    }
    
    /**
     * Get touch statistics
     */
    public static String getTouchStatistics() {
        float dropRate = touchEventCount.get() > 0 ? 
            (float) droppedEventCount.get() / touchEventCount.get() * 100 : 0;
        
        return String.format(
            "Touch Events: %d, Processed: %d, Dropped: %.1f%%, Opt Level: %d, Avg Time: %.2fms",
            touchEventCount.get(),
            processedEventCount.get(),
            dropRate,
            optimizationLevel.get(),
            averageProcessingTime.get() / 1000.0f
        );
    }
    
    /**
     * Get performance statistics
     */
    public static String getPerformanceStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== High Precision Input ===\n");
        stats.append(getTouchStatistics()).append("\n");
        stats.append("High Precision: ").append(highPrecisionEnabled.get()).append("\n");
        stats.append("Historical Events: ").append(historicalEventsEnabled.get()).append("\n");
        stats.append("Raw Pointer: ").append(rawPointerEnabled.get()).append("\n");
        stats.append("Input Filtering: ").append(inputFilteringEnabled.get()).append("\n");
        stats.append("Velocity Filtering: ").append(velocityFilteringEnabled.get()).append("\n");
        stats.append("Max Recursion Depth: ").append(maxRecursionDepth.get()).append("\n");
        stats.append("\n");
        stats.append(MemoryManagementUtils.getMemoryStatistics());
        
        return stats.toString();
    }
    
    /**
     * Enable/disable high precision input
     */
    public static void setHighPrecisionEnabled(boolean enabled) {
        highPrecisionEnabled.set(enabled);
    }
    
    /**
     * Update settings from user config
     */
    public static void updateFromUserConfig() {
        boolean userEnabled = Config.isHighPrecisionInput();
        highPrecisionEnabled.set(userEnabled);
        
        // Update optimization level based on current performance
        if (userEnabled) {
            optimizationLevel.set(calculateOptimalOptimizationLevel());
        } else {
            optimizationLevel.set(5); // Maximum optimization when disabled
        }
    }
    
    /**
     * Enable/disable historical events
     */
    public static void setHistoricalEventsEnabled(boolean enabled) {
        historicalEventsEnabled.set(enabled);
    }
    
    /**
     * Enable/disable raw pointer
     */
    public static void setRawPointerEnabled(boolean enabled) {
        rawPointerEnabled.set(enabled);
    }
    
    /**
     * Enable/disable input filtering
     */
    public static void setInputFilteringEnabled(boolean enabled) {
        inputFilteringEnabled.set(enabled);
    }
    
    /**
     * Enable/disable velocity filtering
     */
    public static void setVelocityFilteringEnabled(boolean enabled) {
        velocityFilteringEnabled.set(enabled);
    }
    
    /**
     * Set optimization level (1-5)
     */
    public static void setOptimizationLevel(int level) {
        optimizationLevel.set(Math.max(1, Math.min(5, level)));
    }
    
    /**
     * Get current optimization level
     */
    public static int getOptimizationLevel() {
        return optimizationLevel.get();
    }
    
    /**
     * Reset statistics
     */
    public static void resetStatistics() {
        touchEventCount.set(0);
        processedEventCount.set(0);
        droppedEventCount.set(0);
        averageProcessingTime.set(0);
        maxRecursionDepth.set(0);
    }
    
    /**
     * Get recommended settings based on current state
     */
    public static RecommendedSettings getRecommendedSettings() {
        MemoryManagementUtils.MemoryStatus memoryStatus = MemoryManagementUtils.getMemoryStatus();
        float avgProcessingTime = averageProcessingTime.get() / 1000.0f;
        
        boolean highPrecision = true;
        boolean historicalEvents = true;
        boolean rawPointer = true;
        int optimizationLevel = 3;
        
        // Adjust based on memory status
        switch (memoryStatus) {
            case EMERGENCY:
                highPrecision = false;
                historicalEvents = false;
                rawPointer = false;
                optimizationLevel = 5;
                break;
            case CRITICAL:
                optimizationLevel = 4;
                break;
            case WARNING:
                optimizationLevel = 3;
                break;
            case NORMAL:
                if (avgProcessingTime < 1.0f) {
                    optimizationLevel = 2;
                } else if (avgProcessingTime > 2.0f) {
                    optimizationLevel = 4;
                }
                break;
        }
        
        return new RecommendedSettings(highPrecision, historicalEvents, rawPointer, optimizationLevel);
    }
    
    /**
     * Recommended settings
     */
    public static class RecommendedSettings {
        public final boolean highPrecision;
        public final boolean historicalEvents;
        public final boolean rawPointer;
        public final int optimizationLevel;
        
        public RecommendedSettings(boolean highPrecision, boolean historicalEvents, 
                                boolean rawPointer, int optimizationLevel) {
            this.highPrecision = highPrecision;
            this.historicalEvents = historicalEvents;
            this.rawPointer = rawPointer;
            this.optimizationLevel = optimizationLevel;
        }
        
        public void apply() {
            HighPrecisionInputManager.setHighPrecisionEnabled(highPrecision);
            HighPrecisionInputManager.setHistoricalEventsEnabled(historicalEvents);
            HighPrecisionInputManager.setRawPointerEnabled(rawPointer);
            HighPrecisionInputManager.setOptimizationLevel(optimizationLevel);
        }
        
        public TouchOptions applyToTouchOptions(TouchOptions touchOptions) {
            return HighPrecisionInputManager.applyHighPrecisionOptions(touchOptions);
        }
    }
}

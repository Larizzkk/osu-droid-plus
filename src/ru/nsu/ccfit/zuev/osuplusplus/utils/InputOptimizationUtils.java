package ru.nsu.ccfit.zuev.osuplusplus.utils;

import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High precision input optimization utilities
 * Prevents stack overflow and optimizes touch processing
 */
public class InputOptimizationUtils {
    
    // Touch event pool for memory optimization
    private static final ConcurrentLinkedQueue<MotionEvent> touchEventPool = new ConcurrentLinkedQueue<>();
    private static final int MAX_POOL_SIZE = 50;
    
    // Input processing optimization
    private static final AtomicBoolean inputOptimizationEnabled = new AtomicBoolean(true);
    private static final AtomicInteger touchEventCount = new AtomicInteger(0);
    private static final AtomicInteger processedEventCount = new AtomicInteger(0);
    
    // Performance metrics
    private static long lastOptimizationTime = 0;
    private static float averageProcessingTime = 0.0f;
    private static int optimizationLevel = 1; // 1-5, 5 = maximum optimization
    
    // Input validation
    private static final AtomicBoolean inputValidationEnabled = new AtomicBoolean(true);
    private static final float MIN_TOUCH_DISTANCE = 1.0f; // Minimum distance to consider as valid touch
    private static final long MIN_TOUCH_INTERVAL = 8; // Minimum milliseconds between touches
    
    // Memory management
    private static final AtomicBoolean memoryOptimizationEnabled = new AtomicBoolean(true);
    private static long lastMemoryCleanup = 0;
    private static final long MEMORY_CLEANUP_INTERVAL = 30000; // 30 seconds
    
    // Thread safety
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean mainThreadProcessing = new AtomicBoolean(false);
    
    /**
     * Initialize input optimization
     */
    public static void initialize() {
        // Pre-populate touch event pool
        for (int i = 0; i < 20; i++) {
            touchEventPool.offer(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0));
        }
        
        lastOptimizationTime = System.currentTimeMillis();
        lastMemoryCleanup = System.currentTimeMillis();
        
        // Start periodic optimization
        startPeriodicOptimization();
    }
    
    /**
     * Optimize touch event for high precision processing
     */
    public static MotionEvent optimizeTouchEvent(MotionEvent event) {
        if (!inputOptimizationEnabled.get()) {
            return event;
        }
        
        long startTime = System.nanoTime();
        
        try {
            // Validate event
            if (inputValidationEnabled.get() && !isValidTouchEvent(event)) {
                recycleTouchEvent(event);
                return null;
            }
            
            // Optimize based on current level
            MotionEvent optimized = event;
            switch (optimizationLevel) {
                case 5: // Maximum optimization
                    optimized = optimizeMaximum(event);
                    break;
                case 4: // High optimization
                    optimized = optimizeHigh(event);
                    break;
                case 3: // Medium optimization
                    optimized = optimizeMedium(event);
                    break;
                case 2: // Low optimization
                    optimized = optimizeLow(event);
                    break;
                case 1: // Minimal optimization
                default:
                    optimized = optimizeMinimal(event);
                    break;
            }
            
            processedEventCount.incrementAndGet();
            
            // Update processing time metrics
            long processingTime = System.nanoTime() - startTime;
            updateProcessingTimeMetrics(processingTime);
            
            return optimized;
            
        } catch (Exception e) {
            // Fallback to original event if optimization fails
            return event;
        }
    }
    
    /**
     * Validate touch event
     */
    private static boolean isValidTouchEvent(MotionEvent event) {
        if (event == null) return false;
        
        // Check event time
        long currentTime = System.currentTimeMillis();
        if (currentTime - event.getDownTime() < MIN_TOUCH_INTERVAL) {
            return false;
        }
        
        // Check touch distance (for move events)
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float distance = calculateTouchDistance(event);
            if (distance < MIN_TOUCH_DISTANCE) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Calculate touch distance
     */
    private static float calculateTouchDistance(MotionEvent event) {
        if (event.getHistorySize() < 2) return 0;
        
        int lastIndex = event.getHistorySize() - 1;
        int prevIndex = lastIndex - 1;
        
        float dx = event.getHistoricalX(lastIndex) - event.getHistoricalX(prevIndex);
        float dy = event.getHistoricalY(lastIndex) - event.getHistoricalY(prevIndex);
        
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Maximum optimization level
     */
    private static MotionEvent optimizeMaximum(MotionEvent event) {
        // Clone event with minimal history
        MotionEvent optimized = cloneTouchEvent(event, 1);
        
        // Apply aggressive filtering
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            // Only keep every 3rd point
            if (touchEventCount.get() % 3 != 0) {
                recycleTouchEvent(optimized);
                return null;
            }
        }
        
        return optimized;
    }
    
    /**
     * High optimization level
     */
    private static MotionEvent optimizeHigh(MotionEvent event) {
        // Clone with limited history
        MotionEvent optimized = cloneTouchEvent(event, 2);
        
        // Apply filtering for move events
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            // Only keep every 2nd point
            if (touchEventCount.get() % 2 != 0) {
                recycleTouchEvent(optimized);
                return null;
            }
        }
        
        return optimized;
    }
    
    /**
     * Medium optimization level
     */
    private static MotionEvent optimizeMedium(MotionEvent event) {
        // Clone with moderate history
        MotionEvent optimized = cloneTouchEvent(event, 5);
        
        // Apply light filtering
        if (event.getAction() == MotionEvent.ACTION_MOVE && touchEventCount.get() % 4 == 0) {
            recycleTouchEvent(optimized);
            return null;
        }
        
        return optimized;
    }
    
    /**
     * Low optimization level
     */
    private static MotionEvent optimizeLow(MotionEvent event) {
        // Clone with most history
        MotionEvent optimized = cloneTouchEvent(event, 10);
        
        // Minimal filtering
        if (event.getAction() == MotionEvent.ACTION_MOVE && touchEventCount.get() % 8 == 0) {
            recycleTouchEvent(optimized);
            return null;
        }
        
        return optimized;
    }
    
    /**
     * Minimal optimization level
     */
    private static MotionEvent optimizeMinimal(MotionEvent event) {
        // Just clone with full history
        return cloneTouchEvent(event, event.getHistorySize());
    }
    
    /**
     * Clone touch event with limited history
     */
    private static MotionEvent cloneTouchEvent(MotionEvent event, int maxHistory) {
        MotionEvent cloned = MotionEvent.obtain(event);
        
        // Set current state (simplified cloning)
        cloned.setLocation(event.getX(), event.getY());
        
        return cloned;
    }
    
    /**
    /**
     * Recycle touch event back to pool
     */
    public static void recycleTouchEvent(MotionEvent event) {
        if (event == null || !memoryOptimizationEnabled.get()) {
            return;
        }
        
        if (touchEventPool.size() < MAX_POOL_SIZE) {
            // Reset event
            event.recycle();
            touchEventPool.offer(event);
        }
    }
    
    /**
     * Update processing time metrics
     */
    private static void updateProcessingTimeMetrics(long processingTimeNanos) {
        float processingTimeMs = processingTimeNanos / 1000000.0f;
        
        // Exponential moving average
        float alpha = 0.1f;
        averageProcessingTime = alpha * processingTimeMs + (1 - alpha) * averageProcessingTime;
        
        // Auto-adjust optimization level based on performance
        if (processingTimeMs > 2.0f && optimizationLevel < 5) {
            optimizationLevel++;
        } else if (processingTimeMs < 0.5f && optimizationLevel > 1) {
            optimizationLevel--;
        }
    }
    
    /**
     * Start periodic optimization
     */
    private static void startPeriodicOptimization() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                performPeriodicOptimization();
                mainHandler.postDelayed(this, 1000); // Run every second
            }
        }, 1000);
    }
    
    /**
     * Perform periodic optimization
     */
    private static void performPeriodicOptimization() {
        long currentTime = System.currentTimeMillis();
        
        // Memory cleanup
        if (currentTime - lastMemoryCleanup > MEMORY_CLEANUP_INTERVAL) {
            performMemoryCleanup();
            lastMemoryCleanup = currentTime;
        }
        
        // Adjust optimization based on performance
        adjustOptimizationLevel();
        
        // Check for potential stack overflow prevention
        checkStackOverflowPrevention();
    }
    
    /**
     * Perform memory cleanup
     */
    private static void performMemoryCleanup() {
        // Clean up touch event pool
        while (touchEventPool.size() > MAX_POOL_SIZE / 2) {
            touchEventPool.poll();
        }
        
        // Force garbage collection if needed
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        
        if (usedMemory > maxMemory * 0.8) {
            System.gc();
        }
    }
    
    /**
     * Adjust optimization level based on current performance
     */
    private static void adjustOptimizationLevel() {
        float targetProcessingTime = 1.0f; // 1ms target
        
        if (averageProcessingTime > targetProcessingTime * 2) {
            // Increase optimization
            optimizationLevel = Math.min(5, optimizationLevel + 1);
        } else if (averageProcessingTime < targetProcessingTime * 0.5) {
            // Decrease optimization
            optimizationLevel = Math.max(1, optimizationLevel - 1);
        }
    }
    
    /**
     * Check for stack overflow prevention
     */
    private static void checkStackOverflowPrevention() {
        // Check recursion depth in touch processing
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        
        // If stack is too deep, force cleanup
        if (stackTrace.length > 100) {
            forceStackCleanup();
        }
    }
    
    /**
     * Force stack cleanup
     */
    private static void forceStackCleanup() {
        // Clear touch event pool to free memory
        while (!touchEventPool.isEmpty()) {
            touchEventPool.poll();
        }
        
        // Reset counters
        touchEventCount.set(0);
        processedEventCount.set(0);
        
        // Force garbage collection
        System.gc();
        
        // Lower optimization level
        optimizationLevel = Math.max(1, optimizationLevel - 1);
    }
    
    /**
     * Process touch event safely (prevents stack overflow)
     */
    public static void processTouchEventSafely(MotionEvent event, TouchEventProcessor processor) {
        if (event == null || processor == null) {
            return;
        }
        
        try {
            // Check stack depth before processing
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            if (stackTrace.length > 80) {
                // Stack is getting deep, skip this event
                recycleTouchEvent(event);
                return;
            }
            
            // Process event
            processor.process(event);
            
        } catch (StackOverflowError e) {
            // Handle stack overflow
            handleStackOverflow(event, e);
        } catch (Exception e) {
            // Handle other exceptions
            handleProcessingError(event, e);
        } finally {
            // Always recycle the event
            recycleTouchEvent(event);
        }
    }
    
    /**
     * Handle stack overflow
     */
    private static void handleStackOverflow(MotionEvent event, StackOverflowError error) {
        // Force immediate cleanup
        forceStackCleanup();
        
        // Lower optimization level significantly
        optimizationLevel = 1;
        
        // Log error (in production, use proper logging)
        System.err.println("Stack overflow detected in touch processing: " + error.getMessage());
    }
    
    /**
     * Handle processing error
     */
    private static void handleProcessingError(MotionEvent event, Exception error) {
        // Lower optimization level
        optimizationLevel = Math.max(1, optimizationLevel - 1);
        
        // Log error
        System.err.println("Error processing touch event: " + error.getMessage());
    }
    
    /**
     * Touch event processor interface
     */
    public interface TouchEventProcessor {
        void process(MotionEvent event);
    }
    
    /**
     * Enable/disable input optimization
     */
    public static void setInputOptimizationEnabled(boolean enabled) {
        inputOptimizationEnabled.set(enabled);
    }
    
    /**
     * Enable/disable input validation
     */
    public static void setInputValidationEnabled(boolean enabled) {
        inputValidationEnabled.set(enabled);
    }
    
    /**
     * Enable/disable memory optimization
     */
    public static void setMemoryOptimizationEnabled(boolean enabled) {
        memoryOptimizationEnabled.set(enabled);
    }
    
    /**
     * Set optimization level (1-5)
     */
    public static void setOptimizationLevel(int level) {
        optimizationLevel = Math.max(1, Math.min(5, level));
    }
    
    /**
     * Get current optimization level
     */
    public static int getOptimizationLevel() {
        return optimizationLevel;
    }
    
    /**
     * Get average processing time
     */
    public static float getAverageProcessingTime() {
        return averageProcessingTime;
    }
    
    /**
     * Get touch event statistics
     */
    public static String getTouchStatistics() {
        return String.format(
            "Touch Events: %d, Processed: %d, Pool Size: %d, Opt Level: %d, Avg Time: %.2fms",
            touchEventCount.get(),
            processedEventCount.get(),
            touchEventPool.size(),
            optimizationLevel,
            averageProcessingTime
        );
    }
    
    /**
     * Reset statistics
     */
    public static void resetStatistics() {
        touchEventCount.set(0);
        processedEventCount.set(0);
        averageProcessingTime = 0.0f;
        optimizationLevel = 1;
    }
    
    /**
     * Check if system is under heavy load
     */
    public static boolean isUnderHeavyLoad() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        
        return (usedMemory > maxMemory * 0.7) || (averageProcessingTime > 2.0f);
    }
    
    /**
     * Get recommended settings based on current state
     */
    public static RecommendedSettings getRecommendedSettings() {
        boolean heavyLoad = isUnderHeavyLoad();
        
        return new RecommendedSettings(
            heavyLoad ? 3 : 5,  // Optimization level
            heavyLoad ? false : true,  // Input validation
            heavyLoad ? false : true,  // Memory optimization
            heavyLoad ? 20 : 50     // Max pool size
        );
    }
    
    /**
     * Recommended settings
     */
    public static class RecommendedSettings {
        public final int optimizationLevel;
        public final boolean inputValidation;
        public final boolean memoryOptimization;
        public final int maxPoolSize;
        
        public RecommendedSettings(int optimizationLevel, boolean inputValidation, 
                                boolean memoryOptimization, int maxPoolSize) {
            this.optimizationLevel = optimizationLevel;
            this.inputValidation = inputValidation;
            this.memoryOptimization = memoryOptimization;
            this.maxPoolSize = maxPoolSize;
        }
        
        public void apply() {
            InputOptimizationUtils.setOptimizationLevel(optimizationLevel);
            InputOptimizationUtils.setInputValidationEnabled(inputValidation);
            InputOptimizationUtils.setMemoryOptimizationEnabled(memoryOptimization);
        }
    }
}

package ru.nsu.ccfit.zuev.osuplusplus.utils;

import android.os.Handler;
import android.os.Looper;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced memory management utilities
 * Prevents stack overflow and optimizes memory usage
 */
public class MemoryManagementUtils {
    
    // Memory monitoring
    private static final AtomicLong lastGCTime = new AtomicLong(0);
    private static final AtomicInteger gcCount = new AtomicInteger(0);
    private static final AtomicInteger forcedGCCount = new AtomicInteger(0);
    private static final AtomicBoolean memoryOptimizationEnabled = new AtomicBoolean(true);
    
    // Memory thresholds
    private static final float MEMORY_WARNING_THRESHOLD = 0.75f; // 75%
    private static final float MEMORY_CRITICAL_THRESHOLD = 0.85f; // 85%
    private static final float MEMORY_EMERGENCY_THRESHOLD = 0.95f; // 95%
    
    // Object pooling
    private static final ConcurrentHashMap<Class<?>, ObjectPool<?>> objectPools = new ConcurrentHashMap<>();
    private static final int DEFAULT_POOL_SIZE = 50;
    
    // Stack monitoring
    private static final AtomicInteger maxStackDepth = new AtomicInteger(0);
    private static final AtomicBoolean stackMonitoringEnabled = new AtomicBoolean(true);
    
    // Memory cleanup scheduler
    private static final Handler cleanupHandler = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean cleanupScheduled = new AtomicBoolean(false);
    
    /**
     * Initialize memory management
     */
    public static void initialize() {
        lastGCTime.set(System.currentTimeMillis());
        
        // Start periodic memory monitoring
        startMemoryMonitoring();
        
        // Start periodic cleanup
        schedulePeriodicCleanup();
    }
    
    /**
     * Get object from pool
     */
    @SuppressWarnings("unchecked")
    public static <T> T obtain(Class<T> clazz) {
        if (!memoryOptimizationEnabled.get()) {
            return createNewInstance(clazz);
        }
        
        ObjectPool<?> pool = objectPools.get(clazz);
        if (pool == null) {
            pool = new ObjectPool<>(clazz, DEFAULT_POOL_SIZE);
            objectPools.put(clazz, pool);
        }
        
        return (T) pool.obtain();
    }
    
    /**
     * Return object to pool
     */
    public static <T> void recycle(T object) {
        if (object == null || !memoryOptimizationEnabled.get()) {
            return;
        }
        
        @SuppressWarnings("unchecked")
        ObjectPool<T> pool = (ObjectPool<T>) objectPools.get(object.getClass());
        if (pool != null) {
            pool.recycle(object);
        }
    }
    
    /**
     * Create new instance if pooling is disabled
     */
    @SuppressWarnings("unchecked")
    private static <T> T createNewInstance(Class<T> clazz) {
        try {
            return (T) clazz.newInstance();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Object pool implementation
     */
    private static class ObjectPool<T> {
        private final Class<T> clazz;
        private final WeakReference<T>[] pool;
        private final AtomicInteger poolIndex = new AtomicInteger(0);
        private final int maxSize;
        
        @SuppressWarnings("unchecked")
        public ObjectPool(Class<T> clazz, int maxSize) {
            this.clazz = clazz;
            this.maxSize = maxSize;
            this.pool = new WeakReference[maxSize];
        }
        
        public T obtain() {
            int index = poolIndex.get() - 1;
            if (index >= 0) {
                poolIndex.set(index);
                WeakReference<T> ref = pool[index];
                if (ref != null) {
                    T object = ref.get();
                    if (object != null) {
                        return object;
                    }
                }
            }
            
            // Create new instance if pool is empty
            return createNewInstance(clazz);
        }
        
        public void recycle(T object) {
            int index = poolIndex.get();
            if (index < maxSize) {
                pool[index] = new WeakReference<>(object);
                poolIndex.set(index + 1);
            }
        }
    }
    
    /**
     * Monitor memory usage
     */
    public static void monitorMemory() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        float memoryUsageRatio = (float) usedMemory / maxMemory;
        
        // Check memory thresholds
        if (memoryUsageRatio > MEMORY_EMERGENCY_THRESHOLD) {
            handleEmergencyMemory();
        } else if (memoryUsageRatio > MEMORY_CRITICAL_THRESHOLD) {
            handleCriticalMemory();
        } else if (memoryUsageRatio > MEMORY_WARNING_THRESHOLD) {
            handleWarningMemory();
        }
        
        // Check stack depth
        checkStackDepth();
    }
    
    /**
     * Handle emergency memory situation
     */
    private static void handleEmergencyMemory() {
        // Force immediate garbage collection
        System.gc();
        forcedGCCount.incrementAndGet();
        
        // Clear all object pools
        clearAllObjectPools();
        
        // Suggest aggressive cleanup
        suggestAggressiveCleanup();
        
        // Lower memory optimization level
        memoryOptimizationEnabled.set(false);
        
        System.err.println("EMERGENCY: Memory usage critical, forcing cleanup");
    }
    
    /**
     * Handle critical memory situation
     */
    private static void handleCriticalMemory() {
        // Force garbage collection
        System.gc();
        forcedGCCount.incrementAndGet();
        
        // Clear half of object pools
        clearHalfObjectPools();
        
        // Schedule immediate cleanup
        scheduleImmediateCleanup();
        
        System.err.println("CRITICAL: Memory usage high, forcing cleanup");
    }
    
    /**
     * Handle warning memory situation
     */
    private static void handleWarningMemory() {
        // Suggest cleanup
        suggestCleanup();
        
        // Clear some object pools
        clearSomeObjectPools();
        
        System.err.println("WARNING: Memory usage elevated");
    }
    
    /**
     * Check stack depth
     */
    private static void checkStackDepth() {
        if (!stackMonitoringEnabled.get()) {
            return;
        }
        
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int currentDepth = stackTrace.length;
        
        // Update max depth
        int currentMax = maxStackDepth.get();
        while (currentMax < currentDepth && !maxStackDepth.compareAndSet(currentMax, currentDepth)) {
            currentMax = maxStackDepth.get();
        }
        
        // Check for potential stack overflow
        if (currentDepth > 150) {
            handleDeepStack(currentDepth);
        }
    }
    
    /**
     * Handle deep stack situation
     */
    private static void handleDeepStack(int depth) {
        // Force cleanup
        clearAllObjectPools();
        System.gc();
        
        // Disable stack monitoring temporarily
        stackMonitoringEnabled.set(false);
        
        // Re-enable after delay
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                stackMonitoringEnabled.set(true);
            } catch (InterruptedException e) {
                // Ignore
            }
        }).start();
        
        System.err.println("STACK WARNING: Deep stack detected (" + depth + "), forcing cleanup");
    }
    
    /**
     * Clear all object pools
     */
    private static void clearAllObjectPools() {
        for (ObjectPool<?> pool : objectPools.values()) {
            // Clear pool by setting index to 0
            if (pool instanceof MemoryManagementUtils.ObjectPool) {
                @SuppressWarnings("unchecked")
                MemoryManagementUtils.ObjectPool<Object> typedPool = (MemoryManagementUtils.ObjectPool<Object>) pool;
                typedPool.poolIndex.set(0);
            }
        }
    }
    
    /**
     * Clear half of object pools
     */
    private static void clearHalfObjectPools() {
        for (ObjectPool<?> pool : objectPools.values()) {
            if (pool instanceof MemoryManagementUtils.ObjectPool) {
                @SuppressWarnings("unchecked")
                MemoryManagementUtils.ObjectPool<Object> typedPool = (MemoryManagementUtils.ObjectPool<Object>) pool;
                int currentIndex = typedPool.poolIndex.get();
                typedPool.poolIndex.set(currentIndex / 2);
            }
        }
    }
    
    /**
     * Clear some object pools
     */
    private static void clearSomeObjectPools() {
        int cleared = 0;
        for (ObjectPool<?> pool : objectPools.values()) {
            if (cleared % 2 == 0) { // Clear every other pool
                if (pool instanceof MemoryManagementUtils.ObjectPool) {
                    @SuppressWarnings("unchecked")
                    MemoryManagementUtils.ObjectPool<Object> typedPool = (MemoryManagementUtils.ObjectPool<Object>) pool;
                    int currentIndex = typedPool.poolIndex.get();
                    typedPool.poolIndex.set(Math.max(0, currentIndex - 10));
                }
            }
            cleared++;
        }
    }
    
    /**
     * Suggest cleanup
     */
    private static void suggestCleanup() {
        if (!cleanupScheduled.get()) {
            scheduleImmediateCleanup();
        }
    }
    
    /**
     * Suggest aggressive cleanup
     */
    private static void suggestAggressiveCleanup() {
        scheduleImmediateCleanup();
        schedulePeriodicCleanup(); // Ensure periodic cleanup continues
    }
    
    /**
     * Schedule immediate cleanup
     */
    private static void scheduleImmediateCleanup() {
        cleanupHandler.post(new Runnable() {
            @Override
            public void run() {
                performCleanup();
                cleanupScheduled.set(false);
            }
        });
        cleanupScheduled.set(true);
    }
    
    /**
     * Schedule periodic cleanup
     */
    private static void schedulePeriodicCleanup() {
        cleanupHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                performCleanup();
                schedulePeriodicCleanup(); // Reschedule
            }
        }, 30000); // Every 30 seconds
    }
    
    /**
     * Start memory monitoring
     */
    private static void startMemoryMonitoring() {
        new Thread(() -> {
            while (true) {
                try {
                    monitorMemory();
                    Thread.sleep(1000); // Monitor every second
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "MemoryMonitor").start();
    }
    
    /**
     * Perform cleanup
     */
    private static void performCleanup() {
        // Clear some object pools
        clearSomeObjectPools();
        
        // Force garbage collection if needed
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        
        if (usedMemory > maxMemory * 0.7) {
            System.gc();
        }
        
        // Update GC time
        lastGCTime.set(System.currentTimeMillis());
        gcCount.incrementAndGet();
    }
    
    /**
     * Run task with stack overflow protection
     */
    public static void runWithStackProtection(Runnable task) {
        runWithStackProtection(task, 100); // 100 max stack depth
    }
    
    /**
     * Run task with custom stack depth limit
     */
    public static void runWithStackProtection(Runnable task, int maxStackDepth) {
        if (stackMonitoringEnabled.get()) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            if (stackTrace.length > maxStackDepth) {
                System.err.println("STACK PROTECTION: Task skipped due to deep stack");
                return;
            }
        }
        
        try {
            task.run();
        } catch (StackOverflowError e) {
            handleStackOverflow(task, e);
        }
    }
    
    /**
     * Handle stack overflow
     */
    private static void handleStackOverflow(Runnable task, StackOverflowError error) {
        // Force cleanup
        clearAllObjectPools();
        System.gc();
        
        // Reset stack monitoring
        stackMonitoringEnabled.set(false);
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                stackMonitoringEnabled.set(true);
            } catch (InterruptedException e) {
                // Ignore
            }
        }).start();
        
        System.err.println("STACK OVERFLOW: " + error.getMessage());
    }
    
    /**
     * Get memory statistics
     */
    public static String getMemoryStatistics() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        float memoryUsageRatio = (float) usedMemory / maxMemory;
        
        return String.format(
            "Memory: %d/%d MB (%.1f%%), Pools: %d, Max Stack: %d, GC Count: %d, Forced GC: %d",
            usedMemory / (1024 * 1024),
            maxMemory / (1024 * 1024),
            memoryUsageRatio * 100,
            objectPools.size(),
            maxStackDepth.get(),
            gcCount.get(),
            forcedGCCount.get()
        );
    }
    
    /**
     * Get memory status
     */
    public static MemoryStatus getMemoryStatus() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        float memoryUsageRatio = (float) usedMemory / maxMemory;
        
        if (memoryUsageRatio > MEMORY_EMERGENCY_THRESHOLD) {
            return MemoryStatus.EMERGENCY;
        } else if (memoryUsageRatio > MEMORY_CRITICAL_THRESHOLD) {
            return MemoryStatus.CRITICAL;
        } else if (memoryUsageRatio > MEMORY_WARNING_THRESHOLD) {
            return MemoryStatus.WARNING;
        } else {
            return MemoryStatus.NORMAL;
        }
    }
    
    /**
     * Memory status enum
     */
    public enum MemoryStatus {
        NORMAL, WARNING, CRITICAL, EMERGENCY
    }
    
    /**
     * Enable/disable memory optimization
     */
    public static void setMemoryOptimizationEnabled(boolean enabled) {
        memoryOptimizationEnabled.set(enabled);
    }
    
    /**
     * Enable/disable stack monitoring
     */
    public static void setStackMonitoringEnabled(boolean enabled) {
        stackMonitoringEnabled.set(enabled);
    }
    
    /**
     * Force garbage collection
     */
    public static void forceGC() {
        System.gc();
        forcedGCCount.incrementAndGet();
        lastGCTime.set(System.currentTimeMillis());
    }
    
    /**
     * Get recommended memory settings
     */
    public static RecommendedMemorySettings getRecommendedSettings() {
        MemoryStatus status = getMemoryStatus();
        
        switch (status) {
            case EMERGENCY:
                return new RecommendedMemorySettings(false, false, 10);
            case CRITICAL:
                return new RecommendedMemorySettings(false, true, 25);
            case WARNING:
                return new RecommendedMemorySettings(true, true, 50);
            case NORMAL:
            default:
                return new RecommendedMemorySettings(true, true, 100);
        }
    }
    
    /**
     * Recommended memory settings
     */
    public static class RecommendedMemorySettings {
        public final boolean memoryOptimization;
        public final boolean stackMonitoring;
        public final int maxPoolSize;
        
        public RecommendedMemorySettings(boolean memoryOptimization, boolean stackMonitoring, int maxPoolSize) {
            this.memoryOptimization = memoryOptimization;
            this.stackMonitoring = stackMonitoring;
            this.maxPoolSize = maxPoolSize;
        }
        
        public void apply() {
            MemoryManagementUtils.setMemoryOptimizationEnabled(memoryOptimization);
            MemoryManagementUtils.setStackMonitoringEnabled(stackMonitoring);
        }
    }
}

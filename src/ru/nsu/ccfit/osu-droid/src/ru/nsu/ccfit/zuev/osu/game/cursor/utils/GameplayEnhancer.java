package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.utils;

import android.graphics.PointF;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Vibrator;
import android.content.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * GameplayEnhancer - Utilities for enhanced gameplay experience
 * Includes haptic feedback, audio cues, timing assistance, and more
 */
public class GameplayEnhancer {
    
    // Timing assistance
    private static class TimingWindow {
        public float perfectWindow;
        public float goodWindow;
        public float okWindow;
        
        public TimingWindow(float perfect, float good, float ok) {
            this.perfectWindow = perfect;
            this.goodWindow = good;
            this.okWindow = ok;
        }
    }
    
    // Audio feedback system
    private static class AudioCue {
        public int frequency;
        public int duration;
        public float volume;
        
        public AudioCue(int frequency, int duration, float volume) {
            this.frequency = frequency;
            this.duration = duration;
            this.volume = volume;
        }
    }
    
    // Haptic feedback patterns
    private static class HapticPattern {
        public long[] pattern;
        public int amplitude;
        
        public HapticPattern(long[] pattern, int amplitude) {
            this.pattern = pattern;
            this.amplitude = amplitude;
        }
    }
    
    private Context context;
    private Vibrator vibrator;
    private ToneGenerator toneGenerator;
    private AudioManager audioManager;
    
    // Settings
    private boolean enableHapticFeedback = true;
    private boolean enableAudioCues = true;
    private boolean enableTimingAssistance = false;
    private boolean enableVisualHints = true;
    private boolean enableAntiMash = true;
    
    // Timing windows (in milliseconds)
    private TimingWindow timingWindows = new TimingWindow(50f, 100f, 150f);
    
    // Audio cues mapping
    private Map<String, AudioCue> audioCues = new HashMap<>();
    
    // Haptic patterns
    private Map<String, HapticPattern> hapticPatterns = new HashMap<>();
    
    // Performance tracking
    private ArrayList<Float> recentTimings = new ArrayList<>();
    private int consecutiveHits = 0;
    private int maxConsecutiveHits = 0;
    private float averageTimingError = 0f;
    
    // Anti-mash protection
    private long lastHitTime = 0;
    private float minHitInterval = 50f; // Minimum time between hits
    
    public GameplayEnhancer(Context context) {
        this.context = context;
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        this.toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        
        initializeAudioCues();
        initializeHapticPatterns();
    }
    
    /**
     * Initialize audio cues for different events
     */
    private void initializeAudioCues() {
        // Hit feedback
        audioCues.put("perfect_hit", new AudioCue(800, 50, 0.3f));
        audioCues.put("good_hit", new AudioCue(600, 40, 0.2f));
        audioCues.put("ok_hit", new AudioCue(400, 30, 0.1f));
        audioCues.put("miss", new AudioCue(200, 100, 0.4f));
        
        // Timing cues
        audioCues.put("early", new AudioCue(300, 20, 0.1f));
        audioCues.put("late", new AudioCue(500, 20, 0.1f));
        
        // Combo milestones
        audioCues.put("combo_10", new AudioCue(1000, 100, 0.5f));
        audioCues.put("combo_50", new AudioCue(1200, 150, 0.6f));
        audioCues.put("combo_100", new AudioCue(1500, 200, 0.7f));
        
        // Special effects
        audioCues.put("spinner_start", new AudioCue(600, 80, 0.3f));
        audioCues.put("spinner_complete", new AudioCue(1000, 120, 0.5f));
    }
    
    /**
     * Initialize haptic patterns for different events
     */
    private void initializeHapticPatterns() {
        // Hit feedback
        hapticPatterns.put("perfect_hit", new HapticPattern(new long[]{0, 10}, 128));
        hapticPatterns.put("good_hit", new HapticPattern(new long[]{0, 5}, 64));
        hapticPatterns.put("ok_hit", new HapticPattern(new long[]{0, 3}, 32));
        hapticPatterns.put("miss", new HapticPattern(new long[]{0, 100}, 255));
        
        // Combo feedback
        hapticPatterns.put("combo_milestone", new HapticPattern(new long[]{0, 20, 10, 20}, 200));
        
        // Spinner feedback
        hapticPatterns.put("spinner_pulse", new HapticPattern(new long[]{0, 5}, 64));
        hapticPatterns.put("spinner_complete", new HapticPattern(new long[]{0, 50, 20, 50}, 255));
        
        // Warning cues
        hapticPatterns.put("timing_warning", new HapticPattern(new long[]{0, 30}, 128));
    }
    
    /**
     * Process hit timing and provide feedback
     */
    public void processHit(float hitTime, float targetTime, int combo) {
        float timingError = Math.abs(hitTime - targetTime);
        
        // Anti-mash protection
        if (enableAntiMash) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastHitTime < minHitInterval) {
                return; // Ignore hits that are too close together
            }
            lastHitTime = currentTime;
        }
        
        // Determine hit quality
        String hitQuality = getHitQuality(timingError);
        
        // Update performance tracking
        updatePerformanceTracking(timingError, hitQuality);
        
        // Provide haptic feedback
        if (enableHapticFeedback) {
            provideHapticFeedback(hitQuality, combo);
        }
        
        // Provide audio feedback
        if (enableAudioCues) {
            provideAudioFeedback(hitQuality, combo);
        }
        
        // Provide visual hints
        if (enableVisualHints) {
            provideVisualHints(hitQuality, timingError);
        }
    }
    
    /**
     * Get hit quality based on timing error
     */
    private String getHitQuality(float timingError) {
        if (timingError <= timingWindows.perfectWindow) {
            return "perfect";
        } else if (timingError <= timingWindows.goodWindow) {
            return "good";
        } else if (timingError <= timingWindows.okWindow) {
            return "ok";
        } else {
            return "miss";
        }
    }
    
    /**
     * Update performance tracking
     */
    private void updatePerformanceTracking(float timingError, String hitQuality) {
        // Add to recent timings
        recentTimings.add(timingError);
        if (recentTimings.size() > 20) {
            recentTimings.remove(0);
        }
        
        // Calculate average timing error
        float totalError = 0f;
        for (Float error : recentTimings) {
            totalError += error;
        }
        averageTimingError = totalError / recentTimings.size();
        
        // Update consecutive hits
        if (!hitQuality.equals("miss")) {
            consecutiveHits++;
            maxConsecutiveHits = Math.max(maxConsecutiveHits, consecutiveHits);
        } else {
            consecutiveHits = 0;
        }
    }
    
    /**
     * Provide haptic feedback
     */
    private void provideHapticFeedback(String hitQuality, int combo) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        
        // Base hit feedback
        HapticPattern pattern = hapticPatterns.get(hitQuality + "_hit");
        if (pattern != null) {
            vibrator.vibrate(pattern.pattern, -1);
        }
        
        // Combo milestone feedback
        if (combo > 0 && combo % 50 == 0) {
            HapticPattern comboPattern = hapticPatterns.get("combo_milestone");
            if (comboPattern != null) {
                vibrator.vibrate(comboPattern.pattern, -1);
            }
        }
    }
    
    /**
     * Provide audio feedback
     */
    private void provideAudioFeedback(String hitQuality, int combo) {
        if (toneGenerator == null) return;
        
        // Base hit feedback
        AudioCue cue = audioCues.get(hitQuality + "_hit");
        if (cue != null) {
            toneGenerator.startTone(cue.frequency, cue.duration);
        }
        
        // Combo milestone feedback
        if (combo > 0) {
            if (combo == 10) {
                cue = audioCues.get("combo_10");
            } else if (combo == 50) {
                cue = audioCues.get("combo_50");
            } else if (combo == 100) {
                cue = audioCues.get("combo_100");
            }
            
            if (cue != null) {
                toneGenerator.startTone(cue.frequency, cue.duration);
            }
        }
    }
    
    /**
     * Provide visual hints
     */
    private void provideVisualHints(String hitQuality, float timingError) {
        // This would integrate with the visual effects system
        // For now, we'll just log the information
        System.out.printf("Visual hint: %s hit, timing error: %.1fms%n", hitQuality, timingError);
    }
    
    /**
     * Process spinner event
     */
    public void processSpinnerEvent(String eventType, float progress) {
        // Spinner haptic feedback
        if (enableHapticFeedback && vibrator != null && vibrator.hasVibrator()) {
            if ("start".equals(eventType)) {
                HapticPattern pattern = hapticPatterns.get("spinner_start");
                if (pattern != null) {
                    vibrator.vibrate(pattern.pattern, -1);
                }
            } else if ("pulse".equals(eventType) && progress > 0.5f) {
                HapticPattern pattern = hapticPatterns.get("spinner_pulse");
                if (pattern != null) {
                    vibrator.vibrate(pattern.pattern, -1);
                }
            } else if ("complete".equals(eventType)) {
                HapticPattern pattern = hapticPatterns.get("spinner_complete");
                if (pattern != null) {
                    vibrator.vibrate(pattern.pattern, -1);
                }
            }
        }
        
        // Spinner audio feedback
        if (enableAudioCues && toneGenerator != null) {
            AudioCue cue = null;
            if ("start".equals(eventType)) {
                cue = audioCues.get("spinner_start");
            } else if ("complete".equals(eventType)) {
                cue = audioCues.get("spinner_complete");
            }
            
            if (cue != null) {
                toneGenerator.startTone(cue.frequency, cue.duration);
            }
        }
    }
    
    /**
     * Get timing assistance for next hit
     */
    public PointF getTimingAssistance(PointF targetPosition, float targetTime, float currentTime) {
        if (!enableTimingAssistance) {
            return null;
        }
        
        float timeToHit = targetTime - currentTime;
        
        // Only provide assistance for hits within 200ms
        if (timeToHit > 200f || timeToHit < 0f) {
            return null;
        }
        
        // Calculate visual indicator
        PointF indicator = new PointF(targetPosition.x, targetPosition.y);
        
        // Adjust position based on timing
        if (timeToHit < timingWindows.perfectWindow) {
            // Perfect timing - green indicator
            indicator.y -= 20f;
        } else if (timeToHit < timingWindows.goodWindow) {
            // Good timing - yellow indicator
            indicator.y -= 15f;
        } else if (timeToHit < timingWindows.okWindow) {
            // OK timing - orange indicator
            indicator.y -= 10f;
        } else {
            // Late timing - red indicator
            indicator.y -= 5f;
        }
        
        return indicator;
    }
    
    /**
     * Check if player needs timing assistance
     */
    public boolean needsTimingAssistance() {
        // Check if average timing error is high
        if (averageTimingError > timingWindows.goodWindow) {
            return true;
        }
        
        // Check if consecutive hits are low
        if (consecutiveHits < 5 && recentTimings.size() >= 10) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get performance statistics
     */
    public String getPerformanceStats() {
        return String.format(
            "Avg Error: %.1fms, Consecutive: %d, Max: %d, Recent: %d hits",
            averageTimingError, consecutiveHits, maxConsecutiveHits, recentTimings.size()
        );
    }
    
    /**
     * Get recommended settings based on performance
     */
    public Map<String, Boolean> getRecommendedSettings() {
        Map<String, Boolean> recommendations = new HashMap<>();
        
        // If timing is poor, enable more assistance
        if (averageTimingError > timingWindows.goodWindow) {
            recommendations.put("enableTimingAssistance", true);
            recommendations.put("enableVisualHints", true);
        }
        
        // If mash protection is needed
        if (consecutiveHits < 3 && recentTimings.size() >= 10) {
            recommendations.put("enableAntiMash", true);
        }
        
        // If player is doing well, reduce assistance
        if (averageTimingError < timingWindows.perfectWindow && consecutiveHits > 20) {
            recommendations.put("enableHapticFeedback", false);
            recommendations.put("enableAudioCues", false);
        }
        
        return recommendations;
    }
    
    /**
     * Apply recommended settings
     */
    public void applyRecommendedSettings() {
        Map<String, Boolean> recommendations = getRecommendedSettings();
        
        for (Map.Entry<String, Boolean> entry : recommendations.entrySet()) {
            switch (entry.getKey()) {
                case "enableHapticFeedback":
                    enableHapticFeedback = entry.getValue();
                    break;
                case "enableAudioCues":
                    enableAudioCues = entry.getValue();
                    break;
                case "enableTimingAssistance":
                    enableTimingAssistance = entry.getValue();
                    break;
                case "enableVisualHints":
                    enableVisualHints = entry.getValue();
                    break;
                case "enableAntiMash":
                    enableAntiMash = entry.getValue();
                    break;
            }
        }
    }
    
    /**
     * Reset performance tracking
     */
    public void resetPerformanceTracking() {
        recentTimings.clear();
        consecutiveHits = 0;
        maxConsecutiveHits = 0;
        averageTimingError = 0f;
        lastHitTime = 0;
    }
    
    /**
     * Set timing windows
     */
    public void setTimingWindows(float perfect, float good, float ok) {
        this.timingWindows = new TimingWindow(perfect, good, ok);
    }
    
    /**
     * Set minimum hit interval for anti-mash protection
     */
    public void setMinHitInterval(float interval) {
        this.minHitInterval = interval;
    }
    
    /**
     * Enable/disable features
     */
    public void setHapticFeedbackEnabled(boolean enabled) {
        this.enableHapticFeedback = enabled;
    }
    
    public void setAudioCuesEnabled(boolean enabled) {
        this.enableAudioCues = enabled;
    }
    
    public void setTimingAssistanceEnabled(boolean enabled) {
        this.enableTimingAssistance = enabled;
    }
    
    public void setVisualHintsEnabled(boolean enabled) {
        this.enableVisualHints = enabled;
    }
    
    public void setAntiMashEnabled(boolean enabled) {
        this.enableAntiMash = enabled;
    }
    
    /**
     * Get current settings
     */
    public boolean isHapticFeedbackEnabled() { return enableHapticFeedback; }
    public boolean isAudioCuesEnabled() { return enableAudioCues; }
    public boolean isTimingAssistanceEnabled() { return enableTimingAssistance; }
    public boolean isVisualHintsEnabled() { return enableVisualHints; }
    public boolean isAntiMashEnabled() { return enableAntiMash; }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (toneGenerator != null) {
            toneGenerator.release();
        }
    }
}

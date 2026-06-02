package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * AdaptiveDifficulty - Dynamic difficulty adjustment system
 * Analyzes player performance and adjusts game parameters accordingly
 */
public class AdaptiveDifficulty {
    
    // Performance metrics
    private static class PerformanceMetrics {
        public float accuracy;
        public float combo;
        public float timingError;
        public float reactionTime;
        public int totalHits;
        public int perfectHits;
        public int goodHits;
        public int okHits;
        public int misses;
        
        public PerformanceMetrics() {
            reset();
        }
        
        public void reset() {
            accuracy = 0f;
            combo = 0f;
            timingError = 0f;
            reactionTime = 0f;
            totalHits = 0;
            perfectHits = 0;
            goodHits = 0;
            okHits = 0;
            misses = 0;
        }
        
        public void updateFromHit(float timingError, float reactionTime, String hitQuality) {
            totalHits++;
            this.timingError = (this.timingError * (totalHits - 1) + timingError) / totalHits;
            this.reactionTime = (this.reactionTime * (totalHits - 1) + reactionTime) / totalHits;
            
            switch (hitQuality) {
                case "perfect":
                    perfectHits++;
                    break;
                case "good":
                    goodHits++;
                    break;
                case "ok":
                    okHits++;
                    break;
                case "miss":
                    misses++;
                    break;
            }
            
            calculateAccuracy();
        }
        
        private void calculateAccuracy() {
            int weightedHits = perfectHits * 300 + goodHits * 100 + okHits * 50;
            int maxWeighted = totalHits * 300;
            accuracy = maxWeighted > 0 ? (float) weightedHits / maxWeighted : 0f;
        }
    }
    
    // Difficulty parameters
    private static class DifficultyParameters {
        public float approachRate;
        public float circleSize;
        public float overallDifficulty;
        public float hpDrainRate;
        public float speedMultiplier;
        public float accuracyRequirement;
        
        public DifficultyParameters(float ar, float cs, float od, float hp, float speed, float acc) {
            this.approachRate = ar;
            this.circleSize = cs;
            this.overallDifficulty = od;
            this.hpDrainRate = hp;
            this.speedMultiplier = speed;
            this.accuracyRequirement = acc;
        }
        
        public DifficultyParameters clone() {
            return new DifficultyParameters(approachRate, circleSize, overallDifficulty, 
                                          hpDrainRate, speedMultiplier, accuracyRequirement);
        }
    }
    
    // Skill assessment
    private enum SkillLevel {
        BEGINNER, NOVICE, INTERMEDIATE, ADVANCED, EXPERT, MASTER
    }
    
    private PerformanceMetrics currentMetrics;
    private PerformanceMetrics recentMetrics;
    private DifficultyParameters baseParameters;
    private DifficultyParameters currentParameters;
    
    // Adaptive settings
    private boolean enableAdaptation = true;
    private float adaptationRate = 0.1f;
    private int analysisWindow = 50; // Number of hits to analyze
    private float performanceThreshold = 0.8f;
    
    // Performance history
    private ArrayList<Float> accuracyHistory = new ArrayList<>();
    private ArrayList<Float> comboHistory = new ArrayList<>();
    private ArrayList<Float> timingHistory = new ArrayList<>();
    
    // Skill level tracking
    private SkillLevel currentSkillLevel = SkillLevel.BEGINNER;
    private Map<SkillLevel, DifficultyParameters> skillLevelPresets = new HashMap<>();
    
    public AdaptiveDifficulty(float baseAR, float baseCS, float baseOD, float baseHP) {
        this.currentMetrics = new PerformanceMetrics();
        this.recentMetrics = new PerformanceMetrics();
        this.baseParameters = new DifficultyParameters(baseAR, baseCS, baseOD, baseHP, 1f, 0.8f);
        this.currentParameters = baseParameters.clone();
        
        initializeSkillLevelPresets();
    }
    
    /**
     * Initialize difficulty presets for different skill levels
     */
    private void initializeSkillLevelPresets() {
        // Beginner - Very forgiving
        skillLevelPresets.put(SkillLevel.BEGINNER, 
            new DifficultyParameters(8f, 4f, 2f, 2f, 0.8f, 0.6f));
        
        // Novice - Easy
        skillLevelPresets.put(SkillLevel.NOVICE, 
            new DifficultyParameters(9f, 3.5f, 4f, 3f, 0.9f, 0.7f));
        
        // Intermediate - Normal
        skillLevelPresets.put(SkillLevel.INTERMEDIATE, 
            new DifficultyParameters(9.5f, 3f, 6f, 4f, 1f, 0.8f));
        
        // Advanced - Hard
        skillLevelPresets.put(SkillLevel.ADVANCED, 
            new DifficultyParameters(10f, 2.5f, 8f, 5f, 1.1f, 0.85f));
        
        // Expert - Very Hard
        skillLevelPresets.put(SkillLevel.EXPERT, 
            new DifficultyParameters(10.5f, 2f, 9.5f, 6f, 1.2f, 0.9f));
        
        // Master - Insane
        skillLevelPresets.put(SkillLevel.MASTER, 
            new DifficultyParameters(11f, 1.5f, 10f, 7f, 1.3f, 0.95f));
    }
    
    /**
     * Process a hit and update difficulty
     */
    public void processHit(float timingError, float reactionTime, String hitQuality, int combo) {
        // Update metrics
        currentMetrics.updateFromHit(timingError, reactionTime, hitQuality);
        currentMetrics.combo = combo;
        
        // Update recent metrics
        recentMetrics.updateFromHit(timingError, reactionTime, hitQuality);
        recentMetrics.combo = combo;
        
        // Add to history
        accuracyHistory.add(currentMetrics.accuracy);
        comboHistory.add((float) combo);
        timingHistory.add(currentMetrics.timingError);
        
        // Limit history size
        limitHistorySize();
        
        // Update difficulty if adaptation is enabled
        if (enableAdaptation) {
            updateDifficulty();
        }
        
        // Update skill level
        updateSkillLevel();
    }
    
    /**
     * Update difficulty based on performance
     */
    private void updateDifficulty() {
        if (recentMetrics.totalHits < analysisWindow) {
            return; // Not enough data yet
        }
        
        float performanceScore = calculatePerformanceScore();
        
        // Adjust parameters based on performance
        if (performanceScore > performanceThreshold) {
            // Player is doing well, increase difficulty
            increaseDifficulty();
        } else if (performanceScore < performanceThreshold * 0.6f) {
            // Player is struggling, decrease difficulty
            decreaseDifficulty();
        }
        
        // Apply gradual changes
        applyGradualChanges();
    }
    
    /**
     * Calculate overall performance score
     */
    private float calculatePerformanceScore() {
        float accuracyScore = recentMetrics.accuracy;
        float comboScore = Math.min(recentMetrics.combo / 100f, 1f); // Normalize to 0-1
        float timingScore = Math.max(0, 1f - (recentMetrics.timingError / 100f)); // Lower timing error = higher score
        
        // Weighted average
        return (accuracyScore * 0.5f + comboScore * 0.3f + timingScore * 0.2f);
    }
    
    /**
     * Increase difficulty
     */
    private void increaseDifficulty() {
        DifficultyParameters target = skillLevelPresets.get(getNextSkillLevel());
        
        // Gradually move towards target parameters
        currentParameters.approachRate = lerp(currentParameters.approachRate, target.approachRate, adaptationRate);
        currentParameters.circleSize = lerp(currentParameters.circleSize, target.circleSize, adaptationRate);
        currentParameters.overallDifficulty = lerp(currentParameters.overallDifficulty, target.overallDifficulty, adaptationRate);
        currentParameters.hpDrainRate = lerp(currentParameters.hpDrainRate, target.hpDrainRate, adaptationRate);
        currentParameters.speedMultiplier = lerp(currentParameters.speedMultiplier, target.speedMultiplier, adaptationRate);
        currentParameters.accuracyRequirement = lerp(currentParameters.accuracyRequirement, target.accuracyRequirement, adaptationRate);
    }
    
    /**
     * Decrease difficulty
     */
    private void decreaseDifficulty() {
        DifficultyParameters target = skillLevelPresets.get(getPreviousSkillLevel());
        
        // Gradually move towards target parameters
        currentParameters.approachRate = lerp(currentParameters.approachRate, target.approachRate, adaptationRate);
        currentParameters.circleSize = lerp(currentParameters.circleSize, target.circleSize, adaptationRate);
        currentParameters.overallDifficulty = lerp(currentParameters.overallDifficulty, target.overallDifficulty, adaptationRate);
        currentParameters.hpDrainRate = lerp(currentParameters.hpDrainRate, target.hpDrainRate, adaptationRate);
        currentParameters.speedMultiplier = lerp(currentParameters.speedMultiplier, target.speedMultiplier, adaptationRate);
        currentParameters.accuracyRequirement = lerp(currentParameters.accuracyRequirement, target.accuracyRequirement, adaptationRate);
    }
    
    /**
     * Apply gradual changes to smooth difficulty transitions
     */
    private void applyGradualChanges() {
        // Ensure parameters stay within reasonable bounds
        currentParameters.approachRate = clamp(currentParameters.approachRate, 5f, 12f);
        currentParameters.circleSize = clamp(currentParameters.circleSize, 1f, 5f);
        currentParameters.overallDifficulty = clamp(currentParameters.overallDifficulty, 0f, 10f);
        currentParameters.hpDrainRate = clamp(currentParameters.hpDrainRate, 0f, 10f);
        currentParameters.speedMultiplier = clamp(currentParameters.speedMultiplier, 0.5f, 2f);
        currentParameters.accuracyRequirement = clamp(currentParameters.accuracyRequirement, 0f, 1f);
    }
    
    /**
     * Update skill level based on performance
     */
    private void updateSkillLevel() {
        SkillLevel newLevel = determineSkillLevel();
        
        if (newLevel != currentSkillLevel) {
            currentSkillLevel = newLevel;
            System.out.println("Skill level updated to: " + newLevel);
        }
    }
    
    /**
     * Determine current skill level
     */
    private SkillLevel determineSkillLevel() {
        float accuracy = currentMetrics.accuracy;
        float avgCombo = getAverageCombo();
        float avgTiming = currentMetrics.timingError;
        
        if (accuracy > 0.95f && avgCombo > 100f && avgTiming < 20f) {
            return SkillLevel.MASTER;
        } else if (accuracy > 0.9f && avgCombo > 50f && avgTiming < 30f) {
            return SkillLevel.EXPERT;
        } else if (accuracy > 0.85f && avgCombo > 25f && avgTiming < 40f) {
            return SkillLevel.ADVANCED;
        } else if (accuracy > 0.75f && avgCombo > 10f && avgTiming < 50f) {
            return SkillLevel.INTERMEDIATE;
        } else if (accuracy > 0.6f && avgCombo > 5f && avgTiming < 70f) {
            return SkillLevel.NOVICE;
        } else {
            return SkillLevel.BEGINNER;
        }
    }
    
    /**
     * Get next skill level
     */
    private SkillLevel getNextSkillLevel() {
        switch (currentSkillLevel) {
            case BEGINNER: return SkillLevel.NOVICE;
            case NOVICE: return SkillLevel.INTERMEDIATE;
            case INTERMEDIATE: return SkillLevel.ADVANCED;
            case ADVANCED: return SkillLevel.EXPERT;
            case EXPERT: return SkillLevel.MASTER;
            case MASTER: return SkillLevel.MASTER;
            default: return currentSkillLevel;
        }
    }
    
    /**
     * Get previous skill level
     */
    private SkillLevel getPreviousSkillLevel() {
        switch (currentSkillLevel) {
            case BEGINNER: return SkillLevel.BEGINNER;
            case NOVICE: return SkillLevel.BEGINNER;
            case INTERMEDIATE: return SkillLevel.NOVICE;
            case ADVANCED: return SkillLevel.INTERMEDIATE;
            case EXPERT: return SkillLevel.ADVANCED;
            case MASTER: return SkillLevel.EXPERT;
            default: return currentSkillLevel;
        }
    }
    
    /**
     * Get average combo
     */
    private float getAverageCombo() {
        if (comboHistory.isEmpty()) return 0f;
        
        float total = 0f;
        for (Float combo : comboHistory) {
            total += combo;
        }
        return total / comboHistory.size();
    }
    
    /**
     * Limit history size
     */
    private void limitHistorySize() {
        int maxSize = analysisWindow * 2; // Keep double the analysis window
        
        while (accuracyHistory.size() > maxSize) {
            accuracyHistory.remove(0);
        }
        while (comboHistory.size() > maxSize) {
            comboHistory.remove(0);
        }
        while (timingHistory.size() > maxSize) {
            timingHistory.remove(0);
        }
    }
    
    /**
     * Linear interpolation
     */
    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
    
    /**
     * Clamp value to range
     */
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Get current difficulty parameters
     */
    public DifficultyParameters getCurrentParameters() {
        return currentParameters.clone();
    }
    
    /**
     * Get performance statistics
     */
    public String getPerformanceStats() {
        return String.format(
            "Skill: %s, Accuracy: %.1f%%, Combo: %.0f, Timing: %.1fms, Hits: %d",
            currentSkillLevel, currentMetrics.accuracy * 100, currentMetrics.combo,
            currentMetrics.timingError, currentMetrics.totalHits
        );
    }
    
    /**
     * Get detailed performance report
     */
    public String getDetailedReport() {
        return String.format(
            "=== Adaptive Difficulty Report ===\n" +
            "Current Skill Level: %s\n" +
            "Performance Metrics:\n" +
            "  Accuracy: %.1f%% (%d perfect, %d good, %d ok, %d miss)\n" +
            "  Average Combo: %.1f\n" +
            "  Average Timing Error: %.1fms\n" +
            "  Total Hits: %d\n" +
            "Current Parameters:\n" +
            "  Approach Rate: %.1f\n" +
            "  Circle Size: %.1f\n" +
            "  Overall Difficulty: %.1f\n" +
            "  HP Drain Rate: %.1f\n" +
            "  Speed Multiplier: %.2f\n" +
            "  Accuracy Requirement: %.1f%%\n" +
            "==============================",
            currentSkillLevel,
            currentMetrics.accuracy * 100,
            currentMetrics.perfectHits, currentMetrics.goodHits, currentMetrics.okHits, currentMetrics.misses,
            getAverageCombo(),
            currentMetrics.timingError,
            currentMetrics.totalHits,
            currentParameters.approachRate,
            currentParameters.circleSize,
            currentParameters.overallDifficulty,
            currentParameters.hpDrainRate,
            currentParameters.speedMultiplier,
            currentParameters.accuracyRequirement * 100
        );
    }
    
    /**
     * Reset all metrics
     */
    public void reset() {
        currentMetrics.reset();
        recentMetrics.reset();
        accuracyHistory.clear();
        comboHistory.clear();
        timingHistory.clear();
        currentParameters = baseParameters.clone();
        currentSkillLevel = SkillLevel.BEGINNER;
    }
    
    /**
     * Set adaptation settings
     */
    public void setAdaptationEnabled(boolean enabled) {
        this.enableAdaptation = enabled;
    }
    
    public void setAdaptationRate(float rate) {
        this.adaptationRate = clamp(rate, 0.01f, 1f);
    }
    
    public void setAnalysisWindow(int window) {
        this.analysisWindow = Math.max(10, window);
    }
    
    public void setPerformanceThreshold(float threshold) {
        this.performanceThreshold = clamp(threshold, 0f, 1f);
    }
    
    /**
     * Get adaptation settings
     */
    public boolean isAdaptationEnabled() { return enableAdaptation; }
    public float getAdaptationRate() { return adaptationRate; }
    public int getAnalysisWindow() { return analysisWindow; }
    public float getPerformanceThreshold() { return performanceThreshold; }
    
    /**
     * Get current skill level
     */
    public SkillLevel getCurrentSkillLevel() {
        return currentSkillLevel;
    }
}

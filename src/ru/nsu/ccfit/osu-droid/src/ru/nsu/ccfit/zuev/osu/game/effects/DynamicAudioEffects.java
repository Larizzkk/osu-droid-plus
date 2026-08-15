package ru.nsu.ccfit.zuev.osuplusplus.game.effects;

import android.graphics.PointF;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic audio effects utilities
 * Based on osu!lazer's advanced audio processing
 */
public class DynamicAudioEffects {
    
    // Dynamic effect types
    public enum EffectType {
        REVERSE, PITCH_SHIFT, TIME_STRETCH, FILTER, DISTORTION, CHORUS, FLANGER, PHASER
    }
    
    // Audio processor
    public static class AudioProcessor {
        public EffectType type;
        public float intensity;
        public float parameter1;
        public float parameter2;
        public boolean enabled;
        public long startTime;
        
        public AudioProcessor(EffectType type) {
            this.type = type;
            this.intensity = 0.5f;
            this.parameter1 = 0.5f;
            this.parameter2 = 0.5f;
            this.enabled = true;
            this.startTime = System.currentTimeMillis();
        }
    }
    
    // Beat synchronization
    public static class BeatSync {
        public float bpm;
        public long beatInterval;
        public long lastBeatTime;
        public float syncStrength;
        public boolean enabled;
        
        public BeatSync() {
            this.bpm = 120.0f;
            this.beatInterval = 500; // 120 BPM
            this.lastBeatTime = 0;
            this.syncStrength = 1.0f;
            this.enabled = true;
        }
        
        public void updateBPM(float newBPM) {
            this.bpm = newBPM;
            this.beatInterval = (long) (60000.0f / newBPM);
        }
        
        public boolean isBeat() {
            long currentTime = System.currentTimeMillis();
            return (currentTime - lastBeatTime) >= beatInterval;
        }
        
        public void markBeat() {
            lastBeatTime = System.currentTimeMillis();
        }
    }
    
    // Audio visualizer data
    public static class AudioVisualizer {
        public float[] frequencyData;
        public float[] waveform;
        public float bassLevel;
        public float midLevel;
        public float trebleLevel;
        public float peakLevel;
        public boolean active;
        
        public AudioVisualizer() {
            this.frequencyData = new float[64];
            this.waveform = new float[256];
            this.bassLevel = 0.0f;
            this.midLevel = 0.0f;
            this.trebleLevel = 0.0f;
            this.peakLevel = 0.0f;
            this.active = false;
        }
    }
    
    // Rhythm analyzer
    public static class RhythmAnalyzer {
        public float[] beatPattern;
        public float rhythmComplexity;
        public float syncopationLevel;
        public float grooveLevel;
        public boolean isComplex;
        
        public RhythmAnalyzer() {
            this.beatPattern = new float[16];
            this.rhythmComplexity = 0.0f;
            this.syncopationLevel = 0.0f;
            this.grooveLevel = 0.0f;
            this.isComplex = false;
        }
        
        public void analyzePattern(long[] hitTimes) {
            if (hitTimes.length < 2) return;
            
            // Calculate intervals
            float[] intervals = new float[hitTimes.length - 1];
            for (int i = 1; i < hitTimes.length; i++) {
                intervals[i - 1] = hitTimes[i] - hitTimes[i - 1];
            }
            
            // Analyze rhythm complexity
            analyzeComplexity(intervals);
        }
        
        private void analyzeComplexity(float[] intervals) {
            if (intervals.length == 0) return;
            
            float meanInterval = 0;
            for (float interval : intervals) {
                meanInterval += interval;
            }
            meanInterval /= intervals.length;
            
            float variance = 0;
            for (float interval : intervals) {
                float diff = interval - meanInterval;
                variance += diff * diff;
            }
            variance /= intervals.length;
            
            rhythmComplexity = (float) Math.sqrt(variance) / meanInterval;
            isComplex = rhythmComplexity > 0.3f;
        }
    }
    
    private static final Random random = new Random();
    private static List<AudioProcessor> processors = new CopyOnWriteArrayList<>();
    private static BeatSync beatSync = new BeatSync();
    private static AudioVisualizer visualizer = new AudioVisualizer();
    private static RhythmAnalyzer rhythmAnalyzer = new RhythmAnalyzer();
    
    // Dynamic audio parameters
    private static boolean dynamicEffectsEnabled = true;
    private static float globalIntensity = 1.0f;
    private static float bassBoost = 0.0f;
    private static float midBoost = 0.0f;
    private static float trebleBoost = 0.0f;
    
    /**
     * Add audio processor
     */
    public static void addProcessor(EffectType type) {
        AudioProcessor processor = new AudioProcessor(type);
        processors.add(processor);
    }
    
    /**
     * Remove audio processor
     */
    public static void removeProcessor(EffectType type) {
        processors.removeIf(processor -> processor.type == type);
    }
    
    /**
     * Apply reverse effect
     */
    public static float[] applyReverse(float[] audioData) {
        float[] reversed = new float[audioData.length];
        for (int i = 0; i < audioData.length; i++) {
            reversed[i] = audioData[audioData.length - 1 - i];
        }
        return reversed;
    }
    
    /**
     * Apply pitch shift effect
     */
    public static float[] applyPitchShift(float[] audioData, float shift) {
        if (shift == 1.0f) return audioData.clone();
        
        float[] shifted = new float[audioData.length];
        float step = 1.0f / shift;
        
        for (int i = 0; i < audioData.length; i++) {
            float sourceIndex = i * step;
            if (sourceIndex < audioData.length - 1) {
                int index = (int) sourceIndex;
                float fraction = sourceIndex - index;
                
                // Linear interpolation
                shifted[i] = audioData[index] * (1 - fraction) + audioData[index + 1] * fraction;
            }
        }
        
        return shifted;
    }
    
    /**
     * Apply time stretch effect
     */
    public static float[] applyTimeStretch(float[] audioData, float stretch) {
        if (stretch == 1.0f) return audioData.clone();
        
        int newLength = (int) (audioData.length * stretch);
        float[] stretched = new float[newLength];
        
        for (int i = 0; i < newLength; i++) {
            float sourceIndex = i / stretch;
            if (sourceIndex < audioData.length - 1) {
                int index = (int) sourceIndex;
                float fraction = sourceIndex - index;
                
                // Linear interpolation
                stretched[i] = audioData[index] * (1 - fraction) + audioData[index + 1] * fraction;
            }
        }
        
        return stretched;
    }
    
    /**
     * Apply filter effect
     */
    public static float[] applyFilter(float[] audioData, float cutoff, float resonance) {
        float[] filtered = audioData.clone();
        
        // Simple low-pass filter
        float dt = 1.0f / 44100.0f;
        float RC = 1.0f / (2.0f * (float) Math.PI * cutoff);
        float alpha = dt / (RC + dt);
        
        float prev = 0;
        for (int i = 0; i < filtered.length; i++) {
            float current = filtered[i];
            filtered[i] = prev + alpha * (current - prev);
            prev = filtered[i];
        }
        
        return filtered;
    }
    
    /**
     * Apply distortion effect
     */
    public static float[] applyDistortion(float[] audioData, float intensity) {
        float[] distorted = new float[audioData.length];
        
        for (int i = 0; i < audioData.length; i++) {
            float sample = audioData[i] * intensity;
            
            // Soft clipping
            if (sample > 0.5f) {
                distorted[i] = 0.5f + (sample - 0.5f) * 0.5f;
            } else if (sample < -0.5f) {
                distorted[i] = -0.5f + (sample + 0.5f) * 0.5f;
            } else {
                distorted[i] = sample;
            }
            
            // Normalize
            distorted[i] = Math.max(-1.0f, Math.min(1.0f, distorted[i]));
        }
        
        return distorted;
    }
    
    /**
     * Apply chorus effect
     */
    public static float[] applyChorus(float[] audioData, float rate, float depth) {
        float[] chorus = audioData.clone();
        float delay = (float) (0.03 * depth); // 30ms max delay
        int delaySamples = (int) (delay * 44100);
        
        for (int i = delaySamples; i < audioData.length; i++) {
            float modulation = (float) Math.sin(2 * Math.PI * rate * i / 44100.0);
            float currentDelay = delaySamples * (1 + modulation * depth);
            int delayIndex = i - (int) currentDelay;
            
            if (delayIndex >= 0) {
                chorus[i] = 0.5f * audioData[i] + 0.5f * audioData[delayIndex];
            }
        }
        
        return chorus;
    }
    
    /**
     * Apply flanger effect
     */
    public static float[] applyFlanger(float[] audioData, float rate, float depth) {
        float[] flanged = audioData.clone();
        float delay = (float) (0.01 * depth); // 10ms max delay
        int delaySamples = (int) (delay * 44100);
        
        for (int i = delaySamples; i < audioData.length; i++) {
            float modulation = (float) Math.sin(2 * Math.PI * rate * i / 44100.0);
            float currentDelay = delaySamples * (1 + modulation * depth);
            int delayIndex = i - (int) currentDelay;
            
            if (delayIndex >= 0) {
                flanged[i] = audioData[i] + audioData[delayIndex] * 0.7f;
            }
        }
        
        return flanged;
    }
    
    /**
     * Apply phaser effect
     */
    public static float[] applyPhaser(float[] audioData, float rate, float depth) {
        float[] phased = audioData.clone();
        float[] allpassBuffer = new float[1024];
        int bufferIndex = 0;
        
        for (int i = 0; i < audioData.length; i++) {
            float modulation = (float) Math.sin(2 * Math.PI * rate * i / 44100.0);
            float delay = 2 + modulation * depth * 10;
            
            // All-pass filter
            float delayed = allpassBuffer[bufferIndex];
            allpassBuffer[bufferIndex] = audioData[i] + delayed * 0.5f;
            bufferIndex = (bufferIndex + 1) % allpassBuffer.length;
            
            phased[i] = audioData[i] + delayed * 0.5f;
        }
        
        return phased;
    }
    
    /**
     * Process audio with all active processors
     */
    public static float[] processAudio(float[] audioData) {
        if (!dynamicEffectsEnabled) return audioData;
        
        float[] processed = audioData.clone();
        
        for (AudioProcessor processor : processors) {
            if (!processor.enabled) continue;
            
            switch (processor.type) {
                case REVERSE:
                    processed = applyReverse(processed);
                    break;
                case PITCH_SHIFT:
                    processed = applyPitchShift(processed, 1.0f + processor.parameter1);
                    break;
                case TIME_STRETCH:
                    processed = applyTimeStretch(processed, 1.0f + processor.parameter1);
                    break;
                case FILTER:
                    processed = applyFilter(processed, processor.parameter1 * 20000, processor.parameter2 * 10);
                    break;
                case DISTORTION:
                    processed = applyDistortion(processed, processor.intensity);
                    break;
                case CHORUS:
                    processed = applyChorus(processed, processor.parameter1, processor.parameter2);
                    break;
                case FLANGER:
                    processed = applyFlanger(processed, processor.parameter1, processor.parameter2);
                    break;
                case PHASER:
                    processed = applyPhaser(processed, processor.parameter1, processor.parameter2);
                    break;
            }
        }
        
        return processed;
    }
    
    /**
     * Update beat synchronization
     */
    public static void updateBeatSync(float currentBPM) {
        beatSync.updateBPM(currentBPM);
        
        if (beatSync.isBeat()) {
            beatSync.markBeat();
            onBeatDetected();
        }
    }
    
    /**
     * Called when beat is detected
     */
    private static void onBeatDetected() {
        // Trigger beat-synced effects
        if (random.nextFloat() < 0.3f) {
            addProcessor(EffectType.FLANGER);
        }
        
        // Adjust intensity based on beat
        globalIntensity = 0.8f + random.nextFloat() * 0.4f;
    }
    
    /**
     * Update audio visualization
     */
    public static void updateVisualization(float[] audioData) {
        if (!visualizer.active) return;
        
        // Generate fake frequency data (in real implementation, use FFT)
        for (int i = 0; i < visualizer.frequencyData.length; i++) {
            visualizer.frequencyData[i] = (float) Math.random() * globalIntensity;
        }
        
        // Generate fake waveform
        for (int i = 0; i < visualizer.waveform.length; i++) {
            int dataIndex = (int) (i * audioData.length / (float) visualizer.waveform.length);
            if (dataIndex < audioData.length) {
                visualizer.waveform[i] = audioData[dataIndex];
            }
        }
        
        // Calculate frequency band levels
        calculateFrequencyLevels();
    }
    
    /**
     * Calculate frequency band levels
     */
    private static void calculateFrequencyLevels() {
        float[] freq = visualizer.frequencyData;
        
        // Bass (0-8)
        float bass = 0;
        for (int i = 0; i < 8; i++) {
            bass += freq[i];
        }
        visualizer.bassLevel = bass / 8;
        
        // Mid (8-32)
        float mid = 0;
        for (int i = 8; i < 32; i++) {
            mid += freq[i];
        }
        visualizer.midLevel = mid / 24;
        
        // Treble (32-64)
        float treble = 0;
        for (int i = 32; i < 64; i++) {
            treble += freq[i];
        }
        visualizer.trebleLevel = treble / 32;
        
        // Peak
        visualizer.peakLevel = Math.max(visualizer.bassLevel, Math.max(visualizer.midLevel, visualizer.trebleLevel));
    }
    
    /**
     * Apply frequency-based effects
     */
    public static void applyFrequencyEffects() {
        if (!visualizer.active) return;
        
        // Bass boost
        if (visualizer.bassLevel > 0.7f) {
            bassBoost = visualizer.bassLevel;
        } else {
            bassBoost *= 0.95f; // Decay
        }
        
        // Mid boost
        if (visualizer.midLevel > 0.6f) {
            midBoost = visualizer.midLevel;
        } else {
            midBoost *= 0.95f;
        }
        
        // Treble boost
        if (visualizer.trebleLevel > 0.5f) {
            trebleBoost = visualizer.trebleLevel;
        } else {
            trebleBoost *= 0.95f;
        }
    }
    
    /**
     * Create dynamic hit sound based on context
     */
    public static void createDynamicHitSound(float x, float y, int combo, float accuracy) {
        // Base sound
        Audio3DEffects.play3DHitSound(x, y, false, false, false);
        
        // Add effects based on context
        if (combo > 50) {
            addProcessor(EffectType.CHORUS);
        }
        
        if (accuracy > 0.95f) {
            addProcessor(EffectType.FLANGER);
        }
        
        if (visualizer.peakLevel > 0.8f) {
            addProcessor(EffectType.DISTORTION);
        }
        
        // Remove processors after a delay
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                removeProcessor(EffectType.CHORUS);
                removeProcessor(EffectType.FLANGER);
                removeProcessor(EffectType.DISTORTION);
            } catch (InterruptedException e) {
                // Ignore
            }
        }).start();
    }
    
    /**
     * Create dynamic slider sound
     */
    public static void createDynamicSliderSound(PointF[] path, float speed) {
        // Adjust effects based on slider speed
        if (speed > 2.0f) {
            addProcessor(EffectType.PITCH_SHIFT);
            // Set higher pitch for fast sliders
            for (AudioProcessor processor : processors) {
                if (processor.type == EffectType.PITCH_SHIFT) {
                    processor.parameter1 = 0.2f; // Higher pitch
                    break;
                }
            }
        } else if (speed < 0.5f) {
            addProcessor(EffectType.PITCH_SHIFT);
            // Set lower pitch for slow sliders
            for (AudioProcessor processor : processors) {
                if (processor.type == EffectType.PITCH_SHIFT) {
                    processor.parameter1 = -0.2f; // Lower pitch
                    break;
                }
            }
        }
        
        Audio3DEffects.play3DSliderSounds(path, false, false);
    }
    
    /**
     * Create dynamic spinner sound
     */
    public static void createDynamicSpinnerSound(float rpm, boolean bonus) {
        // Adjust pitch based on RPM
        if (rpm > 300) {
            addProcessor(EffectType.PITCH_SHIFT);
            for (AudioProcessor processor : processors) {
                if (processor.type == EffectType.PITCH_SHIFT) {
                    processor.parameter1 = 0.3f; // Higher pitch for fast spin
                    break;
                }
            }
        }
        
        Audio3DEffects.play3DSpinnerSounds(0, 0, bonus);
    }
    
    /**
     * Enable/disable dynamic effects
     */
    public static void setDynamicEffectsEnabled(boolean enabled) {
        dynamicEffectsEnabled = enabled;
        
        if (!enabled) {
            processors.clear();
        }
    }
    
    /**
     * Set global intensity
     */
    public static void setGlobalIntensity(float intensity) {
        globalIntensity = Math.max(0, Math.min(2, intensity));
    }
    
    /**
     * Get current beat sync
     */
    public static BeatSync getBeatSync() {
        return beatSync;
    }
    
    /**
     * Get current visualizer
     */
    public static AudioVisualizer getVisualizer() {
        return visualizer;
    }
    
    /**
     * Get rhythm analyzer
     */
    public static RhythmAnalyzer getRhythmAnalyzer() {
        return rhythmAnalyzer;
    }
    
    /**
     * Clear all processors
     */
    public static void clear() {
        processors.clear();
        beatSync.lastBeatTime = 0;
        visualizer.active = false;
    }
    
    /**
     * Get dynamic audio info
     */
    public static String getDynamicAudioInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== Dynamic Audio Effects ===\n");
        info.append("Enabled: ").append(dynamicEffectsEnabled).append("\n");
        info.append("Global Intensity: ").append(String.format("%.2f", globalIntensity)).append("\n");
        info.append("Active Processors: ").append(processors.size()).append("\n");
        info.append("Beat Sync BPM: ").append(String.format("%.1f", beatSync.bpm)).append("\n");
        info.append("Visualizer Active: ").append(visualizer.active).append("\n");
        info.append("Bass Level: ").append(String.format("%.2f", visualizer.bassLevel)).append("\n");
        info.append("Mid Level: ").append(String.format("%.2f", visualizer.midLevel)).append("\n");
        info.append("Treble Level: ").append(String.format("%.2f", visualizer.trebleLevel)).append("\n");
        info.append("Rhythm Complexity: ").append(String.format("%.2f", rhythmAnalyzer.rhythmComplexity)).append("\n");
        
        return info.toString();
    }
}

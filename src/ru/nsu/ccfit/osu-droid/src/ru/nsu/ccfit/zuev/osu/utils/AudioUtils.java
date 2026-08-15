package ru.nsu.ccfit.zuev.osuplusplus.utils;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.content.Context;
import android.media.MediaPlayer;
import java.util.HashMap;
import java.util.Map;

/**
 * Audio utilities for enhanced sound management
 * Based on osu!lazer's audio systems
 */
public class AudioUtils {
    
    // Audio constants
    public static final int SAMPLE_RATE = 44100;
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_STEREO;
    public static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public static final int BUFFER_SIZE_FACTOR = 4;
    
    // Audio effects
    public static class AudioEffect {
        public static final int NONE = 0;
        public static final int LOW_PASS = 1;
        public static final int HIGH_PASS = 2;
        public static final int BAND_PASS = 3;
        public static final int REVERB = 4;
        public static final int ECHO = 5;
        public static final int DISTORTION = 6;
    }
    
    // Audio filter parameters
    public static class FilterParams {
        public float cutoff = 1000.0f;
        public float resonance = 1.0f;
        public float gain = 1.0f;
        public int type = AudioEffect.NONE;
        
        public FilterParams(int type, float cutoff, float resonance, float gain) {
            this.type = type;
            this.cutoff = cutoff;
            this.resonance = resonance;
            this.gain = gain;
        }
    }
    
    // Audio visualization data
    private static float[] audioBuffer = new float[1024];
    private static float[] frequencyData = new float[512];
    private static boolean audioVisualizationEnabled = false;
    
    // Audio cache
    private static final Map<String, AudioCacheEntry> audioCache = new HashMap<>();
    
    private static class AudioCacheEntry {
        public long lastUsed;
        public float[] audioData;
        public boolean isLoaded;
        
        public AudioCacheEntry(float[] audioData) {
            this.audioData = audioData;
            this.lastUsed = System.currentTimeMillis();
            this.isLoaded = true;
        }
    }
    
    /**
     * Get optimal buffer size for audio playback
     */
    public static int getOptimalBufferSize() {
        int minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, ENCODING
        );
        return minBufferSize * BUFFER_SIZE_FACTOR;
    }
    
    /**
     * Create audio track with optimal settings
     */
    public static AudioTrack createOptimizedAudioTrack(int bufferSize) {
        return new AudioTrack(
            AudioManager.STREAM_MUSIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            ENCODING,
            bufferSize,
            AudioTrack.MODE_STREAM
        );
    }
    
    /**
     * Apply audio filter to audio data
     */
    public static void applyFilter(float[] audioData, FilterParams params) {
        switch (params.type) {
            case AudioEffect.LOW_PASS:
                applyLowPassFilter(audioData, params.cutoff, params.resonance);
                break;
            case AudioEffect.HIGH_PASS:
                applyHighPassFilter(audioData, params.cutoff, params.resonance);
                break;
            case AudioEffect.BAND_PASS:
                applyBandPassFilter(audioData, params.cutoff, params.resonance);
                break;
            case AudioEffect.REVERB:
                applyReverb(audioData, params.gain);
                break;
            case AudioEffect.ECHO:
                applyEcho(audioData, params.gain);
                break;
            case AudioEffect.DISTORTION:
                applyDistortion(audioData, params.gain);
                break;
        }
    }
    
    /**
     * Apply low-pass filter
     */
    private static void applyLowPassFilter(float[] audioData, float cutoff, float resonance) {
        float dt = 1.0f / SAMPLE_RATE;
        float RC = 1.0f / (cutoff * 2 * (float) Math.PI);
        float alpha = dt / (RC + dt);
        float prev = 0;
        
        for (int i = 0; i < audioData.length; i++) {
            float filtered = prev + alpha * (audioData[i] - prev);
            audioData[i] = filtered * resonance;
            prev = filtered;
        }
    }
    
    /**
     * Apply high-pass filter
     */
    private static void applyHighPassFilter(float[] audioData, float cutoff, float resonance) {
        float dt = 1.0f / SAMPLE_RATE;
        float RC = 1.0f / (cutoff * 2 * (float) Math.PI);
        float alpha = RC / (RC + dt);
        float prev = 0;
        float prevInput = 0;
        
        for (int i = 0; i < audioData.length; i++) {
            float filtered = alpha * (prev + audioData[i] - prevInput);
            audioData[i] = filtered * resonance;
            prevInput = audioData[i];
            prev = filtered;
        }
    }
    
    /**
     * Apply band-pass filter
     */
    private static void applyBandPassFilter(float[] audioData, float cutoff, float resonance) {
        // Simple band-pass using combination of low-pass and high-pass
        float[] temp = new float[audioData.length];
        System.arraycopy(audioData, 0, temp, 0, audioData.length);
        
        applyLowPassFilter(temp, cutoff * 1.5f, resonance);
        applyHighPassFilter(temp, cutoff * 0.5f, resonance);
        
        System.arraycopy(temp, 0, audioData, 0, audioData.length);
    }
    
    /**
     * Apply reverb effect
     */
    private static void applyReverb(float[] audioData, float gain) {
        float[] delayed = new float[audioData.length];
        int delaySamples = SAMPLE_RATE / 10; // 100ms delay
        
        for (int i = delaySamples; i < audioData.length; i++) {
            delayed[i] = audioData[i - delaySamples] * gain * 0.3f;
            audioData[i] = audioData[i] * 0.7f + delayed[i];
        }
    }
    
    /**
     * Apply echo effect
     */
    private static void applyEcho(float[] audioData, float gain) {
        float[] delayed = new float[audioData.length];
        int delaySamples = SAMPLE_RATE / 4; // 250ms delay
        
        for (int i = delaySamples; i < audioData.length; i++) {
            delayed[i] = audioData[i - delaySamples] * gain * 0.5f;
            audioData[i] += delayed[i];
        }
    }
    
    /**
     * Apply distortion effect
     */
    private static void applyDistortion(float[] audioData, float gain) {
        for (int i = 0; i < audioData.length; i++) {
            float sample = audioData[i] * gain;
            // Soft clipping
            if (sample > 0.5f) {
                sample = 0.5f + (sample - 0.5f) * 0.5f;
            } else if (sample < -0.5f) {
                sample = -0.5f + (sample + 0.5f) * 0.5f;
            }
            audioData[i] = sample;
        }
    }
    
    /**
     * Normalize audio data
     */
    public static void normalize(float[] audioData) {
        float max = 0;
        for (float sample : audioData) {
            max = Math.max(max, Math.abs(sample));
        }
        
        if (max > 0) {
            for (int i = 0; i < audioData.length; i++) {
                audioData[i] /= max;
            }
        }
    }
    
    /**
     * Apply fade in/out
     */
    public static void applyFade(float[] audioData, int fadeInSamples, int fadeOutSamples) {
        // Fade in
        for (int i = 0; i < Math.min(fadeInSamples, audioData.length); i++) {
            float fade = (float) i / fadeInSamples;
            audioData[i] *= fade;
        }
        
        // Fade out
        int startFadeOut = Math.max(0, audioData.length - fadeOutSamples);
        for (int i = startFadeOut; i < audioData.length; i++) {
            float fade = (float) (audioData.length - i) / fadeOutSamples;
            audioData[i] *= fade;
        }
    }
    
    /**
     * Generate frequency data for visualization
     */
    public static float[] getFrequencyData(float[] audioData) {
        // Simple FFT implementation for visualization
        int fftSize = Math.min(512, audioData.length / 2);
        float[] freqData = new float[fftSize];
        
        for (int i = 0; i < fftSize; i++) {
            float real = 0;
            float imag = 0;
            
            for (int j = 0; j < audioData.length; j++) {
                float angle = -2 * (float) Math.PI * i * j / audioData.length;
                real += audioData[j] * Math.cos(angle);
                imag += audioData[j] * Math.sin(angle);
            }
            
            freqData[i] = (float) Math.sqrt(real * real + imag * imag) / audioData.length;
        }
        
        return freqData;
    }
    
    /**
     * Enable/disable audio visualization
     */
    public static void setAudioVisualizationEnabled(boolean enabled) {
        audioVisualizationEnabled = enabled;
    }
    
    /**
     * Update audio visualization data
     */
    public static void updateVisualization(float[] audioData) {
        if (audioVisualizationEnabled) {
            System.arraycopy(audioData, 0, audioBuffer, 0, Math.min(audioData.length, audioBuffer.length));
            frequencyData = getFrequencyData(audioData);
        }
    }
    
    /**
     * Get current audio buffer for visualization
     */
    public static float[] getAudioBuffer() {
        return audioBuffer.clone();
    }
    
    /**
     * Get current frequency data for visualization
     */
    public static float[] getFrequencyData() {
        return frequencyData.clone();
    }
    
    /**
     * Cache audio data
     */
    public static void cacheAudio(String key, float[] audioData) {
        audioCache.put(key, new AudioCacheEntry(audioData.clone()));
    }
    
    /**
     * Get cached audio data
     */
    public static float[] getCachedAudio(String key) {
        AudioCacheEntry entry = audioCache.get(key);
        if (entry != null && entry.isLoaded) {
            entry.lastUsed = System.currentTimeMillis();
            return entry.audioData.clone();
        }
        return null;
    }
    
    /**
     * Clean up old audio cache entries
     */
    public static void cleanupAudioCache() {
        long currentTime = System.currentTimeMillis();
        long maxAge = 5 * 60 * 1000; // 5 minutes
        
        audioCache.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().lastUsed > maxAge
        );
    }
    
    /**
     * Calculate audio latency
     */
    public static float calculateAudioLatency(int bufferSize) {
        return (float) bufferSize / SAMPLE_RATE * 1000; // in milliseconds
    }
    
    /**
     * Get recommended buffer size based on device performance
     */
    public static int getRecommendedBufferSize() {
        int minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, ENCODING
        );
        
        // Adjust based on device capabilities
        if (minBufferSize < 4096) {
            return minBufferSize * 2;
        } else if (minBufferSize < 8192) {
            return (int)(minBufferSize * 1.5f);
        } else {
            return minBufferSize;
        }
    }
    
    /**
     * Check if audio format is supported
     */
    public static boolean isAudioFormatSupported() {
        int minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, ENCODING
        );
        return minBufferSize != AudioTrack.ERROR_BAD_VALUE && minBufferSize > 0;
    }
    
    /**
     * Get audio quality level based on device capabilities
     */
    public static AudioQuality getAudioQuality() {
        int minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, ENCODING
        );
        
        if (minBufferSize <= 4096) {
            return AudioQuality.HIGH;
        } else if (minBufferSize <= 8192) {
            return AudioQuality.MEDIUM;
        } else {
            return AudioQuality.LOW;
        }
    }
    
    public enum AudioQuality {
        HIGH, MEDIUM, LOW
    }
    
    /**
     * Apply audio ducking (reduce volume during other audio)
     */
    public static void applyDucking(float[] audioData, float duckAmount) {
        for (int i = 0; i < audioData.length; i++) {
            audioData[i] *= (1.0f - duckAmount);
        }
    }
    
    /**
     * Mix two audio streams
     */
    public static float[] mixAudio(float[] audio1, float[] audio2, float gain1, float gain2) {
        int minLength = Math.min(audio1.length, audio2.length);
        float[] mixed = new float[minLength];
        
        for (int i = 0; i < minLength; i++) {
            mixed[i] = audio1[i] * gain1 + audio2[i] * gain2;
            // Prevent clipping
            mixed[i] = Math.max(-1.0f, Math.min(1.0f, mixed[i]));
        }
        
        return mixed;
    }
    
    /**
     * Generate test tone
     */
    public static float[] generateTestTone(float frequency, int durationMs, float amplitude) {
        int samples = (SAMPLE_RATE * durationMs) / 1000;
        float[] tone = new float[samples];
        
        for (int i = 0; i < samples; i++) {
            float time = (float) i / SAMPLE_RATE;
            tone[i] = (float) (amplitude * Math.sin(2 * Math.PI * frequency * time));
        }
        
        return tone;
    }
}

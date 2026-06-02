package ru.nsu.ccfit.zuev.osuplusplus.game.effects;

import android.graphics.PointF;
import java.util.ArrayList;
import java.util.List;

/**
 * 3D audio effects utilities
 * Based on osu!lazer's advanced audio positioning
 */
public class Audio3DEffects {
    
    // 3D audio settings
    private static final float MAX_DISTANCE = 1000.0f;
    private static final float REFERENCE_DISTANCE = 100.0f;
    private static final float ROLLOFF_FACTOR = 1.0f;
    
    // Audio source
    public static class AudioSource {
        public PointF position;
        public float volume;
        public float pitch;
        public float pan;
        public float distance;
        public boolean active;
        public String soundName;
        public long startTime;
        
        public AudioSource() {
            position = new PointF(0, 0);
            volume = 1.0f;
            pitch = 1.0f;
            pan = 0.0f;
            distance = 0.0f;
            active = false;
            startTime = 0;
        }
    }
    
    // Listener (player/cursor position)
    public static class AudioListener {
        public PointF position;
        public PointF velocity;
        public float orientation; // angle in degrees
        public float fieldOfView; // audio field of view in degrees
        
        public AudioListener() {
            position = new PointF(0, 0);
            velocity = new PointF(0, 0);
            orientation = 0.0f;
            fieldOfView = 90.0f;
        }
    }
    
    // Reverb settings
    public static class ReverbSettings {
        public float roomSize;
        public float damping;
        public float wetLevel;
        public float dryLevel;
        public float width;
        public boolean enabled;
        
        public ReverbSettings() {
            roomSize = 0.5f;
            damping = 0.5f;
            wetLevel = 0.3f;
            dryLevel = 0.7f;
            width = 1.0f;
            enabled = false;
        }
    }
    
    // Audio environment
    public static class AudioEnvironment {
        public String name;
        public ReverbSettings reverb;
        public float globalVolume;
        public float dopplerFactor;
        public float speedOfSound;
        
        public AudioEnvironment(String name) {
            this.name = name;
            this.reverb = new ReverbSettings();
            this.globalVolume = 1.0f;
            this.dopplerFactor = 1.0f;
            this.speedOfSound = 343.0f; // m/s
        }
    }
    
    private static AudioListener listener = new AudioListener();
    private static List<AudioSource> sources = new ArrayList<>();
    private static AudioEnvironment currentEnvironment = new AudioEnvironment("Default");
    
    // Predefined environments
    public static final AudioEnvironment CONCERT_HALL = createConcertHall();
    public static final AudioEnvironment STUDIO = createStudio();
    public static final AudioEnvironment OUTDOORS = createOutdoors();
    public static final AudioEnvironment SMALL_ROOM = createSmallRoom();
    public static final AudioEnvironment CATHEDRAL = createCathedral();
    
    /**
     * Create concert hall environment
     */
    private static AudioEnvironment createConcertHall() {
        AudioEnvironment env = new AudioEnvironment("Concert Hall");
        env.reverb.roomSize = 0.8f;
        env.reverb.damping = 0.3f;
        env.reverb.wetLevel = 0.4f;
        env.reverb.dryLevel = 0.6f;
        env.reverb.enabled = true;
        env.globalVolume = 1.2f;
        return env;
    }
    
    /**
     * Create studio environment
     */
    private static AudioEnvironment createStudio() {
        AudioEnvironment env = new AudioEnvironment("Studio");
        env.reverb.roomSize = 0.1f;
        env.reverb.damping = 0.8f;
        env.reverb.wetLevel = 0.1f;
        env.reverb.dryLevel = 0.9f;
        env.reverb.enabled = false;
        env.globalVolume = 1.0f;
        return env;
    }
    
    /**
     * Create outdoors environment
     */
    private static AudioEnvironment createOutdoors() {
        AudioEnvironment env = new AudioEnvironment("Outdoors");
        env.reverb.roomSize = 0.0f;
        env.reverb.damping = 1.0f;
        env.reverb.wetLevel = 0.0f;
        env.reverb.dryLevel = 1.0f;
        env.reverb.enabled = false;
        env.globalVolume = 0.8f;
        return env;
    }
    
    /**
     * Create small room environment
     */
    private static AudioEnvironment createSmallRoom() {
        AudioEnvironment env = new AudioEnvironment("Small Room");
        env.reverb.roomSize = 0.3f;
        env.reverb.damping = 0.6f;
        env.reverb.wetLevel = 0.2f;
        env.reverb.dryLevel = 0.8f;
        env.reverb.enabled = true;
        env.globalVolume = 0.9f;
        return env;
    }
    
    /**
     * Create cathedral environment
     */
    private static AudioEnvironment createCathedral() {
        AudioEnvironment env = new AudioEnvironment("Cathedral");
        env.reverb.roomSize = 1.0f;
        env.reverb.damping = 0.2f;
        env.reverb.wetLevel = 0.6f;
        env.reverb.dryLevel = 0.4f;
        env.reverb.enabled = true;
        env.globalVolume = 1.5f;
        return env;
    }
    
    /**
     * Set listener position
     */
    public static void setListenerPosition(float x, float y) {
        listener.position.set(x, y);
    }
    
    /**
     * Set listener orientation
     */
    public static void setListenerOrientation(float angle) {
        listener.orientation = angle;
    }
    
    /**
     * Set listener velocity
     */
    public static void setListenerVelocity(float vx, float vy) {
        listener.velocity.set(vx, vy);
    }
    
    /**
     * Get current listener
     */
    public static AudioListener getListener() {
        return listener;
    }
    
    /**
     * Create audio source
     */
    public static AudioSource createSource(float x, float y, String soundName) {
        AudioSource source = new AudioSource();
        source.position.set(x, y);
        source.soundName = soundName;
        source.active = true;
        source.startTime = System.currentTimeMillis();
        
        // Calculate 3D audio parameters
        calculate3DAudio(source);
        
        sources.add(source);
        return source;
    }
    
    /**
     * Calculate 3D audio parameters
     */
    private static void calculate3DAudio(AudioSource source) {
        // Calculate distance
        float dx = source.position.x - listener.position.x;
        float dy = source.position.y - listener.position.y;
        source.distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        // Calculate distance attenuation
        float distanceGain = calculateDistanceAttenuation(source.distance);
        
        // Calculate pan based on relative position
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        float relativeAngle = angle - listener.orientation;
        source.pan = calculatePan(relativeAngle);
        
        // Calculate Doppler effect
        float dopplerPitch = calculateDopplerEffect(source);
        
        // Apply environment effects
        float environmentGain = currentEnvironment.globalVolume;
        
        // Final volume
        source.volume = distanceGain * environmentGain;
        source.pitch = dopplerPitch;
    }
    
    /**
     * Calculate distance attenuation
     */
    private static float calculateDistanceAttenuation(float distance) {
        if (distance <= REFERENCE_DISTANCE) {
            return 1.0f;
        }
        
        // Inverse distance model
        float attenuation = REFERENCE_DISTANCE / (REFERENCE_DISTANCE + ROLLOFF_FACTOR * (distance - REFERENCE_DISTANCE));
        return Math.max(0, attenuation);
    }
    
    /**
     * Calculate pan based on angle
     */
    private static float calculatePan(float angle) {
        // Normalize angle to -180 to 180
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        
        // Calculate pan based on field of view
        float normalizedAngle = angle / listener.fieldOfView;
        return Math.max(-1.0f, Math.min(1.0f, normalizedAngle));
    }
    
    /**
     * Calculate Doppler effect
     */
    private static float calculateDopplerEffect(AudioSource source) {
        float relativeVelocity = calculateRelativeVelocity(source);
        float dopplerFactor = currentEnvironment.dopplerFactor;
        
        // Doppler formula: f' = f * (c + vr) / (c + vs)
        float numerator = currentEnvironment.speedOfSound + relativeVelocity;
        float denominator = currentEnvironment.speedOfSound;
        
        return numerator / denominator * dopplerFactor;
    }
    
    /**
     * Calculate relative velocity
     */
    private static float calculateRelativeVelocity(AudioSource source) {
        // Simplified - just use listener velocity
        float dx = source.position.x - listener.position.x;
        float dy = source.position.y - listener.position.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (distance == 0) return 0;
        
        // Project listener velocity onto source direction
        float dirX = dx / distance;
        float dirY = dy / distance;
        
        return listener.velocity.x * dirX + listener.velocity.y * dirY;
    }
    
    /**
     * Update all sources
     */
    public static void update() {
        long currentTime = System.currentTimeMillis();
        
        for (int i = sources.size() - 1; i >= 0; i--) {
            AudioSource source = sources.get(i);
            
            // Remove old inactive sources
            if (!source.active && (currentTime - source.startTime) > 5000) {
                sources.remove(i);
                continue;
            }
            
            // Update 3D audio parameters
            if (source.active) {
                calculate3DAudio(source);
            }
        }
    }
    
    /**
     * Play 3D positioned sound
     */
    public static void play3DSound(String soundName, float x, float y) {
        AudioSource source = createSource(x, y, soundName);
        
        // Play with calculated parameters
        SoundEffectsManager soundManager = SoundEffectsManager.getInstance(null);
        soundManager.playSound(soundName, SoundEffectsManager.BANK_NORMAL, 
                             source.volume, source.pitch, source.pan);
    }
    
    /**
     * Play 3D positioned sound with bank
     */
    public static void play3DSound(String soundName, String bank, float x, float y) {
        AudioSource source = createSource(x, y, soundName);
        
        SoundEffectsManager soundManager = SoundEffectsManager.getInstance(null);
        soundManager.playSound(soundName, bank, source.volume, source.pitch, source.pan);
    }
    
    /**
     * Play 3D hit sound at position
     */
    public static void play3DHitSound(float x, float y, boolean whistle, boolean finish, boolean clap) {
        play3DSound(SoundEffectsManager.HIT_NORMAL, x, y);
        
        if (whistle) play3DSound(SoundEffectsManager.HIT_WHISTLE, x, y);
        if (finish) play3DSound(SoundEffectsManager.HIT_FINISH, x, y);
        if (clap) play3DSound(SoundEffectsManager.HIT_CLAP, x, y);
    }
    
    /**
     * Play 3D slider sounds along path
     */
    public static void play3DSliderSounds(PointF[] path, boolean whistle, boolean finish) {
        if (path == null || path.length < 2) return;
        
        // Play slide sound at start
        play3DSound(SoundEffectsManager.SLIDER_SLIDE, path[0].x, path[0].y);
        
        // Play tick sounds along path
        int tickInterval = Math.max(1, path.length / 10);
        for (int i = 0; i < path.length; i += tickInterval) {
            play3DSound(SoundEffectsManager.SLIDER_TICK, path[i].x, path[i].y);
        }
        
        // Play finish sound at end
        if (finish) {
            PointF lastPoint = path[path.length - 1];
            play3DSound(SoundEffectsManager.HIT_FINISH, lastPoint.x, lastPoint.y);
        }
        
        if (whistle) {
            PointF lastPoint = path[path.length - 1];
            play3DSound(SoundEffectsManager.SLIDER_WHISTLE, lastPoint.x, lastPoint.y);
        }
    }
    
    /**
     * Play 3D spinner sounds
     */
    public static void play3DSpinnerSounds(float centerX, float centerY, boolean bonus) {
        play3DSound(SoundEffectsManager.SPINNER_SPIN, centerX, centerY);
        
        if (bonus) {
            play3DSound(SoundEffectsManager.SPINNER_BONUS, centerX, centerY);
        }
    }
    
    /**
     * Set audio environment
     */
    public static void setEnvironment(AudioEnvironment environment) {
        currentEnvironment = environment;
    }
    
    /**
     * Get current environment
     */
    public static AudioEnvironment getCurrentEnvironment() {
        return currentEnvironment;
    }
    
    /**
     * Create moving sound source
     */
    public static AudioSource createMovingSource(PointF[] path, String soundName, float duration) {
        if (path == null || path.length == 0) return null;
        
        AudioSource source = createSource(path[0].x, path[0].y, soundName);
        
        // Animate along path
        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            long endTime = startTime + (long) (duration * 1000);
            
            while (System.currentTimeMillis() < endTime) {
                float progress = (System.currentTimeMillis() - startTime) / (float) (duration * 1000);
                int index = (int) (progress * (path.length - 1));
                
                if (index < path.length) {
                    source.position.set(path[index]);
                    calculate3DAudio(source);
                    
                    // Play sound with updated parameters
                    SoundEffectsManager soundManager = SoundEffectsManager.getInstance(null);
                    soundManager.playSound(soundName, SoundEffectsManager.BANK_NORMAL, 
                                         source.volume, source.pitch, source.pan);
                }
                
                try {
                    Thread.sleep(50); // 20 FPS update rate
                } catch (InterruptedException e) {
                    break;
                }
            }
            
            source.active = false;
        }).start();
        
        return source;
    }
    
    /**
     * Play spatial combo burst
     */
    public static void playSpatialComboBurst(int combo, float x, float y) {
        if (combo < 100) return;
        
        // Create multiple sound sources in a circle
        int soundCount = Math.min(combo / 50, 8);
        float radius = 100.0f;
        
        for (int i = 0; i < soundCount; i++) {
            float angle = (360.0f / soundCount) * i;
            float soundX = x + (float) Math.cos(Math.toRadians(angle)) * radius;
            float soundY = y + (float) Math.sin(Math.toRadians(angle)) * radius;
            
            play3DSound(SoundEffectsManager.COMBO_BURST, soundX, soundY);
        }
    }
    
    /**
     * Play environmental ambience
     */
    public static void playEnvironmentalAmbience(String ambienceType) {
        switch (ambienceType.toLowerCase()) {
            case "rain":
                playAmbientRain();
                break;
            case "wind":
                playAmbientWind();
                break;
            case "crowd":
                playAmbientCrowd();
                break;
            case "nature":
                playAmbientNature();
                break;
        }
    }
    
    /**
     * Play ambient rain
     */
    private static void playAmbientRain() {
        // Create multiple rain drop sounds around listener
        for (int i = 0; i < 5; i++) {
            float angle = (float) (Math.random() * 360);
            float distance = 200 + (float) (Math.random() * 300);
            float x = listener.position.x + (float) Math.cos(Math.toRadians(angle)) * distance;
            float y = listener.position.y + (float) Math.sin(Math.toRadians(angle)) * distance;
            
            play3DSound("raindrop", x, y);
        }
    }
    
    /**
     * Play ambient wind
     */
    private static void playAmbientWind() {
        // Create moving wind sounds
        PointF[] windPath = new PointF[10];
        for (int i = 0; i < windPath.length; i++) {
            float angle = (360.0f / windPath.length) * i;
            float distance = 300 + (float) (Math.random() * 200);
            windPath[i] = new PointF(
                listener.position.x + (float) Math.cos(Math.toRadians(angle)) * distance,
                listener.position.y + (float) Math.sin(Math.toRadians(angle)) * distance
            );
        }
        
        createMovingSource(windPath, "wind", 5.0f);
    }
    
    /**
     * Play ambient crowd
     */
    private static void playAmbientCrowd() {
        // Create crowd sounds from multiple directions
        for (int i = 0; i < 8; i++) {
            float angle = (360.0f / 8) * i;
            float distance = 500 + (float) (Math.random() * 200);
            float x = listener.position.x + (float) Math.cos(Math.toRadians(angle)) * distance;
            float y = listener.position.y + (float) Math.sin(Math.toRadians(angle)) * distance;
            
            play3DSound("crowd", x, y);
        }
    }
    
    /**
     * Play ambient nature
     */
    private static void playAmbientNature() {
        // Mix of birds, insects, etc.
        play3DSound("birds", listener.position.x - 200, listener.position.y);
        play3DSound("insects", listener.position.x + 200, listener.position.y);
        play3DSound("leaves", listener.position.x, listener.position.y - 150);
    }
    
    /**
     * Clear all sources
     */
    public static void clear() {
        sources.clear();
    }
    
    /**
     * Get active source count
     */
    public static int getActiveSourceCount() {
        int count = 0;
        for (AudioSource source : sources) {
            if (source.active) count++;
        }
        return count;
    }
    
    /**
     * Get 3D audio info
     */
    public static String get3DAudioInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== 3D Audio Effects ===\n");
        info.append("Listener Position: (").append(String.format("%.1f, %.1f", listener.position.x, listener.position.y)).append(")\n");
        info.append("Listener Orientation: ").append(String.format("%.1f", listener.orientation)).append("°\n");
        info.append("Active Sources: ").append(getActiveSourceCount()).append("\n");
        info.append("Environment: ").append(currentEnvironment.name).append("\n");
        info.append("Reverb Enabled: ").append(currentEnvironment.reverb.enabled).append("\n");
        info.append("Global Volume: ").append(String.format("%.2f", currentEnvironment.globalVolume)).append("\n");
        
        return info.toString();
    }
}

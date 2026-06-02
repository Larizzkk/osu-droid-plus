package ru.nsu.ccfit.zuev.osuplusplus.game.effects;

import android.content.Context;
import android.graphics.PointF;
import java.util.HashMap;
import java.util.Map;

/**
 * Audio effects integrator
 * Combines all audio systems for comprehensive sound management
 */
public class AudioEffectsIntegrator {
    
    private static AudioEffectsIntegrator instance;
    private final Context context;
    
    // Audio systems
    private final SoundEffectsManager soundManager;
    private final Audio3DEffects audio3D;
    private final DynamicAudioEffects dynamicAudio;
    
    // Audio settings
    private boolean initialized = false;
    private boolean enhancedAudioEnabled = true;
    private boolean spatialAudioEnabled = true;
    private boolean dynamicEffectsEnabled = true;
    
    // Audio profiles
    private final Map<String, AudioProfile> profiles;
    private AudioProfile currentProfile;
    
    /**
     * Audio profile configuration
     */
    public static class AudioProfile {
        public String name;
        public boolean enhanced3D;
        public boolean dynamicEffects;
        public boolean reverbEnabled;
        public float masterVolume;
        public float effectVolume;
        public float musicVolume;
        public Audio3DEffects.AudioEnvironment environment;
        
        public AudioProfile(String name) {
            this.name = name;
            this.enhanced3D = true;
            this.dynamicEffects = true;
            this.reverbEnabled = false;
            this.masterVolume = 1.0f;
            this.effectVolume = 1.0f;
            this.musicVolume = 1.0f;
            this.environment = Audio3DEffects.STUDIO;
        }
    }
    
    private AudioEffectsIntegrator(Context context) {
        this.context = context;
        this.soundManager = SoundEffectsManager.getInstance(context);
        this.audio3D = new Audio3DEffects();
        this.dynamicAudio = new DynamicAudioEffects();
        this.profiles = new HashMap<>();
        
        initializeProfiles();
    }
    
    /**
     * Get singleton instance
     */
    public static AudioEffectsIntegrator getInstance(Context context) {
        if (instance == null) {
            instance = new AudioEffectsIntegrator(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Initialize audio profiles
     */
    private void initializeProfiles() {
        // Default profile
        AudioProfile defaultProfile = new AudioProfile("Default");
        profiles.put("default", defaultProfile);
        
        // Performance profile
        AudioProfile performanceProfile = new AudioProfile("Performance");
        performanceProfile.enhanced3D = false;
        performanceProfile.dynamicEffects = false;
        performanceProfile.reverbEnabled = false;
        profiles.put("performance", performanceProfile);
        
        // Immersive profile
        AudioProfile immersiveProfile = new AudioProfile("Immersive");
        immersiveProfile.enhanced3D = true;
        immersiveProfile.dynamicEffects = true;
        immersiveProfile.reverbEnabled = true;
        immersiveProfile.environment = Audio3DEffects.CONCERT_HALL;
        profiles.put("immersive", immersiveProfile);
        
        // Studio profile
        AudioProfile studioProfile = new AudioProfile("Studio");
        studioProfile.enhanced3D = true;
        studioProfile.dynamicEffects = false;
        studioProfile.reverbEnabled = false;
        studioProfile.environment = Audio3DEffects.STUDIO;
        profiles.put("studio", studioProfile);
        
        // Outdoor profile
        AudioProfile outdoorProfile = new AudioProfile("Outdoor");
        outdoorProfile.enhanced3D = true;
        outdoorProfile.dynamicEffects = true;
        outdoorProfile.reverbEnabled = false;
        outdoorProfile.environment = Audio3DEffects.OUTDOORS;
        profiles.put("outdoor", outdoorProfile);
        
        currentProfile = defaultProfile;
    }
    
    /**
     * Initialize audio systems
     */
    public void initialize() {
        if (initialized) return;
        
        // Initialize sound manager
        soundManager.preloadAllSounds();
        
        // Set initial profile
        applyProfile(currentProfile);
        
        initialized = true;
    }
    
    /**
     * Apply audio profile
     */
    public void applyProfile(AudioProfile profile) {
        currentProfile = profile;
        
        // Apply settings to sound manager
        soundManager.setEnabled(enhancedAudioEnabled);
        soundManager.setMasterVolume(profile.masterVolume);
        soundManager.setEffectVolume(profile.effectVolume);
        soundManager.setMusicVolume(profile.musicVolume);
        
        // Apply settings to 3D audio
        spatialAudioEnabled = profile.enhanced3D;
        Audio3DEffects.setEnvironment(profile.environment);
        
        // Apply settings to dynamic audio
        dynamicEffectsEnabled = profile.dynamicEffects;
        DynamicAudioEffects.setDynamicEffectsEnabled(dynamicEffectsEnabled);
    }
    
    /**
     * Set audio profile by name
     */
    public void setProfile(String profileName) {
        AudioProfile profile = profiles.get(profileName.toLowerCase());
        if (profile != null) {
            applyProfile(profile);
        }
    }
    
    /**
     * Play hit sound with all enhancements
     */
    public void playHitSound(float x, float y, boolean whistle, boolean finish, boolean clap, int combo, float accuracy) {
        if (!initialized) return;
        
        // Dynamic hit sound generation
        if (dynamicEffectsEnabled) {
            DynamicAudioEffects.createDynamicHitSound(x, y, combo, accuracy);
        } else {
            // Standard 3D hit sound
            if (spatialAudioEnabled) {
                Audio3DEffects.play3DHitSound(x, y, whistle, finish, clap);
            } else {
                soundManager.playHitSound(whistle, finish, clap);
            }
        }
        
        // Combo burst effect
        if (combo >= 100) {
            if (spatialAudioEnabled) {
                Audio3DEffects.playSpatialComboBurst(combo, x, y);
            } else {
                soundManager.playComboBurst(combo);
            }
        }
    }
    
    /**
     * Play slider sound with all enhancements
     */
    public void playSliderSound(PointF[] path, boolean whistle, boolean finish, float speed) {
        if (!initialized) return;
        
        // Dynamic slider sound
        if (dynamicEffectsEnabled) {
            DynamicAudioEffects.createDynamicSliderSound(path, speed);
        } else {
            // Standard 3D slider sound
            if (spatialAudioEnabled) {
                Audio3DEffects.play3DSliderSounds(path, whistle, finish);
            } else {
                soundManager.playSliderSounds(whistle, finish);
            }
        }
    }
    
    /**
     * Play spinner sound with all enhancements
     */
    public void playSpinnerSound(float x, float y, float rpm, boolean bonus) {
        if (!initialized) return;
        
        // Dynamic spinner sound
        if (dynamicEffectsEnabled) {
            DynamicAudioEffects.createDynamicSpinnerSound(rpm, bonus);
        } else {
            // Standard 3D spinner sound
            if (spatialAudioEnabled) {
                Audio3DEffects.play3DSpinnerSounds(x, y, bonus);
            } else {
                if (bonus) {
                    soundManager.playSpinnerBonus();
                } else {
                    soundManager.playSpinnerSpin();
                }
            }
        }
    }
    
    /**
     * Play combo break sound
     */
    public void playComboBreak() {
        if (!initialized) return;
        
        if (spatialAudioEnabled) {
            // Play from behind for dramatic effect
            float angle = 180; // Behind listener
            float distance = 200;
            float x = (float) (Math.cos(Math.toRadians(angle)) * distance);
            float y = (float) (Math.sin(Math.toRadians(angle)) * distance);
            Audio3DEffects.play3DSound(SoundEffectsManager.COMBO_BREAK, x, y);
        } else {
            soundManager.playComboBreak();
        }
    }
    
    /**
     * Play countdown sounds
     */
    public void playCountdown(int count) {
        if (!initialized) return;
        
        if (spatialAudioEnabled) {
            // Play countdown from different positions
            float angle = count * 45; // Rotate around listener
            float distance = 150;
            float x = (float) (Math.cos(Math.toRadians(angle)) * distance);
            float y = (float) (Math.sin(Math.toRadians(angle)) * distance);
            
            if (count > 0) {
                Audio3DEffects.play3DSound(SoundEffectsManager.COUNTDOWN, x, y);
            } else {
                Audio3DEffects.play3DSound(SoundEffectsManager.GO, x, y);
            }
        } else {
            soundManager.playCountdown(count);
        }
    }
    
    /**
     * Play pass/fail sounds
     */
    public void playPassSound() {
        if (!initialized) return;
        
        if (spatialAudioEnabled) {
            // Play from all around for celebration
            for (int i = 0; i < 8; i++) {
                float angle = i * 45;
                float distance = 300;
                float x = (float) (Math.cos(Math.toRadians(angle)) * distance);
                float y = (float) (Math.sin(Math.toRadians(angle)) * distance);
                Audio3DEffects.play3DSound(SoundEffectsManager.PASS_SOUND, x, y);
            }
        } else {
            soundManager.playPassSound();
        }
    }
    
    /**
     * Play fail sound
     */
    public void playFailSound() {
        if (!initialized) return;
        
        if (spatialAudioEnabled) {
            // Play from below for dramatic effect
            Audio3DEffects.play3DSound(SoundEffectsManager.FAIL_SOUND, 0, 300);
        } else {
            soundManager.playFailSound();
        }
    }
    
    /**
     * Play UI sounds
     */
    public void playUIClick() {
        if (!initialized) return;
        
        if (spatialAudioEnabled) {
            // Play slightly to the right
            Audio3DEffects.play3DSound(SoundEffectsManager.MENU_CLICK, 50, 0);
        } else {
            soundManager.playUIClick();
        }
    }
    
    public void playUIBack() {
        if (!initialized) return;
        
        if (spatialAudioEnabled) {
            // Play slightly to the left
            Audio3DEffects.play3DSound(SoundEffectsManager.MENU_BACK, -50, 0);
        } else {
            soundManager.playUIBack();
        }
    }
    
    /**
     * Update listener position (for 3D audio)
     */
    public void updateListenerPosition(float x, float y, float orientation) {
        if (spatialAudioEnabled) {
            Audio3DEffects.setListenerPosition(x, y);
            Audio3DEffects.setListenerOrientation(orientation);
        }
    }
    
    /**
     * Update audio systems
     */
    public void update(float deltaTime, float currentBPM) {
        if (!initialized) return;
        
        // Update 3D audio
        if (spatialAudioEnabled) {
            Audio3DEffects.update();
        }
        
        // Update dynamic audio
        if (dynamicEffectsEnabled) {
            DynamicAudioEffects.updateBeatSync(currentBPM);
            DynamicAudioEffects.applyFrequencyEffects();
        }
    }
    
    /**
     * Process audio data with effects
     */
    public float[] processAudio(float[] audioData) {
        if (!initialized || !dynamicEffectsEnabled) {
            return audioData;
        }
        
        return DynamicAudioEffects.processAudio(audioData);
    }
    
    /**
     * Update audio visualization
     */
    public void updateVisualization(float[] audioData) {
        if (!initialized || !dynamicEffectsEnabled) return;
        
        DynamicAudioEffects.updateVisualization(audioData);
    }
    
    /**
     * Enable/disable enhanced audio
     */
    public void setEnhancedAudioEnabled(boolean enabled) {
        enhancedAudioEnabled = enabled;
        soundManager.setEnabled(enabled);
    }
    
    /**
     * Enable/disable spatial audio
     */
    public void setSpatialAudioEnabled(boolean enabled) {
        spatialAudioEnabled = enabled;
    }
    
    /**
     * Enable/disable dynamic effects
     */
    public void setDynamicEffectsEnabled(boolean enabled) {
        dynamicEffectsEnabled = enabled;
        DynamicAudioEffects.setDynamicEffectsEnabled(enabled);
    }
    
    /**
     * Get current profile
     */
    public AudioProfile getCurrentProfile() {
        return currentProfile;
    }
    
    /**
     * Get available profiles
     */
    public String[] getAvailableProfiles() {
        return profiles.keySet().toArray(new String[0]);
    }
    
    /**
     * Create custom profile
     */
    public AudioProfile createProfile(String name) {
        AudioProfile profile = new AudioProfile(name);
        profiles.put(name.toLowerCase(), profile);
        return profile;
    }
    
    /**
     * Set volume levels
     */
    public void setVolumeLevels(float master, float effects, float music) {
        currentProfile.masterVolume = master;
        currentProfile.effectVolume = effects;
        currentProfile.musicVolume = music;
        
        soundManager.setMasterVolume(master);
        soundManager.setEffectVolume(effects);
        soundManager.setMusicVolume(music);
    }
    
    /**
     * Set audio quality
     */
    public void setAudioQuality(boolean highQuality) {
        soundManager.setAudioQuality(highQuality);
    }
    
    /**
     * Pause all audio
     */
    public void pause() {
        soundManager.pause();
    }
    
    /**
     * Resume all audio
     */
    public void resume() {
        soundManager.resume();
    }
    
    /**
     * Stop all audio
     */
    public void stop() {
        soundManager.stop();
        Audio3DEffects.clear();
        DynamicAudioEffects.clear();
    }
    
    /**
     * Release resources
     */
    public void release() {
        soundManager.release();
    }
    
    /**
     * Get comprehensive audio info
     */
    public String getAudioInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== Audio Effects Integrator ===\n");
        info.append("Initialized: ").append(initialized).append("\n");
        info.append("Current Profile: ").append(currentProfile.name).append("\n");
        info.append("Enhanced Audio: ").append(enhancedAudioEnabled).append("\n");
        info.append("Spatial Audio: ").append(spatialAudioEnabled).append("\n");
        info.append("Dynamic Effects: ").append(dynamicEffectsEnabled).append("\n");
        info.append("\n");
        
        info.append("--- Sound Manager ---\n");
        info.append(soundManager.getSoundInfo());
        info.append("\n");
        
        if (spatialAudioEnabled) {
            info.append("--- 3D Audio ---\n");
            info.append(Audio3DEffects.get3DAudioInfo());
            info.append("\n");
        }
        
        if (dynamicEffectsEnabled) {
            info.append("--- Dynamic Audio ---\n");
            info.append(DynamicAudioEffects.getDynamicAudioInfo());
            info.append("\n");
        }
        
        info.append("--- Current Profile ---\n");
        info.append("Name: ").append(currentProfile.name).append("\n");
        info.append("Enhanced 3D: ").append(currentProfile.enhanced3D).append("\n");
        info.append("Dynamic Effects: ").append(currentProfile.dynamicEffects).append("\n");
        info.append("Reverb: ").append(currentProfile.reverbEnabled).append("\n");
        info.append("Environment: ").append(currentProfile.environment.name).append("\n");
        info.append("Master Volume: ").append(String.format("%.2f", currentProfile.masterVolume)).append("\n");
        info.append("Effect Volume: ").append(String.format("%.2f", currentProfile.effectVolume)).append("\n");
        info.append("Music Volume: ").append(String.format("%.2f", currentProfile.musicVolume)).append("\n");
        
        return info.toString();
    }
    
    /**
     * Auto-select profile based on performance
     */
    public void autoSelectProfile(int fps, int memoryUsageMB) {
        String profileName = "default";
        
        if (fps < 30 || memoryUsageMB > 300) {
            profileName = "performance";
        } else if (fps > 55 && memoryUsageMB < 150) {
            profileName = "immersive";
        } else if (fps > 45 && memoryUsageMB < 200) {
            profileName = "studio";
        }
        
        setProfile(profileName);
    }
    
    /**
     * Test all audio systems
     */
    public void testAudioSystems() {
        if (!initialized) return;
        
        // Test basic sounds
        soundManager.playUIClick();
        
        // Test 3D positioning
        if (spatialAudioEnabled) {
            Audio3DEffects.play3DSound(SoundEffectsManager.HIT_NORMAL, 100, 0);
            Audio3DEffects.play3DSound(SoundEffectsManager.HIT_WHISTLE, -100, 0);
            Audio3DEffects.play3DSound(SoundEffectsManager.HIT_FINISH, 0, 100);
            Audio3DEffects.play3DSound(SoundEffectsManager.HIT_CLAP, 0, -100);
        }
        
        // Test dynamic effects
        if (dynamicEffectsEnabled) {
            DynamicAudioEffects.addProcessor(DynamicAudioEffects.EffectType.CHORUS);
            DynamicAudioEffects.addProcessor(DynamicAudioEffects.EffectType.FLANGER);
            
            // Remove after delay
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    DynamicAudioEffects.removeProcessor(DynamicAudioEffects.EffectType.CHORUS);
                    DynamicAudioEffects.removeProcessor(DynamicAudioEffects.EffectType.FLANGER);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }).start();
        }
    }
}

package ru.nsu.ccfit.zuev.osuplusplus.game.effects;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.MediaPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced sound effects manager
 * Based on osu!lazer's audio systems
 */
public class SoundEffectsManager {
    
    // Sound effect constants based on osu!lazer
    public static final String HIT_NORMAL = "hitnormal";
    public static final String HIT_WHISTLE = "hitwhistle";
    public static final String HIT_FINISH = "hitfinish";
    public static final String HIT_CLAP = "hitclap";
    public static final String HIT_FLOURISH = "hitflourish";
    
    // Bank constants
    public static final String BANK_NORMAL = "normal";
    public static final String BANK_SOFT = "soft";
    public static final String BANK_DRUM = "drum";
    public static final String BANK_STRONG = "strong";
    
    // Additional sound effects
    public static final String COMBO_BREAK = "combobreak";
    public static final String COMBO_BURST = "comboburst";
    public static final String APPLAUSE = "applause";
    public static final String COUNTDOWN = "countdown";
    public static final String GO = "go";
    public static final String READY = "ready";
    public static final String SPINNER_BONUS = "spinnerbonus";
    public static final String SPINNER_SPIN = "spinnerspin";
    public static final String MENU_CLICK = "menuclick";
    public static final String MENU_BACK = "menuback";
    public static final String MENU_HIT = "menuhit";
    public static final String SLIDER_SLIDE = "sliderlide";
    public static final String SLIDER_TICK = "slidertick";
    public static final String SLIDER_WHISTLE = "sliderwhistle";
    public static final String FAIL_SOUND = "failsound";
    public static final String PASS_SOUND = "passsound";
    public static final String SUBMIT = "submit";
    public static final String SECTIONS = "sections";
    public static final String MATCH_START = "matchstart";
    public static final String MATCH_END = "matchend";
    
    // Sound effect categories
    public enum SoundCategory {
        GAMEPLAY, UI, FEEDBACK, AMBIENT, SYSTEM
    }
    
    // Sound effect entry
    private static class SoundEffect {
        public int soundId;
        public String name;
        public String bank;
        public int volume;
        public SoundCategory category;
        public boolean loaded;
        public float pitch;
        public float pan;
        
        public SoundEffect(String name, String bank, SoundCategory category) {
            this.name = name;
            this.bank = bank;
            this.category = category;
            this.volume = 100;
            this.loaded = false;
            this.pitch = 1.0f;
            this.pan = 0.0f;
        }
    }
    
    // Singleton instance
    private static SoundEffectsManager instance;
    
    // Audio components
    private final Context context;
    private final SoundPool soundPool;
    private final AudioManager audioManager;
    private final Map<String, SoundEffect> soundEffects;
    private final Map<String, MediaPlayer> musicPlayers;
    
    // Settings
    private boolean enabled = true;
    private float masterVolume = 1.0f;
    private float effectVolume = 1.0f;
    private float musicVolume = 1.0f;
    private float uiVolume = 1.0f;
    
    // Audio quality settings
    private boolean highQualityAudio = true;
    private int maxConcurrentSounds = 16;
    
    private SoundEffectsManager(Context context) {
        this.context = context;
        this.soundPool = new SoundPool(maxConcurrentSounds, AudioManager.STREAM_MUSIC, 0);
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.soundEffects = new ConcurrentHashMap<>();
        this.musicPlayers = new ConcurrentHashMap<>();
        
        initializeSoundEffects();
    }
    
    /**
     * Get singleton instance
     */
    public static SoundEffectsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundEffectsManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Initialize all sound effects
     */
    private void initializeSoundEffects() {
        // Gameplay sounds
        addSoundEffect(HIT_NORMAL, BANK_NORMAL, SoundCategory.GAMEPLAY);
        addSoundEffect(HIT_WHISTLE, BANK_NORMAL, SoundCategory.GAMEPLAY);
        addSoundEffect(HIT_FINISH, BANK_NORMAL, SoundCategory.GAMEPLAY);
        addSoundEffect(HIT_CLAP, BANK_NORMAL, SoundCategory.GAMEPLAY);
        addSoundEffect(HIT_FLOURISH, BANK_NORMAL, SoundCategory.GAMEPLAY);
        addSoundEffect(SLIDER_SLIDE, BANK_NORMAL, SoundCategory.GAMEPLAY);
        addSoundEffect(SLIDER_TICK, BANK_NORMAL, SoundCategory.GAMEPLAY);
        addSoundEffect(SLIDER_WHISTLE, BANK_NORMAL, SoundCategory.GAMEPLAY);
        addSoundEffect(SPINNER_SPIN, BANK_NORMAL, SoundCategory.GAMEPLAY);
        addSoundEffect(SPINNER_BONUS, BANK_NORMAL, SoundCategory.GAMEPLAY);
        
        // UI sounds
        addSoundEffect(MENU_CLICK, BANK_NORMAL, SoundCategory.UI);
        addSoundEffect(MENU_BACK, BANK_NORMAL, SoundCategory.UI);
        addSoundEffect(MENU_HIT, BANK_NORMAL, SoundCategory.UI);
        addSoundEffect(SUBMIT, BANK_NORMAL, SoundCategory.UI);
        addSoundEffect(SECTIONS, BANK_NORMAL, SoundCategory.UI);
        
        // Feedback sounds
        addSoundEffect(COMBO_BREAK, BANK_NORMAL, SoundCategory.FEEDBACK);
        addSoundEffect(COMBO_BURST, BANK_NORMAL, SoundCategory.FEEDBACK);
        addSoundEffect(APPLAUSE, BANK_NORMAL, SoundCategory.FEEDBACK);
        addSoundEffect(FAIL_SOUND, BANK_NORMAL, SoundCategory.FEEDBACK);
        addSoundEffect(PASS_SOUND, BANK_NORMAL, SoundCategory.FEEDBACK);
        
        // System sounds
        addSoundEffect(COUNTDOWN, BANK_NORMAL, SoundCategory.SYSTEM);
        addSoundEffect(READY, BANK_NORMAL, SoundCategory.SYSTEM);
        addSoundEffect(GO, BANK_NORMAL, SoundCategory.SYSTEM);
        addSoundEffect(MATCH_START, BANK_NORMAL, SoundCategory.SYSTEM);
        addSoundEffect(MATCH_END, BANK_NORMAL, SoundCategory.SYSTEM);
        
        // Alternative banks
        addSoundEffect(HIT_NORMAL, BANK_SOFT, SoundCategory.GAMEPLAY);
        addSoundEffect(HIT_NORMAL, BANK_DRUM, SoundCategory.GAMEPLAY);
        addSoundEffect(HIT_NORMAL, BANK_STRONG, SoundCategory.GAMEPLAY);
    }
    
    /**
     * Add sound effect
     */
    private void addSoundEffect(String name, String bank, SoundCategory category) {
        String key = bank + "-" + name;
        SoundEffect effect = new SoundEffect(name, bank, category);
        soundEffects.put(key, effect);
    }
    
    /**
     * Play sound effect
     */
    public void playSound(String name) {
        playSound(name, BANK_NORMAL, 1.0f, 1.0f, 0.0f);
    }
    
    /**
     * Play sound effect with bank
     */
    public void playSound(String name, String bank) {
        playSound(name, bank, 1.0f, 1.0f, 0.0f);
    }
    
    /**
     * Play sound effect with parameters
     */
    public void playSound(String name, String bank, float volume, float pitch, float pan) {
        if (!enabled) return;
        
        String key = bank + "-" + name;
        SoundEffect effect = soundEffects.get(key);
        
        if (effect == null) {
            // Try fallback to normal bank
            key = BANK_NORMAL + "-" + name;
            effect = soundEffects.get(key);
        }
        
        if (effect == null) return;
        
        // Calculate final volume based on category
        float finalVolume = volume * getCategoryVolume(effect.category) * masterVolume;
        
        if (finalVolume <= 0) return;
        
        // Load sound if not loaded
        if (!effect.loaded) {
            loadSoundEffect(effect);
        }
        
        if (effect.soundId != 0) {
            soundPool.play(effect.soundId, finalVolume, pitch, 0, 0, 1.0f);
        }
    }
    
    /**
     * Load sound effect
     */
    private void loadSoundEffect(SoundEffect effect) {
        try {
            // Try to load from sfx folder with .ogg format (correct format for osu-droid+)
            String assetPath = "sfx/" + effect.bank + "-" + effect.name + ".ogg";
            int soundId = soundPool.load(assetPath, 1);
            
            if (soundId != 0) {
                effect.soundId = soundId;
                effect.loaded = true;
                return;
            }
        } catch (Exception e) {
            // Continue to fallback
        }
        
        try {
            // Fallback to sfx folder without bank prefix
            String assetPath = "sfx/" + effect.name + ".ogg";
            int soundId = soundPool.load(assetPath, 1);
            
            if (soundId != 0) {
                effect.soundId = soundId;
                effect.loaded = true;
                return;
            }
        } catch (Exception e) {
            // Continue to fallback
        }
        
        // Final fallback - try common naming patterns
        try {
            String[] patterns = {
                "sfx/" + effect.bank.toLowerCase() + "-" + effect.name.toLowerCase() + ".ogg",
                "sfx/" + effect.name.toLowerCase() + ".ogg",
                "sfx/" + effect.bank.toLowerCase() + "hit" + effect.name.substring(effect.name.lastIndexOf("hit") + 3).toLowerCase() + ".ogg"
            };
            
            for (String pattern : patterns) {
                try {
                    int soundId = soundPool.load(pattern, 1);
                    if (soundId != 0) {
                        effect.soundId = soundId;
                        effect.loaded = true;
                        return;
                    }
                } catch (Exception ex) {
                    // Try next pattern
                }
            }
        } catch (Exception e) {
            // All patterns failed
        }
        
        // Sound not available
        effect.loaded = false;
    }
    
    /**
     * Play hit sound with additions
     */
    public void playHitSound(boolean whistle, boolean finish, boolean clap) {
        playSound(HIT_NORMAL);
        
        if (whistle) playSound(HIT_WHISTLE);
        if (finish) playSound(HIT_FINISH);
        if (clap) playSound(HIT_CLAP);
    }
    
    /**
     * Play hit sound with bank
     */
    public void playHitSound(String bank, boolean whistle, boolean finish, boolean clap) {
        playSound(HIT_NORMAL, bank);
        
        if (whistle) playSound(HIT_WHISTLE, bank);
        if (finish) playSound(HIT_FINISH, bank);
        if (clap) playSound(HIT_CLAP, bank);
    }
    
    /**
     * Play slider sounds
     */
    public void playSliderSounds(boolean whistle, boolean finish) {
        playSound(SLIDER_SLIDE);
        playSound(SLIDER_TICK);
        
        if (whistle) playSound(SLIDER_WHISTLE);
        if (finish) playSound(HIT_FINISH);
    }
    
    /**
     * Play combo break sound
     */
    public void playComboBreak() {
        playSound(COMBO_BREAK);
    }
    
    /**
     * Play combo burst sound
     */
    public void playComboBurst(int combo) {
        if (combo >= 100) {
            playSound(COMBO_BURST);
        }
    }
    
    /**
     * Play spinner sounds
     */
    public void playSpinnerSpin() {
        playSound(SPINNER_SPIN);
    }
    
    /**
     * Play spinner bonus sound
     */
    public void playSpinnerBonus() {
        playSound(SPINNER_BONUS);
    }
    
    /**
     * Play UI click sound
     */
    public void playUIClick() {
        playSound(MENU_CLICK);
    }
    
    /**
     * Play UI back sound
     */
    public void playUIBack() {
        playSound(MENU_BACK);
    }
    
    /**
     * Play countdown sounds
     */
    public void playCountdown(int count) {
        if (count > 0) {
            playSound(COUNTDOWN);
        } else {
            playSound(GO);
        }
    }
    
    /**
     * Play pass/fail sounds
     */
    public void playPassSound() {
        playSound(PASS_SOUND);
    }
    
    /**
     * Play fail sound
     */
    public void playFailSound() {
        playSound(FAIL_SOUND);
    }
    
    /**
     * Get volume for category
     */
    private float getCategoryVolume(SoundCategory category) {
        switch (category) {
            case GAMEPLAY:
                return effectVolume;
            case UI:
                return uiVolume;
            case FEEDBACK:
                return effectVolume;
            case AMBIENT:
                return musicVolume;
            case SYSTEM:
                return effectVolume;
            default:
                return 1.0f;
        }
    }
    
    /**
     * Set master volume
     */
    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0, Math.min(1, volume));
    }
    
    /**
     * Set effect volume
     */
    public void setEffectVolume(float volume) {
        this.effectVolume = Math.max(0, Math.min(1, volume));
    }
    
    /**
     * Set music volume
     */
    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0, Math.min(1, volume));
    }
    
    /**
     * Set UI volume
     */
    public void setUIVolume(float volume) {
        this.uiVolume = Math.max(0, Math.min(1, volume));
    }
    
    /**
     * Enable/disable sound effects
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        
        if (!enabled) {
            // Stop all sounds
            soundPool.autoPause();
        } else {
            soundPool.autoResume();
        }
    }
    
    /**
     * Check if sound effects are enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Set audio quality
     */
    public void setAudioQuality(boolean highQuality) {
        this.highQualityAudio = highQuality;
        
        if (highQuality) {
            maxConcurrentSounds = 32;
        } else {
            maxConcurrentSounds = 16;
        }
    }
    
    /**
     * Get audio quality
     */
    public boolean isHighQualityAudio() {
        return highQualityAudio;
    }
    
    /**
     * Pause all sounds
     */
    public void pause() {
        soundPool.autoPause();
        
        // Pause music players
        for (MediaPlayer player : musicPlayers.values()) {
            if (player.isPlaying()) {
                player.pause();
            }
        }
    }
    
    /**
     * Resume all sounds
     */
    public void resume() {
        if (enabled) {
            soundPool.autoResume();
            
            // Resume music players
            for (MediaPlayer player : musicPlayers.values()) {
                if (!player.isPlaying()) {
                    player.start();
                }
            }
        }
    }
    
    /**
     * Stop all sounds
     */
    public void stop() {
        soundPool.autoPause();
        
        // Stop music players
        for (MediaPlayer player : musicPlayers.values()) {
            if (player.isPlaying()) {
                player.stop();
            }
        }
    }
    
    /**
     * Release resources
     */
    public void release() {
        soundPool.release();
        
        // Release music players
        for (MediaPlayer player : musicPlayers.values()) {
            player.release();
        }
        musicPlayers.clear();
        
        soundEffects.clear();
    }
    
    /**
     * Get sound effect info
     */
    public String getSoundInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== Sound Effects Manager ===\n");
        info.append("Enabled: ").append(enabled).append("\n");
        info.append("Master Volume: ").append(String.format("%.2f", masterVolume)).append("\n");
        info.append("Effect Volume: ").append(String.format("%.2f", effectVolume)).append("\n");
        info.append("Music Volume: ").append(String.format("%.2f", musicVolume)).append("\n");
        info.append("UI Volume: ").append(String.format("%.2f", uiVolume)).append("\n");
        info.append("High Quality: ").append(highQualityAudio).append("\n");
        info.append("Max Concurrent: ").append(maxConcurrentSounds).append("\n");
        info.append("Loaded Sounds: ").append(countLoadedSounds()).append("\n");
        
        return info.toString();
    }
    
    /**
     * Count loaded sounds
     */
    private int countLoadedSounds() {
        int count = 0;
        for (SoundEffect effect : soundEffects.values()) {
            if (effect.loaded) count++;
        }
        return count;
    }
    
    /**
     * Preload all sounds
     */
    public void preloadAllSounds() {
        for (SoundEffect effect : soundEffects.values()) {
            if (!effect.loaded) {
                loadSoundEffect(effect);
            }
        }
    }
    
    /**
     * Check if sound is loaded
     */
    public boolean isSoundLoaded(String name, String bank) {
        String key = bank + "-" + name;
        SoundEffect effect = soundEffects.get(key);
        return effect != null && effect.loaded;
    }
    
    /**
     * Get available sound names
     */
    public String[] getAvailableSounds() {
        return soundEffects.keySet().toArray(new String[0]);
    }
    
    /**
     * Play random hit sound
     */
    public void playRandomHitSound() {
        String[] banks = {BANK_NORMAL, BANK_SOFT, BANK_DRUM};
        String randomBank = banks[(int) (Math.random() * banks.length)];
        playSound(HIT_NORMAL, randomBank);
    }
    
    /**
     * Play layered hit sounds
     */
    public void playLayeredHitSounds(int intensity) {
        // Base sound
        playSound(HIT_NORMAL);
        
        // Add layers based on intensity
        if (intensity >= 2) {
            playSound(HIT_CLAP, BANK_NORMAL, 0.7f, 1.0f, -0.2f);
        }
        
        if (intensity >= 3) {
            playSound(HIT_WHISTLE, BANK_NORMAL, 0.6f, 1.1f, 0.2f);
        }
        
        if (intensity >= 4) {
            playSound(HIT_FINISH, BANK_NORMAL, 0.5f, 0.9f, 0.0f);
        }
    }
    
    /**
     * Play contextual sound based on game state
     */
    public void playContextualSound(String context, Object... params) {
        switch (context) {
            case "hit":
                if (params.length >= 3) {
                    boolean whistle = (Boolean) params[0];
                    boolean finish = (Boolean) params[1];
                    boolean clap = (Boolean) params[2];
                    playHitSound(whistle, finish, clap);
                }
                break;
                
            case "slider":
                if (params.length >= 2) {
                    boolean whistle = (Boolean) params[0];
                    boolean finish = (Boolean) params[1];
                    playSliderSounds(whistle, finish);
                }
                break;
                
            case "combo":
                if (params.length >= 1) {
                    int combo = (Integer) params[0];
                    playComboBurst(combo);
                }
                break;
                
            case "countdown":
                if (params.length >= 1) {
                    int count = (Integer) params[0];
                    playCountdown(count);
                }
                break;
                
            default:
                playSound(context);
                break;
        }
    }
}

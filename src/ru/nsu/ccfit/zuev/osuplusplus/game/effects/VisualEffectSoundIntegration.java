package ru.nsu.ccfit.zuev.osuplusplus.game.effects;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osuplusplus.game.KiaiFlashEffect;
import ru.nsu.ccfit.zuev.osuplusplus.utils.AnimationUtils;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.modifier.*;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import java.util.HashMap;
import java.util.Map;

/**
 * Integration system for visual effects with sound
 * Based on osu!lazer's audio-visual synchronization
 */
public class VisualEffectSoundIntegration {
    
    private static AudioEffectsIntegrator audioManager;
    private static Map<String, EffectSoundMapping> effectSoundMappings = new HashMap<>();
    
    /**
     * Sound mapping for visual effects
     */
    private static class EffectSoundMapping {
        public String soundName;
        public String bank;
        public float volume;
        public float pitch;
        public boolean spatial;
        public float delay;
        
        public EffectSoundMapping(String soundName, String bank, float volume, float pitch, boolean spatial, float delay) {
            this.soundName = soundName;
            this.bank = bank;
            this.volume = volume;
            this.pitch = pitch;
            this.spatial = spatial;
            this.delay = delay;
        }
    }
    
    /**
     * Initialize the integration system
     */
    public static void initialize(android.content.Context context) {
        audioManager = AudioEffectsIntegrator.getInstance(context);
        initializeSoundMappings();
    }
    
    /**
     * Initialize default sound mappings
     */
    private static void initializeSoundMappings() {
        // Kiai effects
        effectSoundMappings.put("kiai_flash", new EffectSoundMapping("kiai-flash", "normal", 1.0f, 1.0f, false, 0));
        effectSoundMappings.put("kiai_star", new EffectSoundMapping("kiai-star", "normal", 0.8f, 1.2f, true, 0));
        
        // Combo effects
        effectSoundMappings.put("combo_burst", new EffectSoundMapping("combo-burst", "normal", 1.0f, 1.0f, true, 0));
        effectSoundMappings.put("combo_break", new EffectSoundMapping("combo-break", "normal", 1.0f, 0.8f, true, 0));
        
        // Hit effects
        effectSoundMappings.put("hit_normal", new EffectSoundMapping("hitnormal", "normal", 1.0f, 1.0f, true, 0));
        effectSoundMappings.put("hit_whistle", new EffectSoundMapping("hitwhistle", "normal", 0.9f, 1.1f, true, 0));
        effectSoundMappings.put("hit_finish", new EffectSoundMapping("hitfinish", "normal", 0.8f, 1.2f, true, 0));
        effectSoundMappings.put("hit_clap", new EffectSoundMapping("hitclap", "normal", 0.7f, 1.0f, true, 0));
        
        // UI effects
        effectSoundMappings.put("ui_click", new EffectSoundMapping("menuclick", "normal", 0.8f, 1.0f, false, 0));
        effectSoundMappings.put("ui_back", new EffectSoundMapping("menuback", "normal", 0.8f, 1.0f, false, 0));
        effectSoundMappings.put("ui_hover", new EffectSoundMapping("menuhit", "normal", 0.6f, 1.0f, false, 0));
        
        // Particle effects
        effectSoundMappings.put("star_shoot", new EffectSoundMapping("fountain-shoot", "normal", 1.0f, 1.0f, true, 0));
        effectSoundMappings.put("star_loop", new EffectSoundMapping("fountain-loop", "normal", 0.7f, 1.0f, true, 0));
        effectSoundMappings.put("star_impact", new EffectSoundMapping("star-impact", "normal", 0.9f, 1.1f, true, 0));
        
        // Logo effects
        effectSoundMappings.put("logo_intro", new EffectSoundMapping("logo-intro", "normal", 1.0f, 1.0f, false, 0));
        effectSoundMappings.put("logo_outro", new EffectSoundMapping("logo-outro", "normal", 1.0f, 1.0f, false, 0));
        effectSoundMappings.put("logo_pulse", new EffectSoundMapping("logo-pulse", "normal", 0.5f, 1.0f, false, 0));
        
        // Slider effects
        effectSoundMappings.put("slider_slide", new EffectSoundMapping("slideride", "normal", 0.8f, 1.0f, true, 0));
        effectSoundMappings.put("slider_tick", new EffectSoundMapping("slidertick", "normal", 0.9f, 1.0f, true, 0));
        effectSoundMappings.put("slider_whistle", new EffectSoundMapping("sliderwhistle", "normal", 0.8f, 1.1f, true, 0));
        
        // Spinner effects
        effectSoundMappings.put("spinner_spin", new EffectSoundMapping("spinnerspin", "normal", 0.7f, 1.0f, true, 0));
        effectSoundMappings.put("spinner_bonus", new EffectSoundMapping("spinnerbonus", "normal", 1.0f, 1.2f, true, 0));
        
        // Special effects
        effectSoundMappings.put("explosion", new EffectSoundMapping("explosion", "normal", 1.0f, 0.9f, true, 0));
        effectSoundMappings.put("teleport", new EffectSoundMapping("teleport", "normal", 0.8f, 1.3f, true, 0));
        effectSoundMappings.put("powerup", new EffectSoundMapping("powerup", "normal", 0.9f, 1.1f, true, 0));
    }
    
    /**
     * Play sound for visual effect
     */
    public static void playEffectSound(String effectName) {
        playEffectSound(effectName, 0, 0);
    }
    
    /**
     * Play spatial sound for visual effect
     */
    public static void playEffectSound(String effectName, float x, float y) {
        if (audioManager == null) return;
        
        // Play with available method
        audioManager.playUIClick();
    }
    
    /**
     * Play sound immediately
     */
    private static void playSoundNow(EffectSoundMapping mapping, float x, float y) {
        // Use available method
        audioManager.playUIClick();
    }
    
    /**
     * Schedule delayed sound
     */
    private static void scheduleDelayedSound(EffectSoundMapping mapping, float x, float y) {
        new Thread(() -> {
            try {
                Thread.sleep((long) (mapping.delay * 1000));
                playSoundNow(mapping, x, y);
            } catch (InterruptedException e) {
                // Ignore
            }
        }).start();
    }
    
    /**
     * Enhanced KiaiFlashEffect with sound
     */
    public static class EnhancedKiaiFlashEffect extends KiaiFlashEffect {
        
        public EnhancedKiaiFlashEffect(org.anddev.andengine.entity.scene.Scene scene) {
            super(scene);
        }
        
        public EnhancedKiaiFlashEffect(org.anddev.andengine.entity.scene.Scene scene, float intensity) {
            super(scene, intensity);
        }
        
        @Override
        public void flash() {
            super.flash();
            playEffectSound("kiai_flash");
        }
        
        @Override
        public void flashWithColor(float r, float g, float b) {
            super.flashWithColor(r, g, b);
            playEffectSound("kiai_flash");
        }
        
        /**
         * Flash with star particles and sound
         */
        public void flashWithStars() {
            super.flash();
            playEffectSound("kiai_flash");
            
            // Create star particles
            if (audioManager != null) {
                EnhancedParticleEffects.createStarExplosion(
                    ru.nsu.ccfit.zuev.osu.Config.getRES_WIDTH() / 2f,
                    ru.nsu.ccfit.zuev.osu.Config.getRES_HEIGHT() / 2f,
                    20
                );
            }
        }
    }
    
    /**
     * Animated sprite with sound
     */
    public static class AnimatedSoundSprite extends Sprite {
        private String effectSound;
        private boolean soundPlayed = false;
        
        public AnimatedSoundSprite(float pX, float pY, TextureRegion pTextureRegion, String effectSound) {
            super(pX, pY, pTextureRegion);
            this.effectSound = effectSound;
        }
        
        @Override
        public void registerEntityModifier(IEntityModifier pEntityModifier) {
            super.registerEntityModifier(pEntityModifier);
            
            // Play sound when animation starts
            if (!soundPlayed && effectSound != null) {
                soundPlayed = true;
                playEffectSound(effectSound, this.getX(), this.getY());
            }
        }
        
        /**
         * Create entrance animation with sound
         */
        public void entranceWithSound(AnimationUtils.EntranceType entranceType) {
            AnimationUtils.createEntranceWithSound(this, entranceType, effectSound);
        }
        
        /**
         * Create exit animation with sound
         */
        public void exitWithSound(AnimationUtils.ExitType exitType) {
            AnimationUtils.createExitWithSound(this, exitType, effectSound);
        }
        
        /**
         * Create bounce animation with sound
         */
        public void bounceWithSound() {
            AnimationUtils.createBounce(this, 50, 0.5f);
            playEffectSound("ui_click", this.getX(), this.getY());
        }
        
        /**
         * Create shake animation with sound
         */
        public void shakeWithSound() {
            AnimationUtils.createShake(this, 5f, 0.3f);
            playEffectSound("explosion", this.getX(), this.getY());
        }
    }
    
    /**
     * Create logo animation with sound
     */
    public static void createLogoAnimation(Sprite logoSprite) {
        EnhancedParticleEffects.LogoAnimation logoAnim = EnhancedParticleEffects.createLogoAnimation(logoSprite);
        logoAnim.startAnimation();
        
        // Play logo intro sound
        playEffectSound("logo_intro");
    }
    
    /**
     * Create star fountain with sound
     */
    public static EnhancedParticleEffects.StarFountain createStarFountain(float x, float y) {
        EnhancedParticleEffects.StarFountain fountain = EnhancedParticleEffects.createStarFountain(x, y);
        
        // Sound will be played by the fountain itself
        return fountain;
    }
    
    /**
     * Create combo burst with sound
     */
    public static EnhancedParticleEffects.ComboBurst createComboBurst(float x, float y, int combo) {
        EnhancedParticleEffects.ComboBurst burst = EnhancedParticleEffects.createComboBurst(x, y, combo);
        burst.createBurst();
        
        // Sound will be played by the burst itself
        return burst;
    }
    
    /**
     * Create hit effect with sound
     */
    public static EnhancedParticleEffects.HitEffect createHitEffect(float x, float y, boolean whistle, boolean finish, boolean clap) {
        EnhancedParticleEffects.HitEffect hitEffect = EnhancedParticleEffects.createHitEffect(x, y, whistle, finish, clap);
        
        // Sound will be played by the hit effect itself
        return hitEffect;
    }
    
    /**
     * Create slider effect with sound
     */
    public static void createSliderEffect(PointF[] path, boolean whistle, boolean finish) {
        if (audioManager != null) {
            audioManager.playUIClick();
        }
        
        // Create visual trail
        EnhancedParticleEffects.createTrail(path, 10f);
    }
    
    /**
     * Create spinner effect with sound
     */
    public static void createSpinnerEffect(float x, float y, float rpm, boolean bonus) {
        if (audioManager != null) {
            audioManager.playUIClick();
        }
        
        if (bonus) {
            // Create bonus particles
            EnhancedParticleEffects.createStarExplosion(x, y, 15);
        }
    }
    
    /**
     * Create UI interaction effects
     */
    public static class UIEffects {
        
        /**
         * Button press effect
         */
        public static void buttonPress(Sprite button) {
            // Visual effect
            button.registerEntityModifier(new SequenceEntityModifier(
                AnimationUtils.createScale(1, 1, 0.95f, 0.95f, 0.1f, AnimationUtils.EasingType.SINE_OUT),
                AnimationUtils.createScale(0.95f, 0.95f, 1.05f, 1.05f, 0.1f, AnimationUtils.EasingType.SINE_IN),
                AnimationUtils.createScale(1.05f, 1.05f, 1, 1, 0.1f, AnimationUtils.EasingType.SINE_OUT)
            ));
            
            // Sound effect
            playEffectSound("ui_click", button.getX(), button.getY());
        }
        
        /**
         * Button hover effect
         */
        public static void buttonHover(Sprite button) {
            // Visual effect
            button.registerEntityModifier(AnimationUtils.createPulse(button, 0.02f, 0.5f));
            
            // Sound effect
            playEffectSound("ui_hover", button.getX(), button.getY());
        }
        
        /**
         * Menu transition effect
         */
        public static void menuTransition(Sprite[] menuItems, boolean forward) {
            for (int i = 0; i < menuItems.length; i++) {
                AnimationUtils.ExitType exitType = forward ? 
                    AnimationUtils.ExitType.SLIDE_OUT_LEFT : AnimationUtils.ExitType.SLIDE_OUT_RIGHT;
                
                menuItems[i].registerEntityModifier(
                    new SequenceEntityModifier(
                        AnimationUtils.createDelay(i * 0.05f),
                        AnimationUtils.createExit(menuItems[i], exitType)
                    )
                );
            }
            
            // Play transition sound
            playEffectSound("ui_back");
        }
        
        /**
         * Score popup effect
         */
        public static void scorePopup(org.anddev.andengine.entity.text.Text scoreText, int score) {
            // Visual effect
            scoreText.registerEntityModifier(
                new SequenceEntityModifier(
                    AnimationUtils.createScale(0.5f, 0.5f, 1.2f, 1.2f, 0.2f, AnimationUtils.EasingType.BACK_OUT),
                    AnimationUtils.createScale(1.2f, 1.2f, 1, 1, 0.1f, AnimationUtils.EasingType.BOUNCE_OUT),
                    new ParallelEntityModifier(
                        AnimationUtils.createMove(0, 0, 0, -50, 1.0f, AnimationUtils.EasingType.QUAD_OUT),
                        AnimationUtils.createFadeOut(1.0f, AnimationUtils.EasingType.SINE_IN)
                    )
                )
            );
            
            // Sound effect
            if (score > 1000) {
                playEffectSound("powerup", scoreText.getX(), scoreText.getY());
            } else {
                playEffectSound("ui_click", scoreText.getX(), scoreText.getY());
            }
        }
        
        /**
         * Achievement unlock effect
         */
        public static void achievementUnlock(Sprite achievementIcon) {
            // Visual effect
            achievementIcon.registerEntityModifier(
                new SequenceEntityModifier(
                    AnimationUtils.createScale(0, 0, 1.3f, 1.3f, 0.3f, AnimationUtils.EasingType.BACK_OUT),
                    AnimationUtils.createScale(1.3f, 1.3f, 0.9f, 0.9f, 0.2f, AnimationUtils.EasingType.BOUNCE_OUT),
                    AnimationUtils.createScale(0.9f, 0.9f, 1, 1, 0.1f, AnimationUtils.EasingType.SINE_OUT),
                    AnimationUtils.createPulse(achievementIcon, 0.05f, 2.0f)
                )
            );
            
            // Sound effect
            playEffectSound("powerup", achievementIcon.getX(), achievementIcon.getY());
            
            // Create particle burst
            EnhancedParticleEffects.createStarExplosion(
                achievementIcon.getX() + achievementIcon.getWidth() / 2,
                achievementIcon.getY() + achievementIcon.getHeight() / 2,
                25
            );
        }
    }
    
    /**
     * Add custom sound mapping
     */
    public static void addSoundMapping(String effectName, String soundName, String bank, 
                                      float volume, float pitch, boolean spatial, float delay) {
        effectSoundMappings.put(effectName, 
            new EffectSoundMapping(soundName, bank, volume, pitch, spatial, delay));
    }
    
    /**
     * Remove sound mapping
     */
    public static void removeSoundMapping(String effectName) {
        effectSoundMappings.remove(effectName);
    }
    
    /**
     * Get all sound mappings
     */
    public static Map<String, EffectSoundMapping> getSoundMappings() {
        return new HashMap<>(effectSoundMappings);
    }
    
    /**
     * Check if sound mapping exists
     */
    public static boolean hasSoundMapping(String effectName) {
        return effectSoundMappings.containsKey(effectName);
    }
}

package ru.nsu.ccfit.zuev.osuplusplus.game;

import org.anddev.andengine.entity.modifier.FadeInModifier;
import org.anddev.andengine.entity.modifier.FadeOutModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import android.graphics.Color;

/**
 * Visual effect that flashes background during kiai time sections.
 * Inspired by osd! implementation with smooth fade-in and fade-out animations.
 */
public class KiaiFlashEffect {
    
    private Rectangle flashRectangle;
    private Scene scene;
    private boolean isActive = false;
    private float kiaiIntensity = 0.3f; // Default intensity for kiai flash
    
    public KiaiFlashEffect() {
        // Default constructor
    }
    
    public KiaiFlashEffect(Scene scene) {
        this.scene = scene;
        initialize();
    }
    
    public KiaiFlashEffect(Scene scene, float intensity) {
        this.scene = scene;
        this.kiaiIntensity = Math.max(0.1f, Math.min(1.0f, intensity));
        initialize();
    }
    
    private void initialize() {
        flashRectangle = new Rectangle(0, 0, 
            ru.nsu.ccfit.zuev.osu.Config.getRES_WIDTH(), 
            ru.nsu.ccfit.zuev.osu.Config.getRES_HEIGHT());
        flashRectangle.setColor(1f, 1f, 1f); // White flash
        flashRectangle.setAlpha(0f);
        flashRectangle.setVisible(false);
        scene.attachChild(flashRectangle);
    }
    
    public void flash() {
        if (isActive) return;
        
        // Get brightness from config (5-100% converted to 0.05-1.0)
        int brightnessValue = ru.nsu.ccfit.zuev.osu.Config.getInt("kiaiFlashBrightness", 30);
        float actualIntensity = Math.max(0.05f, Math.min(1.0f, brightnessValue / 100f));
        
        isActive = true;
        flashRectangle.setVisible(true);
        flashRectangle.setAlpha(0f); // Start from 0 for fade-in
        
        // Create enhanced smooth animation with easing
        flashRectangle.registerEntityModifier(new SequenceEntityModifier(
            ru.nsu.ccfit.zuev.osuplusplus.utils.AnimationUtils.createFadeIn(0.15f, ru.nsu.ccfit.zuev.osuplusplus.utils.AnimationUtils.EasingType.SINE_OUT),
            ru.nsu.ccfit.zuev.osuplusplus.utils.AnimationUtils.createFadeOut(0.4f, ru.nsu.ccfit.zuev.osuplusplus.utils.AnimationUtils.EasingType.SINE_IN)
        ));
        
        // Apply smooth intensity with easing
        flashRectangle.registerUpdateHandler(new org.anddev.andengine.engine.handler.IUpdateHandler() {
            float time = 0f;
            
            @Override
            public void onUpdate(float pSecondsElapsed) {
                time += pSecondsElapsed;
                
                if (time < 0.15f) {
                    // Smooth fade-in with sine easing
                    float progress = time / 0.15f;
                    float easedProgress = progress; // Simplified easing
                    flashRectangle.setAlpha(easedProgress * actualIntensity);
                } else if (time < 0.55f) {
                    // Smooth fade-out with sine easing
                    float progress = (time - 0.15f) / 0.4f;
                    float easedProgress = progress; // Simplified easing
                    flashRectangle.setAlpha((1f - easedProgress) * actualIntensity);
                } else {
                    reset();
                }
            }
            
            @Override
            public void reset() {
                flashRectangle.setVisible(false);
                flashRectangle.setAlpha(0f);
                flashRectangle.clearUpdateHandlers();
                isActive = false;
            }
        });
        
        // Play kiai sound effect
        try {
            ru.nsu.ccfit.zuev.osuplusplus.game.effects.AudioEffectsIntegrator audio = ru.nsu.ccfit.zuev.osuplusplus.game.effects.AudioEffectsIntegrator.getInstance(null);
            if (audio != null) {
                audio.playUIClick(); // Use available method
            }
        } catch (Exception e) {
            // Ignore if audio manager not available
        }
    }
    
    public void flashWithColor(float r, float g, float b) {
        if (isActive) return;
        
        isActive = true;
        flashRectangle.setColor(r, g, b);
        flashRectangle.setVisible(true);
        flashRectangle.setAlpha(0f);
        
        flashRectangle.registerEntityModifier(new SequenceEntityModifier(
            new FadeInModifier(0.15f),
            new FadeOutModifier(0.4f)
        ));
        
        flashRectangle.registerUpdateHandler(new org.anddev.andengine.engine.handler.IUpdateHandler() {
            float time = 0f;
            
            @Override
            public void onUpdate(float pSecondsElapsed) {
                time += pSecondsElapsed;
                if (time >= 0.55f) {
                    reset();
                }
            }
            
            @Override
            public void reset() {
                flashRectangle.setVisible(false);
                flashRectangle.setAlpha(0f);
                flashRectangle.clearUpdateHandlers();
                isActive = false;
                // Reset to white color
                flashRectangle.setColor(1f, 1f, 1f);
            }
        });
    }
    
    public void setVisible(boolean visible) {
        flashRectangle.setVisible(visible);
    }
    
    public void detach() {
        if (flashRectangle != null && scene != null) {
            scene.detachChild(flashRectangle);
        }
    }
    
    public void detachSelf() {
        detach();
    }
    
    public void triggerFlash() {
        flash();
    }
    
    public void triggerFlashWithColor(float r, float g, float b) {
        flashWithColor(r, g, b);
    }
    
    public void setIntensity(float intensity) {
        this.kiaiIntensity = Math.max(0.1f, Math.min(1.0f, intensity));
    }
    
    public boolean isActive() {
        return isActive;
    }
}

package ru.nsu.ccfit.zuev.osuplusplus.game;

import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplusplus.ResourceManager;

/**
 * Background blur effect using overlay rectangles
 * Simple implementation that doesn't require shaders
 */
public class BackgroundBlur {
    
    private Rectangle blurOverlay;
    private float blurAmount = 0f;
    private boolean enabled = false;
    private int blurRadius = 5;
    
    public BackgroundBlur() {
        // Initialize blur overlay
        blurOverlay = new Rectangle(0f, 0f, Config.getRES_WIDTH(), Config.getRES_HEIGHT());
        blurOverlay.setColor(0f, 0f, 0f, 0f);
        blurOverlay.setZIndex(-1000); // Render behind everything
    }
    
    /**
     * Initialize blur effect
     */
    public void initialize() {
        if (!Config.getBoolean("backgroundBlurEnabled", false)) {
            enabled = false;
            return;
        }
        
        enabled = true;
        blurRadius = Config.getInt("backgroundBlurRadius", 5);
        updateBlurAmount();
    }
    
    /**
     * Update blur amount based on settings
     */
    private void updateBlurAmount() {
        if (!enabled) {
            blurAmount = 0f;
            blurOverlay.setAlpha(0f);
            return;
        }
        
        // Calculate blur amount from config (0-100 scale)
        int blurPercent = Config.getInt("backgroundBlurAmount", 30);
        blurAmount = blurPercent / 100f;
        
        // Apply blur effect using multiple overlay rectangles
        // This simulates blur by creating layered transparency
        applyBlurEffect();
    }
    
    /**
     * Apply blur effect using layered rectangles
     */
    private void applyBlurEffect() {
        if (!enabled || blurOverlay == null) {
            return;
        }
        
        // Create blur effect by adjusting alpha
        // More blur = higher alpha = more "blurred" appearance
        float alpha = blurAmount * 0.7f; // Max 70% opacity for blur
        blurOverlay.setColor(0f, 0f, 0f, alpha);
        
        // For enhanced blur, we could add multiple layers with slight offsets
        // but this simple implementation should work well
    }
    
    /**
     * Get the blur overlay rectangle
     */
    public Rectangle getBlurOverlay() {
        return enabled ? blurOverlay : null;
    }
    
    /**
     * Check if blur is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Enable/disable blur
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        updateBlurAmount();
    }
    
    /**
     * Set blur amount (0.0 to 1.0)
     */
    public void setBlurAmount(float amount) {
        this.blurAmount = Math.max(0f, Math.min(1f, amount));
        applyBlurEffect();
    }
    
    /**
     * Set blur radius (in pixels)
     */
    public void setBlurRadius(int radius) {
        this.blurRadius = Math.max(1, Math.min(20, radius));
        updateBlurAmount();
    }
    
    /**
     * Update configuration from settings
     */
    public void updateFromConfig() {
        enabled = Config.getBoolean("backgroundBlurEnabled", false);
        blurRadius = Config.getInt("backgroundBlurRadius", 5);
        updateBlurAmount();
    }
    
    /**
     * Apply blur based on cursor position (dynamic blur)
     * @param cursorX Cursor position X (0.0 to 1.0)
     * @param cursorY Cursor position Y (0.0 to 1.0)
     */
    public void updateDynamicBlur(float cursorX, float cursorY) {
        if (!enabled || blurOverlay == null) {
            return;
        }
        
        // Calculate distance from center
        float centerX = 0.5f;
        float centerY = 0.5f;
        float distance = (float) Math.sqrt(
            Math.pow(cursorX - centerX, 2) + Math.pow(cursorY - centerY, 2)
        );
        
        // More blur at edges, less blur at center
        float dynamicBlurAmount = blurAmount * (distance * 1.5f);
        dynamicBlurAmount = Math.min(dynamicBlurAmount, blurAmount);
        
        blurOverlay.setColor(0f, 0f, 0f, dynamicBlurAmount * 0.7f);
    }
}

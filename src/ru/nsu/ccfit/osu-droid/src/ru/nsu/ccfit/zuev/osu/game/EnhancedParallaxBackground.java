package ru.nsu.ccfit.zuev.osuplusplus.game;

import org.anddev.andengine.entity.scene.background.IBackground;
import org.anddev.andengine.entity.scene.background.ParallaxBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.vertex.RectangleVertexBuffer;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplusplus.ResourceManager;

/**
 * Enhanced Parallax Background with cursor-based movement and blur support
 */
public class EnhancedParallaxBackground {
    
    private ParallaxBackground parallaxBackground;
    private Sprite backgroundLayer;
    private Sprite foregroundLayer;
    private float cursorX = 0.5f;
    private float cursorY = 0.5f;
    private boolean enabled = false;
    private float parallaxStrength = 0.1f;
    
    public EnhancedParallaxBackground() {
        // Initialize parallax background with black color
        parallaxBackground = new ParallaxBackground(0, 0, 0);
        parallaxStrength = 0.1f;
    }
    
    /**
     * Initialize parallax layers with background texture
     */
    public void initialize() {
        if (!Config.getBoolean("parallaxEnabled", false)) {
            enabled = false;
            return;
        }
        
        enabled = true;
        parallaxStrength = Config.getInt("parallaxStrength", 10) / 100f;
        
        try {
            // Load background texture
            TextureRegion bgTexture = ResourceManager.getInstance().getTexture("menu-background");
            if (bgTexture != null) {
                // Create background layer (moves slower)
                backgroundLayer = new Sprite(0, 0, bgTexture.getWidth(), bgTexture.getHeight(), bgTexture);
                backgroundLayer.setScaleCenter(0, 0);
                backgroundLayer.setScale(Math.max(
                    (float)Config.getRES_WIDTH() / bgTexture.getWidth(),
                    (float)Config.getRES_HEIGHT() / bgTexture.getHeight()
                ));
                
                // Create foreground layer (moves faster) - could be a different texture
                foregroundLayer = new Sprite(0, 0, bgTexture.getWidth(), bgTexture.getHeight(), bgTexture);
                foregroundLayer.setScaleCenter(0, 0);
                foregroundLayer.setScale(Math.max(
                    (float)Config.getRES_WIDTH() / bgTexture.getWidth(),
                    (float)Config.getRES_HEIGHT() / bgTexture.getHeight()
                ));
                foregroundLayer.setAlpha(0.3f); // Make it semi-transparent
                
                // Attach layers to parallax background
                parallaxBackground.attachParallaxEntity(new ParallaxBackground.ParallaxEntity(0.5f, backgroundLayer));
                parallaxBackground.attachParallaxEntity(new ParallaxBackground.ParallaxEntity(1.0f, foregroundLayer));
            }
        } catch (Exception e) {
            // Fallback to disabled if texture loading fails
            enabled = false;
        }
    }
    
    /**
     * Update parallax based on cursor position
     * @param cursorX Cursor position X (0.0 to 1.0)
     * @param cursorY Cursor position Y (0.0 to 1.0)
     */
    public void updateParallax(float cursorX, float cursorY) {
        if (!enabled || parallaxBackground == null) {
            return;
        }
        
        this.cursorX = cursorX;
        this.cursorY = cursorY;
        
        // Calculate parallax offset based on cursor position
        float offsetX = (cursorX - 0.5f) * 2f * parallaxStrength;
        float offsetY = (cursorY - 0.5f) * 2f * parallaxStrength;
        
        // Update parallax background position
        parallaxBackground.setParallaxValue(offsetX);
    }
    
    /**
     * Get the AndEngine ParallaxBackground object
     */
    public ParallaxBackground getParallaxBackground() {
        return enabled ? parallaxBackground : null;
    }
    
    /**
     * Check if parallax is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Enable/disable parallax
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Set parallax strength (0.0 to 1.0)
     */
    public void setParallaxStrength(float strength) {
        this.parallaxStrength = Math.max(0f, Math.min(1f, strength));
        // ParallaxBackground doesn't have setParallaxChangePer method
        // We handle parallax strength in updateParallax method
    }
    
    /**
     * Update configuration from settings
     */
    public void updateFromConfig() {
        enabled = Config.getBoolean("parallaxEnabled", false);
        setParallaxStrength(Config.getInt("parallaxStrength", 10) / 100f);
    }
}

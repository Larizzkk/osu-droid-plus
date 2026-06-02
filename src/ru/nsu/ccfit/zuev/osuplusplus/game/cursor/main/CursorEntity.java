package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.main;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplusplus.ResourceManager;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;
import ru.nsu.ccfit.zuev.osuplusplus.game.cursor.trail.CursorTrail;
import ru.nsu.ccfit.zuev.osuplusplus.game.cursor.trail.CursorTrailOptimized;
import ru.nsu.ccfit.zuev.osuplusplus.game.cursor.trail.CursorTrailAdvanced;
import ru.nsu.ccfit.zuev.osuplusplus.GlobalManager;

public class CursorEntity extends Entity {
    protected final CursorSprite cursorSprite;
    protected Object trail = null; // Can be CursorTrail, CursorTrailOptimized, or CursorTrailAdvanced
    private PointParticleEmitter emitter = null;
    private boolean isShowing = false;
    private float particleOffsetX, particleOffsetY;
    protected int trailImplementation = 1; // Default to optimized trail

    // Trail delay: only show trail when cursor is held >1s
    private float showTimer = 0f;
    private boolean trailEnabled = false;

    // Glow effect sprites
    private Sprite[] glowSprites = null;
    private Entity glowContainer = null;
    private boolean glowEnabled = false;

    public CursorEntity() {
        TextureRegion cursorTex = ResourceManager.getInstance().getTexture("cursor");
        cursorSprite = new CursorSprite(-cursorTex.getWidth() / 2f, -cursorTex.getWidth() / 2f, cursorTex);

        // Load trail implementation from config
        loadTrailImplementation();

        if (Config.isUseParticles()) {
            TextureRegion trailTex = ResourceManager.getInstance().getTexture("cursortrail");

            particleOffsetX = -trailTex.getWidth() / 2f;
            particleOffsetY = -trailTex.getHeight() / 2f;

            // Create trail based on implementation
            createTrail(trailTex);
        }

        attachChild(cursorSprite);

        // Glow effects disabled for cursor
        // initializeGlow();

        setVisible(false);

        // Not necessary to update by itself since it's done by GameScene.
        setIgnoreUpdate(true);
    }

    /**
     * Initialize glow effect sprites
     */
    private void initializeGlow() {
        glowEnabled = Config.isCursorGlowEnabled();

        // Debug logging
        android.util.Log.d("CursorEntity", "Initializing glow - enabled: " + glowEnabled);

        if (glowEnabled) {
            try {
                TextureRegion cursorTex = ResourceManager.getInstance().getTexture("cursor");
                float intensity = Config.getGlowIntensity();

                android.util.Log.d("CursorEntity", "Glow intensity: " + intensity);
                android.util.Log.d("CursorEntity", "Cursor texture: " + (cursorTex != null ? "found" : "null"));

                // Create glow container
                glowContainer = new Entity();

                // Create multiple glow layers for better effect
                glowSprites = new Sprite[3];
                float[] glowScales = {2.0f, 3.0f, 4.0f}; // Increased scales for visibility
                float[] glowAlphas = {0.4f, 0.2f, 0.1f}; // Increased alphas for visibility

                for (int i = 0; i < glowSprites.length; i++) {
                    glowSprites[i] = new Sprite(0, 0, cursorTex);
                    float cursorScale = cursorSprite != null ? cursorSprite.getScaleX() : 1.0f;
                    glowSprites[i].setScale(cursorScale * glowScales[i]);
                    glowSprites[i].setAlpha(glowAlphas[i] * intensity);
                    glowSprites[i].setVisible(true); // Ensure visible

                    // Set additive blending for glow effect
                    if (Config.isAdditiveBlendingEnabled()) {
                        glowSprites[i].setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
                        android.util.Log.d("CursorEntity", "Glow sprite " + i + " using additive blending");
                    } else {
                        glowSprites[i].setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
                        android.util.Log.d("CursorEntity", "Glow sprite " + i + " using normal blending");
                    }

                    glowContainer.attachChild(glowSprites[i]);
                    android.util.Log.d("CursorEntity", "Attached glow sprite " + i + " with scale: " + (cursorScale * glowScales[i]) + ", alpha: " + (glowAlphas[i] * intensity));
                }

                // Attach glow container behind cursor
                glowContainer.attachChild(cursorSprite);
                if (cursorSprite != null) {
                    detachChild(cursorSprite);
                }
                attachChild(glowContainer);

                android.util.Log.d("CursorEntity", "Glow initialization completed successfully");

            } catch (Exception e) {
                // Fail silently if glow can't be initialized
                android.util.Log.e("CursorEntity", "Failed to initialize glow", e);
                glowEnabled = false;
                glowSprites = null;
                glowContainer = null;
            }
        }
    }

    private void loadTrailImplementation() {
        try {
            trailImplementation = Config.getInt("trailImplementation", 1); // Default to optimized
        } catch (Exception e) {
            trailImplementation = 1;
        }
    }

    private void createTrail(TextureRegion trailTex) {
        switch (trailImplementation) {
            case 0: // Legacy particle system
                int spawnRate = (int) (GlobalManager.getInstance().getMainActivity().getRefreshRate() * 2);
                emitter = new PointParticleEmitter(particleOffsetX, particleOffsetY);
                trail = new CursorTrail(emitter, spawnRate, trailTex, cursorSprite);
                ((CursorTrail) trail).setParticlesSpawnEnabled(false);
                break;

            case 1: // Optimized trail
                trail = new CursorTrailOptimized(trailTex, cursorSprite);
                break;

            case 2: // Advanced trail
                trail = new CursorTrailAdvanced(trailTex, cursorSprite);
                break;

            default:
                // Fallback to optimized trail
                trail = new CursorTrailOptimized(trailTex, cursorSprite);
                break;
        }
    }

    /**
     * Set trail implementation (0=legacy, 1=optimized, 2=advanced)
     * Note: This requires recreating the trail
     */
    public void setTrailImplementation(int implementation) {
        if (trailImplementation != implementation) {
            trailImplementation = Math.max(0, Math.min(2, implementation));
            Config.setString("trailImplementation", String.valueOf(trailImplementation));

            // Recreate trail if particles are enabled
            if (Config.isUseParticles()) {
                TextureRegion trailTex = ResourceManager.getInstance().getTexture("cursortrail");
                createTrail(trailTex);
            }
        }
    }

    /**
     * Get current trail implementation
     */
    public int getTrailImplementation() {
        return trailImplementation;
    }

    /**
     * Get trail statistics for debugging
     */
    public String getTrailStats() {
        if (trail == null) {
            return "No trail active";
        }

        switch (trailImplementation) {
            case 0:
                return "Legacy particle trail active";
            case 1:
                CursorTrailOptimized optTrail = (CursorTrailOptimized) trail;
                return String.format("Optimized trail: %d points, length=%.2fs",
                    optTrail.getActivePointCount(), optTrail.getTrailLength());
            case 2:
                CursorTrailAdvanced advTrail = (CursorTrailAdvanced) trail;
                return advTrail.getStats().toString();
            default:
                return "Unknown trail implementation";
        }
    }

    public void setShowing(boolean showing) {
        if (!showing) {
            // Cursor hidden: reset timer and disable trail
            showTimer = 0f;
            trailEnabled = false;
            if (trail != null) {
                switch (trailImplementation) {
                    case 0:
                        ((CursorTrail) trail).setParticlesSpawnEnabled(false);
                        break;
                    case 1:
                        ((CursorTrailOptimized) trail).reset();
                        break;
                    case 2:
                        ((CursorTrailAdvanced) trail).reset();
                        break;
                }
            }
        } else if (!isShowing) {
            // Only reset timer on transition from hidden to shown
            trailEnabled = false;
            showTimer = 0f;
        }
        isShowing = showing;
        setVisible(showing);
    }

    public void click() {
        cursorSprite.handleClick();
    }

    public void update(float pSecondsElapsed) {
        if (isShowing) {
            cursorSprite.update(pSecondsElapsed);

            // Track how long cursor has been showing
            showTimer += pSecondsElapsed;
            if (showTimer > 1.0f && !trailEnabled) {
                // Enable trail after cursor has been held for >1 second
                trailEnabled = true;
                if (trail != null) {
                    switch (trailImplementation) {
                        case 0:
                            ((CursorTrail) trail).setParticlesSpawnEnabled(true);
                            break;
                        case 1:
                            ((CursorTrailOptimized) trail).reset();
                            break;
                        case 2:
                            ((CursorTrailAdvanced) trail).reset();
                            break;
                    }
                }
            }

            // Update glow settings
            updateGlowSettings();

            // Update glow sprites position
            if (glowEnabled && glowSprites != null) {
                float cursorX = getX();
                float cursorY = getY();
                for (Sprite glowSprite : glowSprites) {
                    glowSprite.setPosition(cursorX, cursorY);
                }
            }
        } else {
            showTimer = 0f;
        }

        // Update trail position when enabled
        if (trailEnabled && trail != null) {
            switch (trailImplementation) {
                case 1: // Optimized trail
                    float x1 = getX();
                    float y1 = getY();
                    ((CursorTrailOptimized) trail).updatePosition(x1, y1);
                    break;
                case 2: // Advanced trail
                    float x2 = getX();
                    float y2 = getY();
                    ((CursorTrailAdvanced) trail).updatePosition(x2, y2);
                    break;
            }
        }

        super.onManagedUpdate(pSecondsElapsed);
    }

    public void attachToScene(Scene fgScene) {
        if (trail != null) {
            fgScene.attachChild((Entity) trail);
        }
        fgScene.attachChild(this);
    }

    @Override
    public void setPosition(float pX, float pY) {
        if (emitter != null && trailImplementation == 0) {
            emitter.setCenter(pX + particleOffsetX, pY + particleOffsetY);
        }

        super.setPosition(pX, pY);
    }

    /**
     * Update trail length from configuration
     */
    public void updateTrailLength() {
        if (trail != null) {
            switch (trailImplementation) {
                case 0: // Legacy particle system
                    ((CursorTrail) trail).updateTrailLength();
                    break;
                case 1: // Optimized trail
                    ((CursorTrailOptimized) trail).updateTrailLength();
                    break;
                case 2: // Advanced trail
                    ((CursorTrailAdvanced) trail).updateTrailLength();
                    break;
            }
        }
    }

    /**
     * Update glow settings from configuration
     */
    public void updateGlowSettings() {
        boolean newGlowEnabled = Config.isCursorGlowEnabled();
        float newIntensity = Config.getGlowIntensity();

        // Recreate glow if enabled/disabled changed
        if (newGlowEnabled != glowEnabled) {
            if (glowContainer != null) {
                detachChild(glowContainer);
                glowContainer = null;
                glowSprites = null;
            }
            glowEnabled = newGlowEnabled;

            if (glowEnabled) {
                initializeGlow();
            }
        } else if (glowEnabled && glowSprites != null) {
            // Update intensity and blending mode
            for (int i = 0; i < glowSprites.length; i++) {
                float[] glowAlphas = {0.3f, 0.15f, 0.05f};
                glowSprites[i].setAlpha(glowAlphas[i] * newIntensity);

                // Update blending mode
                if (Config.isAdditiveBlendingEnabled()) {
                    glowSprites[i].setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
                } else {
                    glowSprites[i].setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
                }
            }
        }
    }

    /**
     * Get advanced trail instance for configuration (only works with advanced trail)
     */
    public CursorTrailAdvanced getAdvancedTrail() {
        if (trailImplementation == 2 && trail != null) {
            return (CursorTrailAdvanced) trail;
        }
        return null;
    }
}

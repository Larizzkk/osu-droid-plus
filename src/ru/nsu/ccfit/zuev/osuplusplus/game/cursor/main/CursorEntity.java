package ru.nsu.ccfit.zuev.osuplusplus.game.cursor.main;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplusplus.ResourceManager;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;
import ru.nsu.ccfit.zuev.osuplusplus.game.cursor.trail.CursorTrail;
import ru.nsu.ccfit.zuev.osuplusplus.game.cursor.trail.CursorTrailOptimized;
import ru.nsu.ccfit.zuev.osuplusplus.GlobalManager;

public class CursorEntity extends Entity {
    protected CursorSprite cursorSprite;
    protected Object trail = null; // CursorTrail (legacy) or CursorTrailOptimized (long)
    private PointParticleEmitter emitter = null;
    private boolean isShowing = false;
    private float particleOffsetX, particleOffsetY;
    protected int trailImplementation = 1; // Default to long (optimized) trail

    // Trail delay: only show trail when cursor is held >1s
    private float showTimer = 0f;
    private boolean trailEnabled = false;
    // Flag to prevent double trail update in AutoCursor mode
    private boolean trailUpdatedThisFrame = false;
    // When true, trail is enabled immediately regardless of delay (for replay playback)
    private boolean forceTrailEnabled = false;

    public CursorEntity() {
        TextureRegion cursorTex = ResourceManager.getInstance().getTexture("cursor");
        // Center the cursor sprite on the entity position (negative half extents).
        // Both halves MUST come from their own dimension — using width for Y offset the
        // cursor vertically on non-square cursor textures.
        cursorSprite = new CursorSprite(
            -cursorTex.getWidth() / 2f,
            -cursorTex.getHeight() / 2f,
            cursorTex
        );

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
        setVisible(false);

        // Not necessary to update by itself since it's done by GameScene.
        setIgnoreUpdate(true);
    }

    private void loadTrailImplementation() {
        try {
            trailImplementation = Integer.parseInt(Config.getString("trailImplementation", "1"));
        } catch (Exception e) {
            trailImplementation = 1;
        }
        // Clamp to valid range: 0=legacy, 1=long (optimized)
        trailImplementation = Math.max(0, Math.min(1, trailImplementation));
    }

    private void createTrail(TextureRegion trailTex) {
        switch (trailImplementation) {
            case 0: // Legacy particle system (exact upstream osu-droid behavior)
                int spawnRate = (int) (GlobalManager.getInstance().getMainActivity().getRefreshRate() * 2);
                emitter = new PointParticleEmitter(particleOffsetX, particleOffsetY);
                trail = new CursorTrail(emitter, spawnRate, trailTex, cursorSprite);
                ((CursorTrail) trail).setParticlesSpawnEnabled(false);
                break;

            case 1: // Long trail (optimized)
            default:
                trail = new CursorTrailOptimized(trailTex, cursorSprite);
                break;
        }
    }

    /**
     * Set trail implementation (0=legacy, 1=long/optimized)
     * Note: This requires recreating the trail
     */
    public void setTrailImplementation(int implementation) {
        implementation = Math.max(0, Math.min(1, implementation));
        if (trailImplementation != implementation) {
            trailImplementation = implementation;
            Config.setString("trailImplementation", String.valueOf(trailImplementation));

            // Recreate trail if particles are enabled
            if (Config.isUseParticles()) {
                TextureRegion trailTex = ResourceManager.getInstance().getTexture("cursortrail");
                if (trail instanceof Entity) {
                    ((Entity) trail).detachSelf();
                }
                createTrail(trailTex);
                if (getParent() != null && trail instanceof Entity) {
                    Scene parent = (Scene) getParent();
                    parent.attachChild((Entity) trail);
                    parent.attachChild(this);
                }
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
     * When true, trail bypasses the delay timer and enables immediately.
     * Used during replay playback where frequent UP/DOWN transitions would
     * otherwise reset the delay timer and prevent the trail from ever showing.
     */
    public void setForceTrailEnabled(boolean force) {
        forceTrailEnabled = force;
        if (force && isShowing && !trailEnabled && trail != null) {
            trailEnabled = true;
            if (trailImplementation == 0) {
                ((CursorTrail) trail).setParticlesSpawnEnabled(true);
            }
        }
    }

    /**
     * Reset trail state. Used during replay to clear old position data.
     */
    public void resetTrail() {
        if (trail != null) {
            switch (trailImplementation) {
                case 0: ((CursorTrail) trail).reset(); break;
                case 1: ((CursorTrailOptimized) trail).reset(); break;
            }
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
                        ((CursorTrail) trail).reset();
                        ((CursorTrail) trail).setParticlesSpawnEnabled(false);
                        break;
                    case 1:
                        // Do NOT erase the ribbon: mark the input discontinuity so the next
                        // position update starts a fresh ribbon instead of interpolating from
                        // the lift point, and let the remaining points age out naturally via
                        // updateRemovalOnly() while hidden (danser-go behavior).
                        ((CursorTrailOptimized) trail).markDiscontinuity();
                        break;
                }
            }
        } else if (!isShowing) {
            // Cursor re-appearing (new tap somewhere else)
            showTimer = 0f;
            if (trailImplementation == 0) {
                // Legacy particles spawn immediately upon cursor show (no trail delay).
                trailEnabled = true;
                if (trail != null) {
                    ((CursorTrail) trail).setParticlesSpawnEnabled(true);
                }
            } else {
                if (!forceTrailEnabled) {
                    trailEnabled = false;
                }
                if (trail != null) {
                    ((CursorTrailOptimized) trail).markDiscontinuity();
                }
            }
        }
        isShowing = showing;
        setVisible(showing);
    }

    /**
     * Called by the input paths on every fresh ACTION_DOWN. Guarantees the trail never
     * interpolates across a re-press even if no hidden frame was rendered between the
     * lift and the new press (thread timing), and regardless of tap distance.
     */
    public void onCursorPress() {
        if (trail != null && trailImplementation == 1) {
            ((CursorTrailOptimized) trail).markDiscontinuity();
        }
    }

    public void click() {
        cursorSprite.handleClick();
    }

    public void update(float pSecondsElapsed) {
        if (isShowing) {
            cursorSprite.update(pSecondsElapsed);

            // Track how long cursor has been showing
            showTimer += pSecondsElapsed;

            if (trailImplementation == 0) {
                trailEnabled = true;
                if (trail != null) {
                    ((CursorTrail) trail).setParticlesSpawnEnabled(true);
                    ((CursorTrail) trail).update();
                }
            } else if (forceTrailEnabled || !Config.getBoolean("trailDelayEnabled", true)) {
                if (!trailEnabled) {
                    trailEnabled = true;
                    if (trail != null) {
                        ((CursorTrailOptimized) trail).reset();
                    }
                }
            } else if (showTimer > 1.0f && !trailEnabled) {
                // Trail delay elapsed: enable the long trail for real gameplay.
                trailEnabled = true;
                if (trail != null) {
                    ((CursorTrailOptimized) trail).reset();
                }
            }
        } else {
            showTimer = 0f;
        }

        // Update trail position if not already updated by updateTrailFromMovement().
        // Skip while hidden: in replay mode (forceTrailEnabled) the trail stays "enabled"
        // through ACTION_UP, and feeding it the stale cursor position would keep drawing
        // segments from the lift point.
        if (!trailUpdatedThisFrame && isShowing && trailEnabled && trail != null) {
            float tx = getX();
            float ty = getY();
            switch (trailImplementation) {
                case 0:
                    // Legacy particles spawn through the emitter; the ParticleSystem
                    // updates itself as a child of the scene.
                    break;
                case 1: ((CursorTrailOptimized) trail).updatePosition(tx, ty, pSecondsElapsed); break;
            }
        } else if (!isShowing && trailImplementation == 1 && trail != null) {
            // Hidden cursor: keep aging the remaining points out (danser-go removal runs
            // every frame from the point count), so the trail disappears after the finger
            // is lifted even though no positions are being fed.
            ((CursorTrailOptimized) trail).updateRemovalOnly(pSecondsElapsed);
        }
        trailUpdatedThisFrame = false;

        super.onManagedUpdate(pSecondsElapsed);
    }

    public void attachToScene(Scene fgScene) {
        // Attach order = render order: the trail goes first, the cursor sprite on
        // top of it, so the ribbon never covers the cursor itself.
        if (trail != null) {
            if (trail instanceof Entity && ((Entity) trail).hasParent()) {
                ((Entity) trail).detachSelf();
            }
            fgScene.attachChild((Entity) trail);
        }
        if (hasParent()) {
            detachSelf();
        }
        fgScene.attachChild(this);

        // One-shot trail/parent diagnostics: proves at runtime whether the trail and the
        // cursor share the same parent space (menus draw on the engine overlay HUD).
        android.util.Log.d(
            "CursorCoord",
            "attachToScene: scene=" + fgScene.getClass().getSimpleName() +
            " trailParent=" + (trail instanceof Entity && ((Entity) trail).getParent() != null
                ? ((Entity) trail).getParent().getClass().getSimpleName()
                : "none")
        );
    }

    @Override
    public void setPosition(float pX, float pY) {
        if (emitter != null && trailImplementation == 0) {
            emitter.setCenter(pX + particleOffsetX, pY + particleOffsetY);
        }

        super.setPosition(pX, pY);
    }

    /**
     * Update trail with current cursor position.
     * Called AFTER setPosition() in updateMovement() so the trail always
     * reads the CURRENT frame's position, not the previous frame's.
     * This eliminates the 1-frame lag that caused trail detachment.
     */
    public void updateTrailFromMovement(float deltaTimeSeconds) {
        if (!trailEnabled || trail == null) return;
        float x = getX();
        float y = getY();
        switch (trailImplementation) {
            case 1:
                ((CursorTrailOptimized) trail).updatePosition(x, y, deltaTimeSeconds);
                break;
        }
        trailUpdatedThisFrame = true;
    }

    public void updateTrailLength() {
        if (trail != null) {
            switch (trailImplementation) {
                case 0:
                    // Legacy trail is fixed-length (upstream behavior); nothing to update.
                    break;
                case 1: // Long trail
                    ((CursorTrailOptimized) trail).updateTrailLength();
                    break;
            }
        }
    }

    /**
     * Clean up trail resources. Must be called when the cursor entity is removed.
     */
    /**
     * Re-pulls skin textures after a mid-game skin switch (replay visual panel).
     * AndEngine Sprite has no texture setter, so the cursor sprite is rebuilt in
     * place: transform state (position/scale/visibility) is preserved manually.
     */
    public void refreshSkinTextures() {
        TextureRegion cursorTex = ResourceManager.getInstance().getTexture("cursor");
        if (cursorTex == null) {
            return;
        }

        float x = getX(), y = getY();
        boolean visible = isVisible();
        float scale = cursorSprite.getScaleX();

        detachChild(cursorSprite);
        cursorSprite.onDetached();

        cursorSprite = new CursorSprite(
            -cursorTex.getWidth() / 2f,
            -cursorTex.getHeight() / 2f,
            cursorTex
        );
        attachChild(cursorSprite);

        setPosition(x, y);
        cursorSprite.setScale(scale);
        setVisible(visible);

        if (trail != null) {
            TextureRegion newTrailTex = ResourceManager.getInstance().getTexture("cursortrail");
            if (newTrailTex != null) {
                if (trail instanceof CursorTrailOptimized) {
                    ((CursorTrailOptimized) trail).refreshTexture(newTrailTex, cursorSprite);
                    ((CursorTrailOptimized) trail).markDiscontinuity();
                    ((CursorTrailOptimized) trail).syncToPosition(x, y);
                } else {
                    recreateTrail(newTrailTex);
                }
            }
        }
    }

    /**
     * Fully recreates the trail after a skin hot-swap: CursorTrailOptimized bakes UVs
     * and blend state from the region at construction and legacy CursorTrail keeps
     * live particles bound to the old region, so a plain rebind can show the previous
     * skin's texture. Trail state (enabled/delay, force flag for replays) is preserved;
     * point history is not — the ribbon restarts from the cursor's current position.
     */
    private void recreateTrail(TextureRegion newTrailTex) {
        if (newTrailTex == null) {
            return;
        }

        // Preserve trail runtime state.
        boolean wasTrailEnabled = trailEnabled;
        boolean wasForceEnabled = forceTrailEnabled;

        // Detach the old trail entity from the scene if it was attached there.
        if (trail instanceof Entity) {
            ((Entity) trail).detachSelf();
        }

        // Recreate from the freshly loaded region (same path as the constructor).
        createTrail(newTrailTex);

        // Restore spawn state for the legacy particle trail and re-anchor it: a fresh
        // emitter starts at (0,0) with the OLD texture's offsets, so a skin switch would
        // streak the trail from the screen origin (and with a shifted half-size) until the
        // next setPosition().
        if (trailImplementation == 0 && trail != null) {
            particleOffsetX = -newTrailTex.getWidth() / 2f;
            particleOffsetY = -newTrailTex.getHeight() / 2f;

            if (emitter != null) {
                emitter.setCenter(getX() + particleOffsetX, getY() + particleOffsetY);
            }
            ((CursorTrail) trail).setParticlesSpawnEnabled(wasTrailEnabled);
        }

        // Re-attach to the scene so the new trail renders BEHIND the cursor sprite.
        if (getParent() != null && trail != null) {
            Scene parent = (Scene) getParent();
            detachSelf();
            parent.attachChild((Entity) trail);
            parent.attachChild(this);
        }

        // The new trail entity starts empty: mark the discontinuity so the next
        // updateTrailFromMovement seeds it at the current cursor position instead of
        // interpolating from the constructor default (0,0).
        if (trailImplementation == 1 && trail != null) {
            ((CursorTrailOptimized) trail).markDiscontinuity();
            ((CursorTrailOptimized) trail).syncToPosition(getX(), getY());
        }

        trailEnabled = wasTrailEnabled && trail != null;
        forceTrailEnabled = wasForceEnabled;
    }

    public void cleanupTrail() {
        if (trail != null) {
            switch (trailImplementation) {
                case 1:
                    ((CursorTrailOptimized) trail).cleanup();
                    break;
                case 0:
                    ((CursorTrail) trail).reset();
                    break;
            }
            trail = null;
        }
    }
}

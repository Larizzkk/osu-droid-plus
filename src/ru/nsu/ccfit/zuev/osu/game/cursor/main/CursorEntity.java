package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import com.reco1l.andengine.component.UIComponent;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.game.cursor.trail.CursorTrail;
import ru.nsu.ccfit.zuev.osu.game.cursor.trail.CursorTrailClassic;
import ru.nsu.ccfit.zuev.osu.game.cursor.trail.CursorTrailOptimized;

public class CursorEntity extends UIComponent {
    protected CursorSprite cursorSprite;
    protected Object trail = null; // CursorTrail (legacy) or CursorTrailOptimized (long)
    private boolean isShowing = false;
    protected int trailImplementation = 1; // Default to long (optimized) trail

    // Original osu!droid particle trail (style 2).
    private PointParticleEmitter classicEmitter = null;
    private float classicOffsetX = 0f;
    private float classicOffsetY = 0f;

    // Trail delay: only show trail when cursor is held >1s
    private float showTimer = 0f;
    private boolean trailEnabled = false;
    // Flag to prevent double trail update in AutoCursor mode
    private boolean trailUpdatedThisFrame = false;
    // When true, trail is enabled immediately regardless of delay (for replay playback)
    private boolean forceTrailEnabled = false;

    /** Skin generation the sprite/trail textures were built from. */
    private int skinVersion;

    /**
     * Throttled settings refresh: Config.get*() hits SharedPreferences, so the
     * trail keys must not be read from update() every frame. Persistent cursors
     * (the menu cursor) rely on this to pick up settings-screen changes live.
     */
    private static final float SETTINGS_REFRESH_INTERVAL = 0.5f;
    private float settingsRefreshTimer = 0f;
    private boolean trailDelayEnabled = true;

    public CursorEntity() {
        skinVersion = ResourceManager.getInstance().getSkinVersion();
        // The port's CursorSprite anchors itself to the centre and loads the
        // skin texture on its own.
        cursorSprite = new CursorSprite();

        // Load trail implementation from config
        loadTrailImplementation();
        trailDelayEnabled = Config.getBoolean("trailDelayEnabled", true);

        if (Config.isUseParticles()) {
            TextureRegion trailTex = ResourceManager.getInstance().getTexture("cursortrail");

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
        // Clamp to valid range: 0=legacy, 1=long (optimized), 2=osu!droid
        trailImplementation = Math.max(0, Math.min(2, trailImplementation));
    }

    private void createTrail(TextureRegion trailTex) {
        switch (trailImplementation) {
            case 0: // Legacy trail — stable's standard time-gated points (torn look)
                trail = new CursorTrail(trailTex, cursorSprite);
                ((CursorTrail) trail).setParticlesSpawnEnabled(false);
                break;

            case 1: // Long trail (optimized)
                trail = new CursorTrailOptimized(trailTex, cursorSprite);
                break;

            case 2: // Original osu!droid particle trail
                classicOffsetX = -trailTex.getWidth() / 2f;
                classicOffsetY = -trailTex.getHeight() / 2f;
                classicEmitter = new PointParticleEmitter(classicOffsetX, classicOffsetY);
                int spawnRate = (int) (GlobalManager.getInstance().getMainActivity().getRefreshRate() * 2);
                trail = new CursorTrailClassic(classicEmitter, spawnRate, trailTex, cursorSprite);
                break;

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
        implementation = Math.max(0, Math.min(2, implementation));
        if (trailImplementation != implementation) {
            trailImplementation = implementation;
            Config.setString("trailImplementation", String.valueOf(trailImplementation));

            // Recreate trail if particles are enabled
            if (Config.isUseParticles()) {
                TextureRegion trailTex = ResourceManager.getInstance().getTexture("cursortrail");
                Entity oldTrail = trail instanceof Entity ? (Entity) trail : null;

                if (oldTrail != null) {
                    oldTrail.detachSelf();
                    if (oldTrail instanceof CursorTrailOptimized) {
                        // Releases the old sprite batch (its texture is final) before the
                        // object is dropped; the legacy trail has no native resources.
                        ((CursorTrailOptimized) oldTrail).cleanup();
                    }
                }

                createTrail(trailTex);

                // The trail must render below the cursor, and attachChild() appends.
                // Re-attach the cursor after the new trail, but detach it first: a
                // second attach without detaching throws "pEntity already has a parent!".
                if (getParent() instanceof Scene && trail instanceof Entity) {
                    Scene parent = (Scene) getParent();
                    if (hasParent()) {
                        detachSelf();
                    }
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
     * Re-reads the "trailImplementation" preference and rebuilds the trail in
     * place when it differs from the one captured at construction. Needed by
     * persistent cursors (e.g. the menu cursor) that outlive a settings change.
     * No-op when the preference is unchanged. CursorEntity.update() already calls
     * it on a 0.5s throttle, so callers normally do not need to poll it.
     */
    public void refreshTrailImplementation() {
        int requested;
        try {
            requested = Integer.parseInt(Config.getString("trailImplementation", "1"));
        } catch (Exception e) {
            requested = 1;
        }
        // Clamp to valid range: 0=legacy, 1=long (optimized), 2=osu!droid
        requested = Math.max(0, Math.min(2, requested));

        if (requested != trailImplementation) {
            setTrailImplementation(requested);
        }
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
            } else if (trailImplementation == 2) {
                ((CursorTrailClassic) trail).setParticlesSpawnEnabled(true);
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
                case 2: ((CursorTrailClassic) trail).reset(); break;
            }
        }
    }

    public boolean isShowing() {
        return isShowing;
    }

    public void setShowing(boolean showing) {
        if (!showing) {
            // Cursor hidden: reset timer and disable trail
            showTimer = 0f;
            trailEnabled = false;
            if (trail != null) {
                switch (trailImplementation) {
                    case 0:
                        // Original osu! behavior: existing points are NOT erased when the
                        // cursor hides — spawning stops and the remaining points age out on
                        // their own (stable just returns early, no wipe). The spawn gate
                        // timestamp persists, so resuming continues after ≤16.67ms.
                        ((CursorTrail) trail).setParticlesSpawnEnabled(false);
                        break;
                    case 1:
                        // Do NOT erase the ribbon: mark the input discontinuity so the next
                        // position update starts a fresh ribbon instead of interpolating from
                        // the lift point, and let the remaining points age out naturally via
                        // updateRemovalOnly() while hidden (danser-go behavior).
                        ((CursorTrailOptimized) trail).markDiscontinuity();
                        break;
                    case 2:
                        ((CursorTrailClassic) trail).setParticlesSpawnEnabled(false);
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
            } else if (trailImplementation == 2) {
                // Original osu!droid trail also needs no delay.
                trailEnabled = true;
                if (trail != null) {
                    ((CursorTrailClassic) trail).setParticlesSpawnEnabled(true);
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
     * Called by the input paths on every fresh ACTION_DOWN. Guarantees the long
     * trail never interpolates across a re-press even if no hidden frame was
     * rendered between the lift and the new press (thread timing), and regardless
     * of tap distance. The legacy trail needs no discontinuity marker: its stable
     * spawn is time-gated at the CURRENT position and never interpolates.
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
        // Re-pull the skin textures when the player switches skins.
        int currentSkinVersion = ResourceManager.getInstance().getSkinVersion();
        if (currentSkinVersion != skinVersion) {
            skinVersion = currentSkinVersion;
            refreshSkinTextures();
        }

        // Pick up cursor/trail setting changes without a reload. Throttled: this is
        // the only place persistent cursors get their settings refreshed from.
        settingsRefreshTimer += pSecondsElapsed;
        if (settingsRefreshTimer >= SETTINGS_REFRESH_INTERVAL) {
            settingsRefreshTimer = 0f;
            refreshTrailImplementation();
            updateTrailLength();
            trailDelayEnabled = Config.getBoolean("trailDelayEnabled", true);
        }

        if (isShowing) {
            cursorSprite.update(pSecondsElapsed);

            // Track how long cursor has been showing
            showTimer += pSecondsElapsed;

            if (trailImplementation == 0) {
                trailEnabled = true;
                if (trail != null) {
                    ((CursorTrail) trail).setParticlesSpawnEnabled(true);
                    // Stable standard branch: feed the cursor position once per frame;
                    // the trail emits a point every ~16.67ms at the current position
                    // (no segment interpolation → visibly torn at speed).
                    ((CursorTrail) trail).feedPosition(getX(), getY());
                }
            } else if (trailImplementation == 2) {
                trailEnabled = true;
                if (trail != null) {
                    ((CursorTrailClassic) trail).setParticlesSpawnEnabled(true);
                    ((CursorTrailClassic) trail).update();
                }
            } else if (forceTrailEnabled || !trailDelayEnabled) {
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
                    // Legacy trail is fed from the block above (feedPosition).
                    break;
                case 1: ((CursorTrailOptimized) trail).updatePosition(tx, ty, pSecondsElapsed); break;
                case 2:
                    // Particle emitter follows the cursor via setPosition().
                    break;
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
                case 2:
                    // The classic particle trail has a fixed lifetime.
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
        skinVersion = ResourceManager.getInstance().getSkinVersion();

        float x = getX(), y = getY();
        boolean visible = isVisible();
        float scale = cursorSprite.getScaleX();

        detachChild(cursorSprite);
        cursorSprite.onDetached();

        cursorSprite = new CursorSprite();
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
                } else if (trail instanceof CursorTrailClassic) {
                    // The particle system's texture is final; recreate it in place.
                    ((CursorTrailClassic) trail).setParticlesSpawnEnabled(false);
                    ((CursorTrailClassic) trail).detachSelf();
                    createTrail(newTrailTex);
                    if (getParent() instanceof Scene && trail instanceof Entity) {
                        ((Scene) getParent()).attachChild((Entity) trail);
                    }
                    setPosition(x, y);
                } else {
                    // Legacy trail rebinds in place too: live points keep fading with
                    // the new texture instead of being wiped by a full recreate, and the
                    // rebuilt cursor sprite reference is handed over.
                    ((CursorTrail) trail).refreshTexture(newTrailTex, cursorSprite);
                }
            }
        }
    }

    @Override
    public void setPosition(float pX, float pY) {
        if (classicEmitter != null) {
            classicEmitter.setCenter(pX + classicOffsetX, pY + classicOffsetY);
        }
        super.setPosition(pX, pY);
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
                case 2:
                    ((CursorTrailClassic) trail).reset();
                    break;
            }
            trail = null;
        }
    }
}

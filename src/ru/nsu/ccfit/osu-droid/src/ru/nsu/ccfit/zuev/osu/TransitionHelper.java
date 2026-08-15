package ru.nsu.ccfit.zuev.osuplusplus;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.Config;

public class TransitionHelper {

    /**
     * Fade out current scene, then switch to new scene.
     * Uses an update handler for timing since AndEngine modifiers don't have reliable completion callbacks.
     */
    public static void fadeOutThenSet(Engine engine, Scene currentScene, Scene newScene, float fadeOutDuration) {
        if (engine == null) return;
        if (currentScene == null || newScene == null) {
            engine.setScene(newScene);
            return;
        }

        Rectangle overlay = new Rectangle(0, 0, Config.getRES_WIDTH(), Config.getRES_HEIGHT());
        overlay.setColor(0, 0, 0);
        overlay.setAlpha(0);
        currentScene.attachChild(overlay);

        final float[] elapsed = {0};

        IUpdateHandler handler = new IUpdateHandler() {
            @Override
            public void onUpdate(float pSecondsElapsed) {
                elapsed[0] += pSecondsElapsed;
                float t = Math.min(1, elapsed[0] / fadeOutDuration);
                overlay.setAlpha(t);

                if (t >= 1) {
                    engine.setScene(newScene);
                    currentScene.unregisterUpdateHandler(this);
                }
            }

            @Override
            public void reset() {}
        };

        currentScene.registerUpdateHandler(handler);
    }

    /**
     * Fade transition with a quick white flash (osu! style).
     * Current scene fades to white, then new scene appears from white.
     */
    public static void whiteFlashTransition(Engine engine, Scene currentScene, Scene newScene) {
        if (engine == null) return;
        if (currentScene == null || newScene == null) {
            engine.setScene(newScene);
            return;
        }

        // White overlay on current scene
        Rectangle overlay = new Rectangle(0, 0, Config.getRES_WIDTH(), Config.getRES_HEIGHT());
        overlay.setColor(1, 1, 1);
        overlay.setAlpha(0);
        currentScene.attachChild(overlay);

        final float[] elapsed = {0};
        final float FADE_OUT = 0.15f;
        final float FADE_IN = 0.15f;

        IUpdateHandler handler = new IUpdateHandler() {
            boolean switched = false;

            @Override
            public void onUpdate(float dt) {
                elapsed[0] += dt;

                if (!switched) {
                    // Fade to white
                    float t = Math.min(1, elapsed[0] / FADE_OUT);
                    overlay.setAlpha(t);

                    if (t >= 1) {
                        // Switch scenes with white overlay
                        Rectangle whiteOverlay = new Rectangle(0, 0, Config.getRES_WIDTH(), Config.getRES_HEIGHT());
                        whiteOverlay.setColor(1, 1, 1);
                        whiteOverlay.setAlpha(1);
                        newScene.attachChild(whiteOverlay);

                        engine.setScene(newScene);
                        switched = true;
                        elapsed[0] = 0;
                    }
                } else {
                    // Fade from white
                    float t = Math.min(1, elapsed[0] / FADE_IN);
                    // Find the white overlay in the new scene
                    if (newScene.getChildCount() > 0) {
                        // Last attached child should be our white overlay
                        newScene.getLastChild().setAlpha(1 - t);
                    }

                    if (elapsed[0] >= FADE_IN) {
                        // Remove the overlay
                        if (newScene.getChildCount() > 0) {
                            newScene.detachChild(newScene.getLastChild());
                        }
                        currentScene.unregisterUpdateHandler(this);
                    }
                }
            }

            @Override
            public void reset() {}
        };

        currentScene.registerUpdateHandler(handler);
    }

    /**
     * Quick fade (0.3s total).
     */
    public static void fadeOutThenSet(Engine engine, Scene currentScene, Scene newScene) {
        fadeOutThenSet(engine, currentScene, newScene, 0.3f);
    }
}

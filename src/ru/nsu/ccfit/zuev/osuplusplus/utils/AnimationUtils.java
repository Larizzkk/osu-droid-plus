package ru.nsu.ccfit.zuev.osuplusplus.utils;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.*;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.util.modifier.ease.*;
import android.graphics.PointF;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Advanced animation utilities
 * Based on osu!lazer's animation systems with smooth transitions
 */
public class AnimationUtils {
    
    private static final Random random = new Random();
    
    // Easing functions for smooth animations
    public enum EasingType {
        LINEAR(EaseLinear.getInstance()),
        SINE_IN(EaseSineIn.getInstance()),
        SINE_OUT(EaseSineOut.getInstance()),
        SINE_IN_OUT(EaseSineInOut.getInstance()),
        QUAD_IN(EaseQuadIn.getInstance()),
        QUAD_OUT(EaseQuadOut.getInstance()),
        QUAD_IN_OUT(EaseQuadInOut.getInstance()),
        CUBIC_IN(EaseCubicIn.getInstance()),
        CUBIC_OUT(EaseCubicOut.getInstance()),
        CUBIC_IN_OUT(EaseCubicInOut.getInstance()),
        QUART_IN(EaseQuartIn.getInstance()),
        QUART_OUT(EaseQuartOut.getInstance()),
        QUART_IN_OUT(EaseQuartInOut.getInstance()),
        QUINT_IN(EaseQuintIn.getInstance()),
        QUINT_OUT(EaseQuintOut.getInstance()),
        QUINT_IN_OUT(EaseQuintInOut.getInstance()),
        // EXPO and CIRC easing not available in AndEngine
        BACK_IN(EaseBackIn.getInstance()),
        BACK_OUT(EaseBackOut.getInstance()),
        BACK_IN_OUT(EaseBackInOut.getInstance()),
        ELASTIC_IN(EaseElasticIn.getInstance()),
        ELASTIC_OUT(EaseElasticOut.getInstance()),
        ELASTIC_IN_OUT(EaseElasticInOut.getInstance()),
        BOUNCE_IN(EaseBounceIn.getInstance()),
        BOUNCE_OUT(EaseBounceOut.getInstance()),
        BOUNCE_IN_OUT(EaseBounceInOut.getInstance());
        
        public final IEaseFunction easeFunction;
        
        EasingType(IEaseFunction easeFunction) {
            this.easeFunction = easeFunction;
        }
    }
    
    // Animation presets
    public static class AnimationPreset {
        public static final float QUICK_DURATION = 0.15f;
        public static final float NORMAL_DURATION = 0.3f;
        public static final float SLOW_DURATION = 0.6f;
        public static final float VERY_SLOW_DURATION = 1.0f;
        
        // Common easing combinations
        public static final EasingType EASE_IN = EasingType.QUAD_IN;
        public static final EasingType EASE_OUT = EasingType.QUAD_OUT;
        public static final EasingType EASE_IN_OUT = EasingType.QUAD_IN_OUT;
        public static final EasingType BOUNCE = EasingType.BOUNCE_OUT;
        public static final EasingType ELASTIC = EasingType.ELASTIC_OUT;
        public static final EasingType SMOOTH = EasingType.SINE_IN_OUT;
    }
    
    /**
     * Create smooth fade animation
     */
    public static FadeInModifier createFadeIn(float duration, EasingType easing) {
        return new FadeInModifier(duration, easing.easeFunction);
    }
    
    public static FadeOutModifier createFadeOut(float duration, EasingType easing) {
        return new FadeOutModifier(duration, easing.easeFunction);
    }
    
    /**
     * Create scale animation
     */
    public static ScaleModifier createScale(float fromX, float fromY, float toX, float toY, 
                                           float duration, EasingType easing) {
        return new ScaleModifier(duration, fromX, fromY, toX, toY, easing.easeFunction);
    }
    
    /**
     * Create rotation animation
     */
    public static RotationModifier createRotation(float fromAngle, float toAngle, 
                                                float duration, EasingType easing) {
        return new RotationModifier(duration, fromAngle, toAngle, easing.easeFunction);
    }
    
    /**
     * Create move animation
     */
    public static MoveModifier createMove(float fromX, float fromY, float toX, float toY,
                                         float duration, EasingType easing) {
        return new MoveModifier(duration, fromX, fromY, toX, toY, easing.easeFunction);
    }
    
    /**
     * Create color animation
     */
    public static ColorModifier createColor(float fromR, float fromG, float fromB, float fromA,
                                          float toR, float toG, float toB, float toA,
                                          float duration, EasingType easing) {
        return new ColorModifier(duration, fromR, fromG, fromB, toR, toG, toB, easing.easeFunction);
    }
    
    /**
     * Create bounce animation
     */
    public static SequenceEntityModifier createBounce(IEntity entity, float height, float duration) {
        List<IEntityModifier> modifiers = new ArrayList<>();
        
        // Jump up
        modifiers.add(createMove(0, 0, 0, -height, duration * 0.4f, EasingType.QUAD_OUT));
        
        // Fall down
        modifiers.add(createMove(0, -height, 0, 0, duration * 0.3f, EasingType.QUAD_IN));
        
        // Small bounce
        modifiers.add(createMove(0, 0, 0, -height * 0.3f, duration * 0.1f, EasingType.QUAD_OUT));
        modifiers.add(createMove(0, -height * 0.3f, 0, 0, duration * 0.1f, EasingType.QUAD_IN));
        
        // Tiny bounce
        modifiers.add(createMove(0, 0, 0, -height * 0.1f, duration * 0.05f, EasingType.QUAD_OUT));
        modifiers.add(createMove(0, -height * 0.1f, 0, 0, duration * 0.05f, EasingType.QUAD_IN));
        
        return new SequenceEntityModifier(modifiers.toArray(new IEntityModifier[0]));
    }
    
    /**
     * Create elastic scale animation
     */
    public static SequenceEntityModifier createElasticScale(IEntity entity, float scaleX, float scaleY, float duration) {
        List<IEntityModifier> modifiers = new ArrayList<>();
        
        // Scale up beyond target
        modifiers.add(createScale(1, 1, scaleX * 1.2f, scaleY * 1.2f, duration * 0.4f, EasingType.ELASTIC_OUT));
        
        // Scale back to target
        modifiers.add(createScale(scaleX * 1.2f, scaleY * 1.2f, scaleX, scaleY, duration * 0.3f, EasingType.BOUNCE_OUT));
        
        // Final settle
        modifiers.add(createScale(scaleX, scaleY, scaleX * 0.95f, scaleY * 0.95f, duration * 0.15f, EasingType.SINE_IN_OUT));
        modifiers.add(createScale(scaleX * 0.95f, scaleY * 0.95f, scaleX, scaleY, duration * 0.15f, EasingType.SINE_IN_OUT));
        
        return new SequenceEntityModifier(modifiers.toArray(new IEntityModifier[0]));
    }
    
    /**
     * Create shake animation
     */
    public static SequenceEntityModifier createShake(IEntity entity, float intensity, float duration) {
        List<IEntityModifier> modifiers = new ArrayList<>();
        int shakes = (int) (duration / 0.05f); // Shake every 50ms
        
        for (int i = 0; i < shakes; i++) {
            float offsetX = (random.nextFloat() - 0.5f) * intensity * 2;
            float offsetY = (random.nextFloat() - 0.5f) * intensity * 2;
            
            modifiers.add(createMove(0, 0, offsetX, offsetY, 0.025f, EasingType.LINEAR));
            modifiers.add(createMove(offsetX, offsetY, -offsetX, -offsetY, 0.025f, EasingType.LINEAR));
        }
        
        // Return to original position
        modifiers.add(createMove(0, 0, 0, 0, 0.05f, EasingType.SINE_OUT));
        
        return new SequenceEntityModifier(modifiers.toArray(new IEntityModifier[0]));
    }
    
    /**
     * Create pulse animation
     */
    public static LoopEntityModifier createPulse(IEntity entity, float scaleAmount, float duration) {
        ScaleModifier scaleUp = createScale(1, 1, 1 + scaleAmount, 1 + scaleAmount, duration / 2, EasingType.SINE_OUT);
        ScaleModifier scaleDown = createScale(1 + scaleAmount, 1 + scaleAmount, 1, 1, duration / 2, EasingType.SINE_IN);
        
        return new LoopEntityModifier(new SequenceEntityModifier(scaleUp, scaleDown));
    }
    
    /**
     * Create float animation
     */
    public static LoopEntityModifier createFloat(IEntity entity, float floatHeight, float duration) {
        MoveModifier moveUp = createMove(0, 0, 0, -floatHeight, duration / 2, EasingType.SINE_IN_OUT);
        MoveModifier moveDown = createMove(0, -floatHeight, 0, 0, duration / 2, EasingType.SINE_IN_OUT);
        
        return new LoopEntityModifier(new SequenceEntityModifier(moveUp, moveDown));
    }
    
    /**
     * Create glow pulse animation
     */
    public static LoopEntityModifier createGlowPulse(IEntity entity, float maxAlpha, float duration) {
        FadeInModifier fadeIn = createFadeIn(duration / 2, EasingType.SINE_IN_OUT);
        FadeOutModifier fadeOut = createFadeOut(duration / 2, EasingType.SINE_IN_OUT);
        
        return new LoopEntityModifier(new SequenceEntityModifier(fadeIn, fadeOut));
    }
    
    /**
     * Create rotation spin animation
     */
    public static LoopEntityModifier createSpin(IEntity entity, float degreesPerSecond) {
        float duration = 360f / Math.abs(degreesPerSecond);
        RotationModifier rotate = createRotation(0, 360, duration, EasingType.LINEAR);
        
        return new LoopEntityModifier(rotate);
    }
    
    /**
     * Create entrance animation
     */
    public static SequenceEntityModifier createEntrance(IEntity entity, EntranceType type) {
        switch (type) {
            case FADE_IN:
                return new SequenceEntityModifier(createFadeIn(AnimationPreset.NORMAL_DURATION, EasingType.SINE_OUT));
                
            case SCALE_IN:
                return new SequenceEntityModifier(
                    createScale(0, 0, 1.1f, 1.1f, AnimationPreset.NORMAL_DURATION * 0.6f, EasingType.BACK_OUT),
                    createScale(1.1f, 1.1f, 1, 1, AnimationPreset.NORMAL_DURATION * 0.4f, EasingType.BOUNCE_OUT)
                );
                
            case SLIDE_IN_LEFT:
                return new SequenceEntityModifier(
                    createMove(-100, 0, 0, 0, AnimationPreset.NORMAL_DURATION, EasingType.BACK_OUT)
                );
                
            case SLIDE_IN_RIGHT:
                return new SequenceEntityModifier(
                    createMove(100, 0, 0, 0, AnimationPreset.NORMAL_DURATION, EasingType.BACK_OUT)
                );
                
            case SLIDE_IN_TOP:
                return new SequenceEntityModifier(
                    createMove(0, -100, 0, 0, AnimationPreset.NORMAL_DURATION, EasingType.BACK_OUT)
                );
                
            case SLIDE_IN_BOTTOM:
                return new SequenceEntityModifier(
                    createMove(0, 100, 0, 0, AnimationPreset.NORMAL_DURATION, EasingType.BACK_OUT)
                );
                
            case ELASTIC_SCALE:
                return createElasticScale(entity, 1, 1, AnimationPreset.NORMAL_DURATION);
                
            case BOUNCE_IN:
                return createBounce(entity, 100, AnimationPreset.NORMAL_DURATION);
                
            default:
                return new SequenceEntityModifier(createFadeIn(AnimationPreset.NORMAL_DURATION, EasingType.SINE_OUT));
        }
    }
    
    /**
     * Create exit animation
     */
    public static SequenceEntityModifier createExit(IEntity entity, ExitType type) {
        switch (type) {
            case FADE_OUT:
                return new SequenceEntityModifier(createFadeOut(AnimationPreset.NORMAL_DURATION, EasingType.SINE_IN));
                
            case SCALE_OUT:
                return new SequenceEntityModifier(
                    createScale(1, 1, 0, 0, AnimationPreset.NORMAL_DURATION, EasingType.BACK_IN)
                );
                
            case SLIDE_OUT_LEFT:
                return new SequenceEntityModifier(
                    createMove(0, 0, -100, 0, AnimationPreset.NORMAL_DURATION, EasingType.BACK_IN)
                );
                
            case SLIDE_OUT_RIGHT:
                return new SequenceEntityModifier(
                    createMove(0, 0, 100, 0, AnimationPreset.NORMAL_DURATION, EasingType.BACK_IN)
                );
                
            case SLIDE_OUT_TOP:
                return new SequenceEntityModifier(
                    createMove(0, 0, 0, -100, AnimationPreset.NORMAL_DURATION, EasingType.BACK_IN)
                );
                
            case SLIDE_OUT_BOTTOM:
                return new SequenceEntityModifier(
                    createMove(0, 0, 0, 100, AnimationPreset.NORMAL_DURATION, EasingType.BACK_IN)
                );
                
            case SHRINK_FADE:
                return new SequenceEntityModifier(
                    new ParallelEntityModifier(
                        createScale(1, 1, 0.5f, 0.5f, AnimationPreset.NORMAL_DURATION * 0.5f, EasingType.QUAD_IN),
                        createFadeOut(AnimationPreset.NORMAL_DURATION, EasingType.SINE_IN)
                    )
                );
                
            default:
                return new SequenceEntityModifier(createFadeOut(AnimationPreset.NORMAL_DURATION, EasingType.SINE_IN));
        }
    }
    
    /**
     * Create attention animation
     */
    public static LoopEntityModifier createAttention(IEntity entity, AttentionType type) {
        switch (type) {
            case PULSE:
                return createPulse(entity, 0.1f, 1.0f);
                
            case SHAKE:
                return new LoopEntityModifier(createShake(entity, 5f, 0.5f));
                
            case GLOW:
                return createGlowPulse(entity, 0.5f, 1.5f);
                
            case FLOAT:
                return createFloat(entity, 10f, 2.0f);
                
            case SPIN:
                return createSpin(entity, 180f); // Half rotation per second
                
            case WOBBLE:
                return createWobble(entity, 0.1f, 1.0f);
                
            default:
                return createPulse(entity, 0.1f, 1.0f);
        }
    }
    
    /**
     * Create wobble animation
     */
    private static LoopEntityModifier createWobble(IEntity entity, float intensity, float duration) {
        List<IEntityModifier> modifiers = new ArrayList<>();
        
        // Wobble X
        modifiers.add(createScale(1, 1, 1 + intensity, 1, duration * 0.25f, EasingType.SINE_IN_OUT));
        modifiers.add(createScale(1 + intensity, 1, 1 - intensity, 1, duration * 0.25f, EasingType.SINE_IN_OUT));
        modifiers.add(createScale(1 - intensity, 1, 1 + intensity, 1, duration * 0.25f, EasingType.SINE_IN_OUT));
        modifiers.add(createScale(1 + intensity, 1, 1, 1, duration * 0.25f, EasingType.SINE_IN_OUT));
        
        return new LoopEntityModifier(new SequenceEntityModifier(modifiers.toArray(new IEntityModifier[0])));
    }
    
    /**
     * Create path animation
     */
    public static MoveModifier createPathAnimation(PointF[] path, float duration, EasingType easing) {
        if (path == null || path.length < 2) {
            return null;
        }
        
        // Simplified path animation - just move to first point
        return createMove(path[0].x, path[0].y, path[path.length-1].x, path[path.length-1].y, duration, easing);
    }
    
    /**
     * Create delayed animation
     */
    public static DelayModifier createDelay(float delay) {
        return new DelayModifier(delay);
    }
    
    /**
     * Create parallel animation
     */
    public static ParallelEntityModifier createParallel(IEntityModifier... modifiers) {
        return new ParallelEntityModifier(modifiers);
    }
    
    /**
     * Create complex entrance with sound
     */
    public static void createEntranceWithSound(IEntity entity, EntranceType type, String soundName) {
        // Play sound
        if (soundName != null) {
            try {
                ru.nsu.ccfit.zuev.osuplusplus.game.effects.AudioEffectsIntegrator audio = ru.nsu.ccfit.zuev.osuplusplus.game.effects.AudioEffectsIntegrator.getInstance(null);
                if (audio != null) {
                    audio.playUIClick();
                }
            } catch (Exception e) {
                // Ignore if audio not available
            }
        }
        
        // Apply animation
        entity.registerEntityModifier(createEntrance(entity, type));
    }
    
    /**
     * Create complex exit with sound
     */
    public static void createExitWithSound(IEntity entity, ExitType type, String soundName) {
        // Play sound
        if (soundName != null) {
            try {
                ru.nsu.ccfit.zuev.osuplusplus.game.effects.AudioEffectsIntegrator audio = ru.nsu.ccfit.zuev.osuplusplus.game.effects.AudioEffectsIntegrator.getInstance(null);
                if (audio != null) {
                    audio.playUIBack();
                }
            } catch (Exception e) {
                // Ignore if audio not available
            }
        }
        
        // Apply animation
        entity.registerEntityModifier(createExit(entity, type));
    }
    
    /**
     * Animate multiple entities in sequence
     */
    public static void animateSequence(IEntity[] entities, float delayBetween, 
                                      IEntityModifier animation) {
        for (int i = 0; i < entities.length; i++) {
            SequenceEntityModifier sequence = new SequenceEntityModifier(
                createDelay(i * delayBetween),
                animation
            );
            entities[i].registerEntityModifier(sequence);
        }
    }
    
    /**
     * Animate multiple entities with stagger
     */
    public static void animateStaggered(IEntity[] entities, float staggerDelay, 
                                       IEntityModifier animation, boolean reverse) {
        int start = reverse ? entities.length - 1 : 0;
        int end = reverse ? -1 : entities.length;
        int step = reverse ? -1 : 1;
        
        for (int i = start; i != end; i += step) {
            SequenceEntityModifier sequence = new SequenceEntityModifier(
                createDelay(Math.abs(i - start) * staggerDelay),
                animation
            );
            entities[i].registerEntityModifier(sequence);
        }
    }
    
    /**
     * Create smooth color transition
     */
    public static void smoothColorTransition(IEntity entity, float[] fromColor, float[] toColor, 
                                           float duration, EasingType easing) {
        ColorModifier colorMod = createColor(
            fromColor[0], fromColor[1], fromColor[2], fromColor[3],
            toColor[0], toColor[1], toColor[2], toColor[3],
            duration, easing
        );
        entity.registerEntityModifier(colorMod);
    }
    
    /**
     * Entrance types
     */
    public enum EntranceType {
        FADE_IN, SCALE_IN, SLIDE_IN_LEFT, SLIDE_IN_RIGHT, 
        SLIDE_IN_TOP, SLIDE_IN_BOTTOM, ELASTIC_SCALE, BOUNCE_IN
    }
    
    /**
     * Exit types
     */
    public enum ExitType {
        FADE_OUT, SCALE_OUT, SLIDE_OUT_LEFT, SLIDE_OUT_RIGHT,
        SLIDE_OUT_TOP, SLIDE_OUT_BOTTOM, SHRINK_FADE
    }
    
    /**
     * Attention types
     */
    public enum AttentionType {
        PULSE, SHAKE, GLOW, FLOAT, SPIN, WOBBLE
    }
    
    /**
     * Utility methods for common animations
     */
    public static void quickFadeIn(IEntity entity) {
        entity.registerEntityModifier(createFadeIn(AnimationPreset.QUICK_DURATION, EasingType.SINE_OUT));
    }
    
    public static void quickFadeOut(IEntity entity) {
        entity.registerEntityModifier(createFadeOut(AnimationPreset.QUICK_DURATION, EasingType.SINE_IN));
    }
    
    public static void bounceIn(IEntity entity) {
        entity.registerEntityModifier(createBounce(entity, 50, AnimationPreset.NORMAL_DURATION));
    }
    
    public static void elasticScale(IEntity entity) {
        entity.registerEntityModifier(createElasticScale(entity, 1.2f, 1.2f, AnimationPreset.NORMAL_DURATION));
    }
    
    public static void shake(IEntity entity) {
        entity.registerEntityModifier(createShake(entity, 3f, 0.3f));
    }
    
    public static void pulse(IEntity entity) {
        entity.registerEntityModifier(createPulse(entity, 0.05f, 1.0f));
    }
}

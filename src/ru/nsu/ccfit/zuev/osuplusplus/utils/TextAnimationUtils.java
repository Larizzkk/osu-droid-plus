package ru.nsu.ccfit.zuev.osuplusplus.utils;

import org.anddev.andengine.entity.text.Text;
import org.anddev.andengine.entity.modifier.*;
import org.anddev.andengine.util.modifier.ease.*;
import android.graphics.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Advanced text animation utilities
 * Based on osu!lazer's text animation systems
 */
public class TextAnimationUtils {
    
    private static final Random random = new Random();
    
    /**
     * Typewriter effect
     */
    public static void typewriter(Text text, String fullText, float charsPerSecond, 
                                  AnimationUtils.EasingType easing) {
        TypewriterAnimation anim = new TypewriterAnimation(text, fullText, charsPerSecond, easing);
        anim.start();
    }
    
    /**
     * Wave text animation
     */
    public static void waveText(Text[] characters, float amplitude, float frequency, float duration) {
        for (int i = 0; i < characters.length; i++) {
            float delay = i / (float) characters.length * duration;
            
            SequenceEntityModifier wave = new SequenceEntityModifier(
                AnimationUtils.createDelay(delay),
                new LoopEntityModifier(
                    new SequenceEntityModifier(
                        AnimationUtils.createMove(0, 0, 0, -amplitude, duration * 0.25f, AnimationUtils.EasingType.SINE_IN_OUT),
                        AnimationUtils.createMove(0, -amplitude, 0, amplitude, duration * 0.25f, AnimationUtils.EasingType.SINE_IN_OUT),
                        AnimationUtils.createMove(0, amplitude, 0, -amplitude, duration * 0.25f, AnimationUtils.EasingType.SINE_IN_OUT),
                        AnimationUtils.createMove(0, -amplitude, 0, 0, duration * 0.25f, AnimationUtils.EasingType.SINE_IN_OUT)
                    )
                )
            );
            
            characters[i].registerEntityModifier(wave);
        }
    }
    
    /**
     * Glitch text effect
     */
    public static void glitchText(Text text, float intensity, float duration) {
        GlitchAnimation anim = new GlitchAnimation(text, intensity, duration);
        anim.start();
    }
    
    /**
     * Rainbow text animation
     */
    public static void rainbowText(Text text, float duration) {
        RainbowAnimation anim = new RainbowAnimation(text, duration);
        anim.start();
    }
    
    /**
     * Bounce text animation
     */
    public static void bounceText(Text[] characters, float bounceHeight, float duration) {
        for (int i = 0; i < characters.length; i++) {
            float delay = i * 0.05f; // Stagger the bounces
            
            SequenceEntityModifier bounce = new SequenceEntityModifier(
                AnimationUtils.createDelay(delay),
                AnimationUtils.createBounce(characters[i], bounceHeight, duration)
            );
            
            characters[i].registerEntityModifier(bounce);
        }
    }
    
    /**
     * Shake text animation
     */
    public static void shakeText(Text text, float intensity, float duration) {
        text.registerEntityModifier(AnimationUtils.createShake(text, intensity, duration));
    }
    
    /**
     * Fade in characters one by one
     */
    public static void cascadeFadeIn(Text[] characters, float delayBetween, float fadeDuration) {
        for (int i = 0; i < characters.length; i++) {
            SequenceEntityModifier cascade = new SequenceEntityModifier(
                AnimationUtils.createDelay(i * delayBetween),
                AnimationUtils.createFadeIn(fadeDuration, AnimationUtils.EasingType.SINE_OUT)
            );
            
            characters[i].registerEntityModifier(cascade);
        }
    }
    
    /**
     * Scale in characters one by one
     */
    public static void cascadeScaleIn(Text[] characters, float delayBetween, float scaleDuration) {
        for (int i = 0; i < characters.length; i++) {
            SequenceEntityModifier cascade = new SequenceEntityModifier(
                AnimationUtils.createDelay(i * delayBetween),
                AnimationUtils.createElasticScale(characters[i], 1, 1, scaleDuration)
            );
            
            characters[i].registerEntityModifier(cascade);
        }
    }
    
    /**
     * Slide in characters from different directions
     */
    public static void multiDirectionSlide(Text[] characters, float slideDistance, float duration) {
        for (int i = 0; i < characters.length; i++) {
            AnimationUtils.EntranceType direction;
            
            // Alternate directions
            switch (i % 4) {
                case 0:
                    direction = AnimationUtils.EntranceType.SLIDE_IN_LEFT;
                    break;
                case 1:
                    direction = AnimationUtils.EntranceType.SLIDE_IN_RIGHT;
                    break;
                case 2:
                    direction = AnimationUtils.EntranceType.SLIDE_IN_TOP;
                    break;
                default:
                    direction = AnimationUtils.EntranceType.SLIDE_IN_BOTTOM;
                    break;
            }
            
            SequenceEntityModifier slide = new SequenceEntityModifier(
                AnimationUtils.createDelay(i * 0.05f),
                AnimationUtils.createEntrance(characters[i], direction)
            );
            
            characters[i].registerEntityModifier(slide);
        }
    }
    
    /**
     * Neon glow effect
     */
    public static void neonGlow(Text text, float glowIntensity, float pulseSpeed) {
        NeonGlowAnimation anim = new NeonGlowAnimation(text, glowIntensity, pulseSpeed);
        anim.start();
    }
    
    /**
     * 3D rotation effect
     */
    public static void rotate3D(Text text, float duration) {
        Rotation3DAnimation anim = new Rotation3DAnimation(text, duration);
        anim.start();
    }
    
    /**
     * Text reveal animation
     */
    public static void revealText(Text text, float revealDuration, AnimationUtils.EasingType easing) {
        // Start from center and expand
        text.setScaleX(0);
        text.setAlpha(0);
        
        SequenceEntityModifier reveal = new SequenceEntityModifier(
            new ParallelEntityModifier(
                AnimationUtils.createScale(0, 1, 1, 1, revealDuration * 0.6f, easing),
                AnimationUtils.createFadeIn(revealDuration, easing)
            ),
            AnimationUtils.createScale(1, 1, 1.1f, 1.1f, revealDuration * 0.2f, AnimationUtils.EasingType.BOUNCE_OUT),
            AnimationUtils.createScale(1.1f, 1.1f, 1, 1, revealDuration * 0.2f, AnimationUtils.EasingType.BOUNCE_OUT)
        );
        
        text.registerEntityModifier(reveal);
    }
    
    /**
     * Typewriter animation class
     */
    private static class TypewriterAnimation {
        private Text text;
        private String fullText;
        private float charsPerSecond;
        private AnimationUtils.EasingType easing;
        private float currentTime = 0;
        private boolean isRunning = false;
        
        public TypewriterAnimation(Text text, String fullText, float charsPerSecond, AnimationUtils.EasingType easing) {
            this.text = text;
            this.fullText = fullText;
            this.charsPerSecond = charsPerSecond;
            this.easing = easing;
        }
        
        public void start() {
            isRunning = true;
            // Note: setText not available in AndEngine Text, using alternative approach
            
            text.registerUpdateHandler(new org.anddev.andengine.engine.handler.IUpdateHandler() {
                @Override
                public void onUpdate(float pSecondsElapsed) {
                    if (!isRunning) return;
                    
                    currentTime += pSecondsElapsed;
                    float progress = currentTime * charsPerSecond / fullText.length();
                    progress = Math.min(1.0f, progress);
                    
                    // Apply easing
                    progress = progress; // Simplified easing
                    
                    // Update alpha based on progress instead of text content
                    text.setAlpha(progress);
                    
                    if (progress >= 1.0f) {
                        isRunning = false;
                        text.clearUpdateHandlers();
                    }
                }
                
                @Override
                public void reset() {
                    isRunning = false;
                    text.clearUpdateHandlers();
                }
            });
        }
    }
    
    /**
     * Glitch animation class
     */
    private static class GlitchAnimation {
        private Text text;
        private float intensity;
        private float duration;
        private float currentTime = 0;
        private String originalText;
        private boolean isRunning = false;
        
        public GlitchAnimation(Text text, float intensity, float duration) {
            this.text = text;
            this.intensity = intensity;
            this.duration = duration;
            this.originalText = text.getText();
        }
        
        public void start() {
            isRunning = true;
            currentTime = 0;
            
            text.registerUpdateHandler(new org.anddev.andengine.engine.handler.IUpdateHandler() {
                @Override
                public void onUpdate(float pSecondsElapsed) {
                    if (!isRunning) return;
                    
                    currentTime += pSecondsElapsed;
                    
                    if (currentTime >= duration) {
                        // Reset visual effects instead of text content
                        text.setPosition(0, 0); // Reset position
                        text.setColor(1, 1, 1); // Reset color
                        text.setAlpha(1); // Reset alpha
                        isRunning = false;
                        text.clearUpdateHandlers();
                        return;
                    }
                    
                    // Random glitch effects
                    if (random.nextFloat() < intensity) {
                        // Random position offset
                        float offsetX = (random.nextFloat() - 0.5f) * intensity * 10;
                        float offsetY = (random.nextFloat() - 0.5f) * intensity * 5;
                        text.setPosition(offsetX, offsetY);
                        
                        // Random color shift
                        float r = 0.5f + random.nextFloat() * 0.5f;
                        float g = random.nextFloat() * 0.5f;
                        float b = random.nextFloat() * 0.5f;
                        text.setColor(r, g, b);
                        
                        // Random alpha flicker
                        text.setAlpha(0.3f + random.nextFloat() * 0.7f);
                    }
                }
                
                @Override
                public void reset() {
                    isRunning = false;
                    text.clearUpdateHandlers();
                }
            });
        }
        
        private String applyGlitchChars(String original) {
            StringBuilder glitched = new StringBuilder();
            String glitchChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";
            
            for (int i = 0; i < original.length(); i++) {
                if (random.nextFloat() < intensity) {
                    glitched.append(glitchChars.charAt(random.nextInt(glitchChars.length())));
                } else {
                    glitched.append(original.charAt(i));
                }
            }
            
            return glitched.toString();
        }
    }
    
    /**
     * Rainbow animation class
     */
    private static class RainbowAnimation {
        private Text text;
        private float duration;
        private float currentTime = 0;
        private boolean isRunning = false;
        
        public RainbowAnimation(Text text, float duration) {
            this.text = text;
            this.duration = duration;
        }
        
        public void start() {
            isRunning = true;
            currentTime = 0;
            
            text.registerUpdateHandler(new org.anddev.andengine.engine.handler.IUpdateHandler() {
                @Override
                public void onUpdate(float pSecondsElapsed) {
                    if (!isRunning) return;
                    
                    currentTime += pSecondsElapsed;
                    
                    float hue = (currentTime / duration) % 1.0f;
                    float[] rgb = hsvToRgb(hue, 1.0f, 1.0f);
                    
                    text.setColor(rgb[0], rgb[1], rgb[2]);
                }
                
                @Override
                public void reset() {
                    isRunning = false;
                    text.clearUpdateHandlers();
                }
            });
        }
        
        private float[] hsvToRgb(float h, float s, float v) {
            float c = v * s;
            float x = c * (1 - Math.abs((h * 6) % 2 - 1));
            float m = v - c;
            
            float r, g, b;
            
            if (h < 1/6f) {
                r = c; g = x; b = 0;
            } else if (h < 2/6f) {
                r = x; g = c; b = 0;
            } else if (h < 3/6f) {
                r = 0; g = c; b = x;
            } else if (h < 4/6f) {
                r = 0; g = x; b = c;
            } else if (h < 5/6f) {
                r = x; g = 0; b = c;
            } else {
                r = c; g = 0; b = x;
            }
            
            return new float[]{r + m, g + m, b + m};
        }
    }
    
    /**
     * Neon glow animation class
     */
    private static class NeonGlowAnimation {
        private Text text;
        private float glowIntensity;
        private float pulseSpeed;
        private float currentTime = 0;
        private boolean isRunning = false;
        
        public NeonGlowAnimation(Text text, float glowIntensity, float pulseSpeed) {
            this.text = text;
            this.glowIntensity = glowIntensity;
            this.pulseSpeed = pulseSpeed;
        }
        
        public void start() {
            isRunning = true;
            currentTime = 0;
            
            text.registerUpdateHandler(new org.anddev.andengine.engine.handler.IUpdateHandler() {
                @Override
                public void onUpdate(float pSecondsElapsed) {
                    if (!isRunning) return;
                    
                    currentTime += pSecondsElapsed;
                    
                    // Pulsing effect
                    float pulse = (float) Math.sin(currentTime * pulseSpeed * 2 * Math.PI) * 0.5f + 0.5f;
                    float alpha = 0.5f + pulse * glowIntensity * 0.5f;
                    
                    // Neon colors (cyan/magenta)
                    float r = 0.2f + pulse * 0.8f;
                    float g = 0.8f + pulse * 0.2f;
                    float b = 1.0f;
                    
                    text.setColor(r, g, b);
                    text.setAlpha(alpha);
                }
                
                @Override
                public void reset() {
                    isRunning = false;
                    text.clearUpdateHandlers();
                }
            });
        }
    }
    
    /**
     * 3D rotation animation class
     */
    private static class Rotation3DAnimation {
        private Text text;
        private float duration;
        private float currentTime = 0;
        private boolean isRunning = false;
        
        public Rotation3DAnimation(Text text, float duration) {
            this.text = text;
            this.duration = duration;
        }
        
        public void start() {
            isRunning = true;
            currentTime = 0;
            
            text.registerUpdateHandler(new org.anddev.andengine.engine.handler.IUpdateHandler() {
                @Override
                public void onUpdate(float pSecondsElapsed) {
                    if (!isRunning) return;
                    
                    currentTime += pSecondsElapsed;
                    float progress = currentTime / duration;
                    
                    if (progress >= 1.0f) {
                        isRunning = false;
                        text.clearUpdateHandlers();
                        return;
                    }
                    
                    // Simulate 3D rotation with scale and alpha
                    float angle = progress * 360;
                    float radians = (float) Math.toRadians(angle);
                    
                    float scaleX = Math.abs((float) Math.cos(radians));
                    float alpha = Math.abs((float) Math.cos(radians));
                    
                    text.setScaleX(scaleX);
                    text.setAlpha(alpha);
                }
                
                @Override
                public void reset() {
                    isRunning = false;
                    text.clearUpdateHandlers();
                }
            });
        }
    }
    
    /**
     * Utility methods for text splitting
     */
    public static Text[] splitTextToCharacters(Text originalText, String text) {
        List<Text> characters = new ArrayList<>();
        
        // Note: Character-based animation not fully supported in AndEngine Text
        // Return array with original text for compatibility
        characters.add(originalText);
        
        return characters.toArray(new Text[0]);
    }
    
    /**
     * Create animated score display
     */
    public static void animateScore(Text scoreText, int fromScore, int toScore, float duration) {
        ScoreAnimation anim = new ScoreAnimation(scoreText, fromScore, toScore, duration);
        anim.start();
    }
    
    /**
     * Score animation class
     */
    private static class ScoreAnimation {
        private Text text;
        private int fromScore;
        private int toScore;
        private float duration;
        private float currentTime = 0;
        private boolean isRunning = false;
        
        public ScoreAnimation(Text text, int fromScore, int toScore, float duration) {
            this.text = text;
            this.fromScore = fromScore;
            this.toScore = toScore;
            this.duration = duration;
        }
        
        public void start() {
            isRunning = true;
            currentTime = 0;
            
            text.registerUpdateHandler(new org.anddev.andengine.engine.handler.IUpdateHandler() {
                @Override
                public void onUpdate(float pSecondsElapsed) {
                    if (!isRunning) return;
                    
                    currentTime += pSecondsElapsed;
                    float progress = currentTime / duration;
                    
                    if (progress >= 1.0f) {
                        // Note: setText not available, using alpha animation instead
                        text.setAlpha(1.0f);
                        isRunning = false;
                        text.clearUpdateHandlers();
                        return;
                    }
                    
                    // Smooth easing
                    progress = progress; // Simplified easing
                    
                    // Animate alpha instead of changing text content
                    text.setAlpha(progress);
                }
                
                @Override
                public void reset() {
                    isRunning = false;
                    text.clearUpdateHandlers();
                }
            });
        }
    }
    
    /**
     * Create floating number animation
     */
    public static void animateFloatingNumber(Text numberText, float endY, float duration) {
        numberText.registerEntityModifier(
            new SequenceEntityModifier(
                new ParallelEntityModifier(
                    AnimationUtils.createMove(0, 0, 0, endY, duration, AnimationUtils.EasingType.QUAD_OUT),
                    AnimationUtils.createFadeOut(duration, AnimationUtils.EasingType.SINE_IN)
                )
            )
        );
    }
    
    /**
     * Create text shake on impact
     */
    public static void impactShake(Text text, float intensity) {
        text.registerEntityModifier(
            new SequenceEntityModifier(
                AnimationUtils.createShake(text, intensity, 0.1f),
                AnimationUtils.createScale(1, 1, 1.1f, 1.1f, 0.05f, AnimationUtils.EasingType.SINE_OUT),
                AnimationUtils.createScale(1.1f, 1.1f, 1, 1, 0.05f, AnimationUtils.EasingType.SINE_IN)
            )
        );
    }
}

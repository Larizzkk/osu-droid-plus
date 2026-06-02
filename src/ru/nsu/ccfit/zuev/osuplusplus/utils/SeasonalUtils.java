package ru.nsu.ccfit.zuev.osuplusplus.utils;

import android.graphics.Color;
import java.util.Calendar;
import java.util.Random;

/**
 * Seasonal event utilities
 * Based on osu!lazer's seasonal systems
 */
public class SeasonalUtils {
    
    // Seasonal event types
    public enum SeasonalEvent {
        NONE("None", 0, 0),
        CHRISTMAS("Christmas", 12, 25),
        NEW_YEAR("New Year", 1, 1),
        HALLOWEEN("Halloween", 10, 31),
        VALENTINE("Valentine's Day", 2, 14),
        EASTER("Easter", -1, -1), // Variable date
        SPRING("Spring", 3, 20),
        SUMMER("Summer", 6, 21),
        AUTUMN("Autumn", 9, 22),
        WINTER("Winter", 12, 21);
        
        private final String name;
        private final int month;
        private final int day;
        
        SeasonalEvent(String name, int month, int day) {
            this.name = name;
            this.month = month;
            this.day = day;
        }
        
        public String getName() { return name; }
        public int getMonth() { return month; }
        public int getDay() { return day; }
    }
    
    // Seasonal colors
    public static class SeasonalColors {
        // Christmas colors
        public static final int CHRISTMAS_RED = Color.parseColor("#D32F2F");
        public static final int CHRISTMAS_GREEN = Color.parseColor("#388E3C");
        public static final int CHRISTMAS_GOLD = Color.parseColor("#FFD700");
        public static final int CHRISTMAS_WHITE = Color.parseColor("#FFFFFF");
        
        // Halloween colors
        public static final int HALLOWEEN_ORANGE = Color.parseColor("#FF8C00");
        public static final int HALLOWEEN_PURPLE = Color.parseColor("#8B008B");
        public static final int HALLOWEEN_BLACK = Color.parseColor("#000000");
        
        // Valentine's Day colors
        public static final int VALENTINE_PINK = Color.parseColor("#FF69B4");
        public static final int VALENTINE_RED = Color.parseColor("#FF1493");
        public static final int VALENTINE_WHITE = Color.parseColor("#FFFFFF");
        
        // Spring colors
        public static final int SPRING_PINK = Color.parseColor("#FFB6C1");
        public static final int SPRING_GREEN = Color.parseColor("#90EE90");
        public static final int SPRING_YELLOW = Color.parseColor("#FFFFE0");
        
        // Summer colors
        public static final int SUMMER_BLUE = Color.parseColor("#87CEEB");
        public static final int SUMMER_YELLOW = Color.parseColor("#FFD700");
        public static final int SUMMER_GREEN = Color.parseColor("#32CD32");
        
        // Autumn colors
        public static final int AUTUMN_ORANGE = Color.parseColor("#FF8C00");
        public static final int AUTUMN_RED = Color.parseColor("#DC143C");
        public static final int AUTUMN_BROWN = Color.parseColor("#8B4513");
        
        // Winter colors
        public static final int WINTER_BLUE = Color.parseColor("#4682B4");
        public static final int WINTER_WHITE = Color.parseColor("#F0F8FF");
        public static final int WINTER_SILVER = Color.parseColor("#C0C0C0");
    }
    
    private static final Random random = new Random();
    private static SeasonalEvent currentEvent = SeasonalEvent.NONE;
    private static boolean seasonalEffectsEnabled = true;
    
    /**
     * Update current seasonal event based on date
     */
    public static void updateSeasonalEvent() {
        if (!seasonalEffectsEnabled) {
            currentEvent = SeasonalEvent.NONE;
            return;
        }
        
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1; // Calendar months are 0-based
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        currentEvent = determineSeasonalEvent(month, day);
    }
    
    /**
     * Determine seasonal event based on month and day
     */
    private static SeasonalEvent determineSeasonalEvent(int month, int day) {
        // Christmas season (December 20-31)
        if (month == 12 && day >= 20) {
            return SeasonalEvent.CHRISTMAS;
        }
        
        // New Year (January 1-7)
        if (month == 1 && day <= 7) {
            return SeasonalEvent.NEW_YEAR;
        }
        
        // Halloween (October 25-31)
        if (month == 10 && day >= 25) {
            return SeasonalEvent.HALLOWEEN;
        }
        
        // Valentine's Day (February 10-15)
        if (month == 2 && day >= 10 && day <= 15) {
            return SeasonalEvent.VALENTINE;
        }
        
        // Spring season (March 20-April 30)
        if ((month == 3 && day >= 20) || month == 4) {
            return SeasonalEvent.SPRING;
        }
        
        // Summer season (June 21-August 31)
        if ((month == 6 && day >= 21) || month == 7 || month == 8) {
            return SeasonalEvent.SUMMER;
        }
        
        // Autumn season (September 22-November 15)
        if ((month == 9 && day >= 22) || month == 10 || (month == 11 && day <= 15)) {
            return SeasonalEvent.AUTUMN;
        }
        
        // Winter season (December 1-19)
        if (month == 12 && day <= 19) {
            return SeasonalEvent.WINTER;
        }
        
        return SeasonalEvent.NONE;
    }
    
    /**
     * Get current seasonal event
     */
    public static SeasonalEvent getCurrentEvent() {
        return currentEvent;
    }
    
    /**
     * Check if seasonal effects are enabled
     */
    public static boolean isSeasonalEffectsEnabled() {
        return seasonalEffectsEnabled;
    }
    
    /**
     * Enable/disable seasonal effects
     */
    public static void setSeasonalEffectsEnabled(boolean enabled) {
        seasonalEffectsEnabled = enabled;
        if (!enabled) {
            currentEvent = SeasonalEvent.NONE;
        } else {
            updateSeasonalEvent();
        }
    }
    
    /**
     * Get seasonal color palette
     */
    public static int[] getSeasonalColors() {
        switch (currentEvent) {
            case CHRISTMAS:
                return new int[] {
                    SeasonalColors.CHRISTMAS_RED,
                    SeasonalColors.CHRISTMAS_GREEN,
                    SeasonalColors.CHRISTMAS_GOLD,
                    SeasonalColors.CHRISTMAS_WHITE
                };
                
            case HALLOWEEN:
                return new int[] {
                    SeasonalColors.HALLOWEEN_ORANGE,
                    SeasonalColors.HALLOWEEN_PURPLE,
                    SeasonalColors.HALLOWEEN_BLACK
                };
                
            case VALENTINE:
                return new int[] {
                    SeasonalColors.VALENTINE_PINK,
                    SeasonalColors.VALENTINE_RED,
                    SeasonalColors.VALENTINE_WHITE
                };
                
            case SPRING:
                return new int[] {
                    SeasonalColors.SPRING_PINK,
                    SeasonalColors.SPRING_GREEN,
                    SeasonalColors.SPRING_YELLOW
                };
                
            case SUMMER:
                return new int[] {
                    SeasonalColors.SUMMER_BLUE,
                    SeasonalColors.SUMMER_YELLOW,
                    SeasonalColors.SUMMER_GREEN
                };
                
            case AUTUMN:
                return new int[] {
                    SeasonalColors.AUTUMN_ORANGE,
                    SeasonalColors.AUTUMN_RED,
                    SeasonalColors.AUTUMN_BROWN
                };
                
            case WINTER:
                return new int[] {
                    SeasonalColors.WINTER_BLUE,
                    SeasonalColors.WINTER_WHITE,
                    SeasonalColors.WINTER_SILVER
                };
                
            default:
                return new int[] {Color.WHITE, Color.GRAY, Color.DKGRAY};
        }
    }
    
    /**
     * Get random seasonal color
     */
    public static int getRandomSeasonalColor() {
        int[] colors = getSeasonalColors();
        return colors[random.nextInt(colors.length)];
    }
    
    /**
     * Get primary seasonal color
     */
    public static int getPrimarySeasonalColor() {
        int[] colors = getSeasonalColors();
        return colors.length > 0 ? colors[0] : Color.WHITE;
    }
    
    /**
     * Get secondary seasonal color
     */
    public static int getSecondarySeasonalColor() {
        int[] colors = getSeasonalColors();
        return colors.length > 1 ? colors[1] : Color.GRAY;
    }
    
    /**
     * Check if should show seasonal particles
     */
    public static boolean shouldShowSeasonalParticles() {
        return seasonalEffectsEnabled && currentEvent != SeasonalEvent.NONE;
    }
    
    /**
     * Get seasonal particle type
     */
    public static String getSeasonalParticleType() {
        switch (currentEvent) {
            case CHRISTMAS:
                return "snow";
            case HALLOWEEN:
                return "pumpkin";
            case VALENTINE:
                return "heart";
            case SPRING:
                return "petal";
            case SUMMER:
                return "sparkle";
            case AUTUMN:
                return "leaf";
            case WINTER:
                return "snowflake";
            default:
                return null;
        }
    }
    
    /**
     * Get seasonal background effect
     */
    public static String getSeasonalBackgroundEffect() {
        switch (currentEvent) {
            case CHRISTMAS:
                return "christmas_lights";
            case HALLOWEEN:
                return "spooky_fog";
            case VALENTINE:
                return "hearts";
            case SPRING:
                return "petals";
            case SUMMER:
                return "sunshine";
            case AUTUMN:
                return "falling_leaves";
            case WINTER:
                return "snow";
            default:
                return null;
        }
    }
    
    /**
     * Get seasonal music modifier
     */
    public static float getSeasonalMusicModifier() {
        switch (currentEvent) {
            case CHRISTMAS:
                return 1.0f; // Normal speed with jingle bells
            case HALLOWEEN:
                return 0.95f; // Slightly slower, spooky
            case VALENTINE:
                return 1.05f; // Slightly faster, romantic
            case SPRING:
                return 1.02f; // Slightly upbeat
            case SUMMER:
                return 1.08f; // More energetic
            case AUTUMN:
                return 0.98f; // Calmer
            case WINTER:
                return 0.95f; // Slower, peaceful
            default:
                return 1.0f;
        }
    }
    
    /**
     * Check if should show seasonal UI elements
     */
    public static boolean shouldShowSeasonalUI() {
        return seasonalEffectsEnabled && 
               (currentEvent == SeasonalEvent.CHRISTMAS || 
                currentEvent == SeasonalEvent.HALLOWEEN ||
                currentEvent == SeasonalEvent.NEW_YEAR);
    }
    
    /**
     * Get seasonal greeting message
     */
    public static String getSeasonalGreeting() {
        switch (currentEvent) {
            case CHRISTMAS:
                return "Merry Christmas!";
            case NEW_YEAR:
                return "Happy New Year!";
            case HALLOWEEN:
                return "Happy Halloween!";
            case VALENTINE:
                return "Happy Valentine's Day!";
            case SPRING:
                return "Welcome Spring!";
            case SUMMER:
                return "Summer Vibes!";
            case AUTUMN:
                return "Autumn Colors!";
            case WINTER:
                return "Winter Wonderland!";
            default:
                return "";
        }
    }
    
    /**
     * Get seasonal special effect intensity
     */
    public static float getSeasonalEffectIntensity() {
        switch (currentEvent) {
            case CHRISTMAS:
            case HALLOWEEN:
            case NEW_YEAR:
                return 1.0f; // High intensity
            case VALENTINE:
            case SPRING:
            case SUMMER:
                return 0.8f; // Medium intensity
            case AUTUMN:
            case WINTER:
                return 0.6f; // Lower intensity
            default:
                return 0.0f;
        }
    }
    
    /**
     * Check if date is special event
     */
    public static boolean isSpecialEventDay() {
        return currentEvent != SeasonalEvent.NONE && 
               (currentEvent == SeasonalEvent.CHRISTMAS ||
                currentEvent == SeasonalEvent.NEW_YEAR ||
                currentEvent == SeasonalEvent.HALLOWEEN ||
                currentEvent == SeasonalEvent.VALENTINE);
    }
    
    /**
     * Get seasonal achievement bonus
     */
    public static float getSeasonalAchievementBonus() {
        if (!isSpecialEventDay()) {
            return 1.0f;
        }
        
        switch (currentEvent) {
            case CHRISTMAS:
            case NEW_YEAR:
                return 1.2f; // 20% bonus
            case HALLOWEEN:
            case VALENTINE:
                return 1.1f; // 10% bonus
            default:
                return 1.0f;
        }
    }
    
    /**
     * Initialize seasonal system
     */
    public static void initialize() {
        updateSeasonalEvent();
    }
    
    /**
     * Get seasonal event description
     */
    public static String getEventDescription() {
        if (currentEvent == SeasonalEvent.NONE) {
            return "No special event";
        }
        
        switch (currentEvent) {
            case CHRISTMAS:
                return "Christmas Season - Snow, lights, and festive music!";
            case NEW_YEAR:
                return "New Year Celebration - Fireworks and celebration!";
            case HALLOWEEN:
                return "Halloween - Spooky effects and dark themes!";
            case VALENTINE:
                return "Valentine's Day - Love and hearts everywhere!";
            case SPRING:
                return "Spring - Flowers blooming and fresh colors!";
            case SUMMER:
                return "Summer - Sunshine and vibrant energy!";
            case AUTUMN:
                return "Autumn - Falling leaves and warm colors!";
            case WINTER:
                return "Winter - Snowflakes and peaceful atmosphere!";
            default:
                return "Seasonal event active!";
        }
    }
}

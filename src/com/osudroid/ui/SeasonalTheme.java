package com.osudroid.ui;

import java.util.Calendar;

/**
 * Colour palettes and seasonal effects for the main menu.
 *
 * <p>The palette follows the current date: New Year (December-January),
 * Valentine's Day, spring, summer, Halloween, autumn and St. Patrick's Day.
 * Summer and the neutral periods keep only the side flashes.</p>
 */
public final class SeasonalTheme {

    public enum Season {
        NEW_YEAR,
        VALENTINE,
        SPRING,
        ST_PATRICK,
        SUMMER,
        AUTUMN,
        HALLOWEEN,
        NEUTRAL
    }

    public enum FallingEffect {
        NONE,
        SNOW,
        VALENTINE_HEARTS,
        SPRING_LEAVES,
        AUTUMN_LEAVES,
        HALLOWEEN
    }

    private static final SeasonalTheme NEUTRAL = new SeasonalTheme(
            Season.NEUTRAL,
            new int[]{0xFFFFFF, 0xFFF8E1, 0xFFFFCC},
            new int[]{0xFFFFFF},
            1f, 1f, 1f
    );

    private static final SeasonalTheme NEW_YEAR = new SeasonalTheme(
            Season.NEW_YEAR,
            new int[]{0xD32F2F, 0x388E3C, 0xFFD54F, 0xFFFFCC, 0xFFFFFF},
            new int[]{0xD32F2F, 0x388E3C, 0xFFD54F},
            1f, 1f, 0.8f
    );

    private static final SeasonalTheme VALENTINE = new SeasonalTheme(
            Season.VALENTINE,
            new int[]{0xE91E63, 0xFF4081, 0xFF80AB, 0xFFD1DC},
            new int[]{0xE91E63, 0xFF4081},
            1f, 0.55f, 0.72f
    );

    private static final SeasonalTheme SPRING = new SeasonalTheme(
            Season.SPRING,
            new int[]{0xFF80AB, 0xFFB6C1, 0xFFC0CB, 0xFFD1DC, 0xFFF0F5},
            new int[]{0xFF80AB, 0xFFB6C1, 0xFF69B4},
            1f, 0.72f, 0.82f
    );

    private static final SeasonalTheme ST_PATRICK = new SeasonalTheme(
            Season.ST_PATRICK,
            new int[]{0x2E7D32, 0x43A047, 0x81C784, 0xFFFFFF},
            new int[]{0x2E7D32, 0x43A047},
            0.55f, 1f, 0.6f
    );

    private static final SeasonalTheme SUMMER = new SeasonalTheme(
            Season.SUMMER,
            new int[]{0x00BCD4, 0x03A9F4, 0x81D4FA, 0xFFFFFF},
            new int[]{0x00BCD4, 0x03A9F4},
            0.55f, 0.9f, 1f
    );

    private static final SeasonalTheme HALLOWEEN = new SeasonalTheme(
            Season.HALLOWEEN,
            new int[]{0xFF8C00, 0xFFA500, 0xFF6F00, 0xFFD180},
            new int[]{0xFF8C00, 0xFF6F00},
            1f, 0.62f, 0.12f
    );

    private static final SeasonalTheme AUTUMN = new SeasonalTheme(
            Season.AUTUMN,
            new int[]{0xFF9800, 0xFFB74D, 0xD84315, 0xFFE0B2},
            new int[]{0xFF9800, 0xFFB74D},
            1f, 0.72f, 0.3f
    );

    private final Season season;
    private final int[] lightColors;
    private final int[] sideColors;
    private final float barR;
    private final float barG;
    private final float barB;

    private SeasonalTheme(Season season, int[] lightColors, int[] sideColors,
                          float barR, float barG, float barB) {
        this.season = season;
        this.lightColors = lightColors;
        this.sideColors = sideColors;
        this.barR = barR;
        this.barG = barG;
        this.barB = barB;
    }

    /** Resolves the season and palette for today's local date. */
    public static SeasonalTheme resolve() {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // December and January: New Year / Christmas.
        if (month == Calendar.DECEMBER || month == Calendar.JANUARY) {
            return NEW_YEAR;
        }
        if (month == Calendar.FEBRUARY) {
            return VALENTINE;
        }
        if (month == Calendar.MARCH && day == 17) {
            return ST_PATRICK;
        }
        if (month >= Calendar.MARCH && month <= Calendar.MAY) {
            return SPRING;
        }
        if (month >= Calendar.JUNE && month <= Calendar.AUGUST) {
            return SUMMER;
        }
        if (month == Calendar.OCTOBER) {
            return HALLOWEEN;
        }
        if (month == Calendar.SEPTEMBER || month == Calendar.NOVEMBER) {
            return AUTUMN;
        }
        return NEUTRAL;
    }

    public Season getSeason() {
        return season;
    }

    /**
     * Falling menu particles for this season. Summer and the neutral palette
     * deliberately have none: only the side flashes remain visible there.
     */
    public FallingEffect getFallingEffect() {
        return switch (season) {
            case NEW_YEAR -> FallingEffect.SNOW;
            case VALENTINE -> FallingEffect.VALENTINE_HEARTS;
            case SPRING -> FallingEffect.SPRING_LEAVES;
            case AUTUMN -> FallingEffect.AUTUMN_LEAVES;
            case HALLOWEEN -> FallingEffect.HALLOWEEN;
            default -> FallingEffect.NONE;
        };
    }

    /** The background beat lights are a New Year-only effect. */
    public boolean usesBeatLights() {
        return season == Season.NEW_YEAR;
    }

    /** The Christmas/New Year bell belongs to winter only. */
    public boolean usesBell() {
        return season == Season.NEW_YEAR;
    }

    public static SeasonalTheme neutral() {
        return NEUTRAL;
    }

    public int randomLightColor() {
        return lightColors[(int) (Math.random() * lightColors.length)];
    }

    public int randomSideColor() {
        return sideColors[(int) (Math.random() * sideColors.length)];
    }

    public float getBarR() {
        return barR;
    }

    public float getBarG() {
        return barG;
    }

    public float getBarB() {
        return barB;
    }
}

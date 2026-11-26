package com.osudroid.ui;

import java.util.Calendar;

/**
 * Colour palettes for the seasonal main-menu effects.
 *
 * <p>With automatic colours enabled the palette follows the current date:
 * New Year (December-January), Valentine's Day, spring, summer, Halloween,
 * autumn and St. Patrick's Day. Every other time of the year is neutral white.</p>
 */
public final class SeasonalTheme {

    private static final SeasonalTheme NEUTRAL = new SeasonalTheme(
            new int[]{0xFFFFFF, 0xFFF8E1, 0xFFFFCC},
            new int[]{0xFFFFFF},
            1f, 1f, 1f
    );

    private static final SeasonalTheme NEW_YEAR = new SeasonalTheme(
            new int[]{0xD32F2F, 0x388E3C, 0xFFD54F, 0xFFFFCC, 0xFFFFFF},
            new int[]{0xD32F2F, 0x388E3C, 0xFFD54F},
            1f, 1f, 0.8f
    );

    private static final SeasonalTheme VALENTINE = new SeasonalTheme(
            new int[]{0xE91E63, 0xFF4081, 0xFF80AB, 0xFFD1DC},
            new int[]{0xE91E63, 0xFF4081},
            1f, 0.55f, 0.72f
    );

    private static final SeasonalTheme SPRING = new SeasonalTheme(
            new int[]{0xFF80AB, 0xFFB6C1, 0xFFC0CB, 0xFFD1DC, 0xFFF0F5},
            new int[]{0xFF80AB, 0xFFB6C1, 0xFF69B4},
            1f, 0.72f, 0.82f
    );

    private static final SeasonalTheme ST_PATRICK = new SeasonalTheme(
            new int[]{0x2E7D32, 0x43A047, 0x81C784, 0xFFFFFF},
            new int[]{0x2E7D32, 0x43A047},
            0.55f, 1f, 0.6f
    );

    private static final SeasonalTheme SUMMER = new SeasonalTheme(
            new int[]{0x00BCD4, 0x03A9F4, 0x81D4FA, 0xFFFFFF},
            new int[]{0x00BCD4, 0x03A9F4},
            0.55f, 0.9f, 1f
    );

    private static final SeasonalTheme HALLOWEEN = new SeasonalTheme(
            new int[]{0xFF8C00, 0xFFA500, 0xFF6F00, 0xFFD180},
            new int[]{0xFF8C00, 0xFF6F00},
            1f, 0.62f, 0.12f
    );

    private static final SeasonalTheme AUTUMN = new SeasonalTheme(
            new int[]{0xFF9800, 0xFFB74D, 0xD84315, 0xFFE0B2},
            new int[]{0xFF9800, 0xFFB74D},
            1f, 0.72f, 0.3f
    );

    private final int[] lightColors;
    private final int[] sideColors;
    private final float barR;
    private final float barG;
    private final float barB;

    private SeasonalTheme(int[] lightColors, int[] sideColors,
                          float barR, float barG, float barB) {
        this.lightColors = lightColors;
        this.sideColors = sideColors;
        this.barR = barR;
        this.barG = barG;
        this.barB = barB;
    }

    /**
     * Resolves the palette for today. With automatic colours off the classic
     * red/green New Year palette is used; with it on, neutral white outside
     * the holiday periods.
     */
    public static SeasonalTheme resolve(boolean automatic) {
        if (!automatic) {
            return NEW_YEAR;
        }

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
        if (month == Calendar.SEPTEMBER) {
            return AUTUMN;
        }
        if (month == Calendar.OCTOBER) {
            return HALLOWEEN;
        }
        return NEUTRAL;
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

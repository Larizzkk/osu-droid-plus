package ru.nsu.ccfit.zuev.osu.game.cursor;

/**
 * Enumeration of available autoplay cursor movement styles.
 * Based on danser-go movement styles.
 */
public enum AutoplayStyle {
    LINEAR("linear"),
    BEZIER("bezier"), 
    SPLINE("spline"),
    CIRCULAR("circular"),
    AXIS("axis"),
    EXGON("exgon"),
    AGGRESSIVE("aggressive"),
    MOMENTUM("momentum"),
    PIPPI("pippi"),
    FLOWER("flower");

    private final String value;

    AutoplayStyle(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AutoplayStyle fromValue(String value) {
        for (AutoplayStyle style : values()) {
            if (style.value.equals(value)) {
                return style;
            }
        }
        return LINEAR; // Default fallback
    }
}

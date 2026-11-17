package ru.nsu.ccfit.zuev.osu.scoring;

/**
 * Represents the scoring mode used in online beatmap leaderboard.
 *
 * <p>The first value stays {@link #SCORE} so previously saved preference values
 * keep their meaning. {@link #DPP} replaces the old generic "PP" mode: the map
 * board now ranks by the same droid PP formula as the player profile instead of
 * the best of DPP/RX/AP.</p>
 */
public enum BeatmapLeaderboardScoringMode {
    SCORE("score", "Score"),
    DPP("dpp", "DPP"),
    RXPP("rxpp", "RX"),
    APPP("appp", "AP"),
    CHEATPP("cheat", "Cheat");

    private final String apiType;
    private final String label;

    BeatmapLeaderboardScoringMode(String apiType, String label) {
        this.apiType = apiType;
        this.label = label;
    }

    /**
     * Value sent as {@code type=} to the server's beatmap leaderboard endpoint.
     */
    public String getApiType() {
        return apiType;
    }

    /**
     * Short label shown next to the leaderboard switcher.
     */
    public String getLabel() {
        return label;
    }

    public boolean isScore() {
        return this == SCORE;
    }

    /**
     * Parses an integer value to a {@link BeatmapLeaderboardScoringMode}.
     *
     * @param value The integer value to parse.
     * @return The parsed {@link BeatmapLeaderboardScoringMode}.
     */
    public static BeatmapLeaderboardScoringMode parse(int value) {
        return switch (value) {
            case 1 -> DPP;
            case 2 -> RXPP;
            case 3 -> APPP;
            case 4 -> CHEATPP;
            default -> SCORE;
        };
    }
}

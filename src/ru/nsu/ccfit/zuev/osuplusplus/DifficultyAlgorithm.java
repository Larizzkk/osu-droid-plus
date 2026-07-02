package ru.nsu.ccfit.zuev.osuplusplus;

/**
 * Represents the algorithm used to calculate the difficulty of a beatmap.
 */
public enum DifficultyAlgorithm {
    /**
     * osu!droid algorithm.
     */
    droid,

    /**
     * osu!standard algorithm.
     */
    standard,

    /**
     * DRPP — Droid Ranking Performance Points (server-side formula).
     */
    drpp,

    /**
     * RXPP — Relax Performance Points (server-side formula).
     */
    rxpp;

    /**
     * Parses an integer value to a {@link DifficultyAlgorithm}.
     *
     * @param value The integer value to parse.
     * @return The parsed {@link DifficultyAlgorithm}.
     */
    public static DifficultyAlgorithm parse(int value) {
        return switch (value) {
            case 1 -> standard;
            case 2 -> drpp;
            case 3 -> rxpp;
            default -> droid;
        };
    }
}

package ru.nsu.ccfit.zuev.osuplusplus.utils;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class for formatting various values in the game
 * Based on osu!lazer's FormatUtils.cs
 */
public class FormatUtils {
    
    private static final DecimalFormat accuracyFormat = new DecimalFormat("0.00%");
    private static final DecimalFormat starRatingFormat = new DecimalFormat("0.00");
    private static final DecimalFormat bpmFormat = new DecimalFormat("0");
    
    /**
     * Formats accuracy as percentage with 2 decimal places
     * @param accuracy The accuracy value (0.0 to 1.0)
     * @return Formatted accuracy string
     */
    public static String formatAccuracy(double accuracy) {
        // Ensure we don't round up to next whole number
        accuracy = Math.floor(accuracy * 10000) / 10000;
        return accuracyFormat.format(accuracy);
    }
    
    /**
     * Formats star rating with 2 decimal places
     * @param starRating The star rating value
     * @return Formatted star rating string
     */
    public static String formatStarRating(double starRating) {
        // Ensure we don't round up to next whole number
        starRating = Math.floor(starRating * 100) / 100;
        return starRatingFormat.format(starRating);
    }
    
    /**
     * Formats rank with metric notation
     * @param rank The rank number
     * @return Formatted rank string
     */
    public static String formatRank(int rank) {
        if (rank < 1000) {
            return String.valueOf(rank);
        } else if (rank < 10000) {
            return String.format(Locale.US, "%.1fk", rank / 1000.0);
        } else if (rank < 1000000) {
            return String.format(Locale.US, "%.0fk", rank / 1000);
        } else if (rank < 10000000) {
            return String.format(Locale.US, "%.1fM", rank / 1000000.0);
        } else {
            return String.format(Locale.US, "%.0fM", rank / 1000000);
        }
    }
    
    /**
     * Formats BPM value
     * @param baseBpm The base BPM
     * @param rate The playback rate (default 1.0)
     * @return Rounded BPM value
     */
    public static int roundBPM(double baseBpm, double rate) {
        return (int) Math.round(baseBpm * rate);
    }
    
    /**
     * Formats time in seconds to MM:SS format
     * @param seconds Time in seconds
     * @return Formatted time string
     */
    public static String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format(Locale.US, "%d:%02d", minutes, secs);
    }
    
    /**
     * Formats time in milliseconds to MM:SS:mmm format
     * @param milliseconds Time in milliseconds
     * @return Formatted time string
     */
    public static String formatTimePrecise(long milliseconds) {
        long seconds = milliseconds / 1000;
        long ms = milliseconds % 1000;
        
        long minutes = seconds / 60;
        long secs = seconds % 60;
        
        return String.format(Locale.US, "%d:%02d.%03d", minutes, secs, ms);
    }
    
    /**
     * Formats large numbers with K/M/B suffixes
     * @param value The number to format
     * @return Formatted number string
     */
    public static String formatLargeNumber(long value) {
        if (value < 1000) {
            return String.valueOf(value);
        } else if (value < 1000000) {
            return String.format(Locale.US, "%.1fK", value / 1000.0);
        } else if (value < 1000000000) {
            return String.format(Locale.US, "%.1fM", value / 1000000.0);
        } else {
            return String.format(Locale.US, "%.1fB", value / 1000000000.0);
        }
    }
    
    /**
     * Creates a spannable string with different sizes for main text and suffix
     * @param mainText The main text
     * @param suffix The suffix text (smaller)
     * @return SpannableString with different text sizes
     */
    public static SpannableString createSizeSpannable(String mainText, String suffix) {
        String fullText = mainText + suffix;
        SpannableString spannable = new SpannableString(fullText);
        spannable.setSpan(new RelativeSizeSpan(0.7f), mainText.length(), fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }
    
    /**
     * Formats score with comma separators
     * @param score The score value
     * @return Formatted score string
     */
    public static String formatScore(long score) {
        return NumberFormat.getNumberInstance(Locale.US).format(score);
    }
    
    /**
     * Formats percentage with specified decimal places
     * @param value The value to format (0.0 to 1.0)
     * @param decimals Number of decimal places
     * @return Formatted percentage string
     */
    public static String formatPercentage(double value, int decimals) {
        double percentage = value * 100;
        String format = "%." + decimals + "f%%";
        return String.format(Locale.US, format, percentage);
    }
    
    /**
     * Formats file size in human readable format
     * @param bytes Size in bytes
     * @return Formatted file size string
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Truncates string to specified length with ellipsis
     * @param text The text to truncate
     * @param maxLength Maximum length
     * @return Truncated text
     */
    public static String truncateWithEllipsis(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Formats difficulty name with proper capitalization
     * @param difficulty The difficulty string
     * @return Formatted difficulty name
     */
    public static String formatDifficultyName(String difficulty) {
        if (difficulty == null || difficulty.isEmpty()) {
            return "";
        }
        
        // Handle special cases
        if (difficulty.equalsIgnoreCase("ez")) return "Easy";
        if (difficulty.equalsIgnoreCase("nm")) return "Normal";
        if (difficulty.equalsIgnoreCase("hr")) return "Hard";
        if (difficulty.equalsIgnoreCase("hd")) return "HD";
        if (difficulty.equalsIgnoreCase("dt")) return "DT";
        if (difficulty.equalsIgnoreCase("ht")) return "HT";
        if (difficulty.equalsIgnoreCase("nc")) return "NC";
        if (difficulty.equalsIgnoreCase("fl")) return "FL";
        
        // Capitalize first letter, lowercase rest
        return difficulty.substring(0, 1).toUpperCase() + difficulty.substring(1).toLowerCase();
    }
}

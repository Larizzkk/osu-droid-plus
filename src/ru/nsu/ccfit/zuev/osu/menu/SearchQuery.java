package ru.nsu.ccfit.zuev.osu.menu;

import com.osudroid.data.BeatmapInfo;
import com.osudroid.data.BeatmapSetInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.nsu.ccfit.zuev.osuplusplus.DifficultyAlgorithm;

/**
 * Parses song select search queries using the osu!lazer query syntax.
 * <p>
 * Supported constructs:
 * <ul>
 *     <li>Plain terms - substring match against title/artist/creator/tags/source/ID/difficulty names.</li>
 *     <li>{@code "quoted phrases"} - the phrase must appear as an isolated sequence of words.</li>
 *     <li>{@code [difficulty name]} - substring match against difficulty names.</li>
 *     <li>Numeric filters with {@code =}, {@code :}, {@code !=}, {@code !:}, {@code <}, {@code <=}, {@code >}, {@code >=}:
 *     {@code ar}, {@code od}, {@code cs}, {@code hp}/{@code dr},
 *     {@code star}/{@code sr}/{@code stars}, {@code droidstar}, {@code standardstar}, {@code bpm}.
 *     Decimal values are allowed (e.g. {@code ar>9.5}).</li>
 *     <li>{@code length[><=]time} - accepts {@code m:ss}, {@code h:mm:ss}, {@code 1h2m3s} or plain seconds.</li>
 *     <li>Text filters with {@code =}/{@code !=}: {@code mapper}/{@code creator}/{@code author},
 *     {@code artist}, {@code title}, {@code diff}, {@code source}, {@code tag}.</li>
 *     <li>{@code played=yes/no} - filters by the presence of local scores on at least one difficulty
 *     of the set (for {@code no}, the set must be completely unplayed).</li>
 * </ul>
 * Unknown {@code key=value} tokens are left as plain search terms.
 */
public final class SearchQuery {

    private enum NumericField {
        AR, OD, CS, HP, STAR_DROID, STAR_STANDARD, BPM
    }

    private enum TextField {
        ARTIST, TITLE, CREATOR, DIFF, SOURCE, TAG
    }

    private enum Op {
        EQ, NEQ, LT, LE, GT, GE
    }

    private static final class NumericFilter {
        final NumericField field;
        final Op op;
        final float value;
        final float tolerance;

        NumericFilter(NumericField field, Op op, float value, float tolerance) {
            this.field = field;
            this.op = op;
            this.value = value;
            this.tolerance = tolerance;
        }
    }

    private static final class LengthFilter {
        final Op op;
        final long millis;
        final long tolerance;

        LengthFilter(Op op, long millis, long tolerance) {
            this.op = op;
            this.millis = millis;
            this.tolerance = tolerance;
        }
    }

    private static final class TextFilter {
        final TextField field;
        final boolean exclude;
        final String term;

        TextFilter(TextField field, boolean exclude, String term) {
            this.field = field;
            this.exclude = exclude;
            this.term = term;
        }
    }

    private static final Pattern QUERY_SYNTAX = Pattern.compile(
        "\\b(\\w+)(!=|!:|>=|<=|<:|>:|=|>|<|:)(\"[^\"]*\"|\\S+)");
    private static final Pattern BRACKET_SYNTAX = Pattern.compile("(^|\\s)\\[(.*?)\\]");
    private static final Pattern QUOTE_SYNTAX = Pattern.compile("\"([^\"]+)\"");
    private static final Pattern NUMERIC_VALUE = Pattern.compile("\\d+(\\.\\d+)?");
    private static final Pattern LENGTH_CLOCK = Pattern.compile("(?:(\\d+):)?(\\d+):(\\d+)");
    private static final Pattern LENGTH_UNITS = Pattern.compile(
        "(?:(\\d+(?:\\.\\d+)?)h)?(?:(\\d+(?:\\.\\d+)?)m)?(?:(\\d+(?:\\.\\d+)?)s)?");
    private static final Pattern LENGTH_SECONDS = Pattern.compile("(\\d+(?:\\.\\d+)?)");

    private final List<String> plainTerms = new ArrayList<>();
    private final List<Pattern> phrases = new ArrayList<>();
    private final List<String> difficultyNames = new ArrayList<>();
    private final List<NumericFilter> numericFilters = new ArrayList<>();
    private final List<LengthFilter> lengthFilters = new ArrayList<>();
    private final List<TextFilter> textFilters = new ArrayList<>();
    private Boolean played = null;

    private SearchQuery() {
    }

    /**
     * Parses the given (already lowercased) filter string into a query.
     */
    public static SearchQuery parse(final String filter) {
        final SearchQuery query = new SearchQuery();
        String text = filter == null ? "" : filter.toLowerCase(Locale.ROOT);

        // Keyed queries (bpm>200, ar<=9.5, mapper="john", played=no, ...).
        Matcher matcher = QUERY_SYNTAX.matcher(text);
        while (matcher.find()) {
            final String key = matcher.group(1);
            final String opString = matcher.group(2);
            String value = matcher.group(3);
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                value = value.substring(1, value.length() - 1);
            }

            if (query.tryApplyQuery(key, opString, value)) {
                text = text.replaceFirst(Pattern.quote(matcher.group(0)), " ");
                matcher = QUERY_SYNTAX.matcher(text);
            }
        }

        // [difficulty name] filters.
        matcher = BRACKET_SYNTAX.matcher(text);
        while (matcher.find()) {
            final String name = matcher.group(2).trim().toLowerCase(Locale.ROOT);
            if (!name.isEmpty()) {
                query.difficultyNames.add(name);
            }
            text = text.replaceFirst(Pattern.quote(matcher.group(0)), " ");
            matcher = BRACKET_SYNTAX.matcher(text);
        }

        // "quoted phrases" - matched as isolated words.
        matcher = QUOTE_SYNTAX.matcher(text);
        while (matcher.find()) {
            query.phrases.add(Pattern.compile(
                "(^|\\b)" + Pattern.quote(matcher.group(1).toLowerCase(Locale.ROOT)) + "($|\\b)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
            text = text.replaceFirst(Pattern.quote(matcher.group(0)), " ");
            matcher = QUOTE_SYNTAX.matcher(text);
        }

        for (final String term : text.split(" ")) {
            if (!term.isEmpty()) {
                query.plainTerms.add(term);
            }
        }

        return query;
    }

    private boolean tryApplyQuery(final String key, final String opString, final String value) {
        final Op op = parseOp(opString);

        switch (key) {
            case "ar", "od", "cs", "hp", "dr", "star", "sr", "stars", "droidstar", "standardstar", "bpm" -> {
                if (op == null || !NUMERIC_VALUE.matcher(value).matches()) {
                    return false;
                }
                final float numeric = Float.parseFloat(value);
                switch (key) {
                    case "ar" -> numericFilters.add(new NumericFilter(NumericField.AR, op, numeric, 0.05f));
                    case "od" -> numericFilters.add(new NumericFilter(NumericField.OD, op, numeric, 0.05f));
                    case "cs" -> numericFilters.add(new NumericFilter(NumericField.CS, op, numeric, 0.05f));
                    case "hp", "dr" -> numericFilters.add(new NumericFilter(NumericField.HP, op, numeric, 0.05f));
                    case "droidstar" ->
                        numericFilters.add(new NumericFilter(NumericField.STAR_DROID, op, numeric, 0.005f));
                    case "standardstar", "star", "sr", "stars" ->
                        numericFilters.add(new NumericFilter(NumericField.STAR_STANDARD, op, numeric, 0.005f));
                    case "bpm" -> numericFilters.add(new NumericFilter(NumericField.BPM, op, numeric, 0.5f));
                }
                return true;
            }
            case "length" -> {
                if (op == null) {
                    return false;
                }
                final Long millis = parseLength(value);
                if (millis == null) {
                    return false;
                }
                // Tolerance mirrors osu!lazer: half of the smallest unit present in the value.
                final long tolerance = value.endsWith("h") ? 1800000 : 500;
                lengthFilters.add(new LengthFilter(op, millis, tolerance));
                return true;
            }
            case "played" -> {
                Boolean playedValue = switch (value) {
                    case "1", "yes", "true" -> Boolean.TRUE;
                    case "0", "no", "false" -> Boolean.FALSE;
                    default -> null;
                };
                if (playedValue == null || (op != Op.EQ && op != Op.NEQ)) {
                    return false;
                }
                if (op == Op.NEQ) {
                    playedValue = !playedValue;
                }
                played = playedValue;
                return true;
            }
            case "mapper", "creator", "author" -> {
                return tryApplyText(TextField.CREATOR, op, value);
            }
            case "artist" -> {
                return tryApplyText(TextField.ARTIST, op, value);
            }
            case "title" -> {
                return tryApplyText(TextField.TITLE, op, value);
            }
            case "diff" -> {
                return tryApplyText(TextField.DIFF, op, value);
            }
            case "source" -> {
                return tryApplyText(TextField.SOURCE, op, value);
            }
            case "tag" -> {
                return tryApplyText(TextField.TAG, op, value);
            }
            default -> {
                return false;
            }
        }
    }

    private boolean tryApplyText(final TextField field, final Op op, final String value) {
        if (value.isEmpty()) {
            return false;
        }
        // Only equality-like operators make sense for text filters.
        if (op == Op.EQ || op == Op.NEQ) {
            textFilters.add(new TextFilter(field, op == Op.NEQ, value));
            return true;
        }
        return false;
    }

    private static Op parseOp(final String opString) {
        return switch (opString) {
            case "=", ":" -> Op.EQ;
            case "!=", "!:" -> Op.NEQ;
            case "<" -> Op.LT;
            case "<=", "<:" -> Op.LE;
            case ">" -> Op.GT;
            case ">=", ">:" -> Op.GE;
            default -> null;
        };
    }

    /**
     * Parses a length value. Accepts {@code m:ss}, {@code h:mm:ss}, {@code 1h2m3s} or plain seconds.
     * Returns the length in milliseconds, or null if the value is not a valid length.
     */
    private static Long parseLength(final String value) {
        Matcher matcher = LENGTH_CLOCK.matcher(value);
        if (matcher.matches()) {
            long millis = 0;
            if (matcher.group(1) != null) {
                millis += Long.parseLong(matcher.group(1)) * 3600000L;
            }
            millis += Long.parseLong(matcher.group(2)) * 60000L;
            millis += Long.parseLong(matcher.group(3)) * 1000L;
            return millis;
        }

        long millis = 0;
        boolean anyUnit = false;
        matcher = LENGTH_UNITS.matcher(value);
        if (matcher.matches()) {
            if (matcher.group(1) != null) {
                millis += (long) (Double.parseDouble(matcher.group(1)) * 3600000f);
                anyUnit = true;
            }
            if (matcher.group(2) != null) {
                millis += (long) (Double.parseDouble(matcher.group(2)) * 60000f);
                anyUnit = true;
            }
            if (matcher.group(3) != null) {
                millis += (long) (Double.parseDouble(matcher.group(3)) * 1000f);
                anyUnit = true;
            }
            if (anyUnit) {
                return millis;
            }
        }

        matcher = LENGTH_SECONDS.matcher(value);
        if (matcher.matches()) {
            return (long) (Double.parseDouble(matcher.group(1)) * 1000f);
        }

        return null;
    }

    /**
     * Returns true if the query contains no filters at all.
     */
    public boolean isEmpty() {
        return plainTerms.isEmpty()
            && phrases.isEmpty()
            && difficultyNames.isEmpty()
            && numericFilters.isEmpty()
            && lengthFilters.isEmpty()
            && textFilters.isEmpty()
            && played == null;
    }

    /**
     * Checks the plain terms and quoted phrases against the combined (lowercased) beatmap text.
     */
    public boolean matchesText(final String lowerText) {
        for (final String term : plainTerms) {
            if (!lowerText.contains(term)) {
                return false;
            }
        }

        for (final Pattern phrase : phrases) {
            if (!phrase.matcher(lowerText).find()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks the [difficulty name] filters against the difficulty names of the set.
     * For single-difficulty items, only the referenced difficulty is checked.
     */
    public boolean matchesDifficultyNames(final BeatmapSetInfo set, final int beatmapId) {
        if (difficultyNames.isEmpty()) {
            return true;
        }

        if (beatmapId >= 0) {
            final String version = set.getBeatmap(beatmapId).getVersion().toLowerCase(Locale.ROOT);
            for (final String name : difficultyNames) {
                if (!version.contains(name)) {
                    return false;
                }
            }
            return true;
        }

        for (final String name : difficultyNames) {
            boolean any = false;
            for (int i = 0; i < set.getCount(); i++) {
                if (set.getBeatmap(i).getVersion().toLowerCase(Locale.ROOT).contains(name)) {
                    any = true;
                    break;
                }
            }
            if (!any) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks the numeric, length, text and played filters against the beatmaps of the set.
     * For sets, the query matches if any difficulty satisfies all filters (osu!lazer behavior).
     * For single-difficulty items, only the referenced difficulty is checked.
     *
     * @param playedHashes the MD5 hashes of beatmaps that have at least one local score
     */
    public boolean matchesBeatmaps(final BeatmapSetInfo set, final int beatmapId, final Set<String> playedHashes) {
        if (numericFilters.isEmpty() && lengthFilters.isEmpty() && textFilters.isEmpty() && played == null) {
            return true;
        }

        if (beatmapId >= 0) {
            return matchesBeatmap(set.getBeatmap(beatmapId), playedHashes);
        }

        for (int i = 0; i < set.getCount(); i++) {
            if (matchesBeatmap(set.getBeatmap(i), playedHashes)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesBeatmap(final BeatmapInfo beatmap, final Set<String> playedHashes) {
        for (final NumericFilter filter : numericFilters) {
            final float value = switch (filter.field) {
                case AR -> beatmap.getApproachRate();
                case OD -> beatmap.getOverallDifficulty();
                case CS -> beatmap.getCircleSize();
                case HP -> beatmap.getHpDrainRate();
                case STAR_DROID -> beatmap.getStarRating(DifficultyAlgorithm.droid);
                case STAR_STANDARD -> beatmap.getStarRating(DifficultyAlgorithm.standard);
                case BPM -> beatmap.getBpmMax();
            };

            if (!compare(value, filter.op, filter.value, filter.tolerance)) {
                return false;
            }
        }

        for (final LengthFilter filter : lengthFilters) {
            if (!compare(beatmap.getLength(), filter.op, filter.millis, filter.tolerance)) {
                return false;
            }
        }

        for (final TextFilter filter : textFilters) {
            final String value = switch (filter.field) {
                case ARTIST -> beatmap.getArtist();
                case TITLE -> beatmap.getTitle();
                case CREATOR -> beatmap.getCreator();
                case DIFF -> beatmap.getVersion();
                case SOURCE -> beatmap.getSource();
                case TAG -> beatmap.getTags();
            };

            final boolean matches = value.toLowerCase(Locale.ROOT).contains(filter.term);
            if (filter.exclude ? matches : !matches) {
                return false;
            }
        }

        if (played != null && (playedHashes.contains(beatmap.getMD5()) != played)) {
            return false;
        }

        return true;
    }

    private static boolean compare(final float value, final Op op, final float target, final float tolerance) {
        return switch (op) {
            case EQ -> Math.abs(value - target) <= tolerance;
            case NEQ -> Math.abs(value - target) > tolerance;
            case LT -> value < target - tolerance;
            case LE -> value <= target + tolerance;
            case GT -> value > target + tolerance;
            case GE -> value >= target - tolerance;
        };
    }

    private static boolean compare(final long value, final Op op, final long target, final long tolerance) {
        return switch (op) {
            case EQ -> Math.abs(value - target) <= tolerance;
            case NEQ -> Math.abs(value - target) > tolerance;
            case LT -> value < target - tolerance;
            case LE -> value <= target + tolerance;
            case GT -> value > target + tolerance;
            case GE -> value >= target - tolerance;
        };
    }
}

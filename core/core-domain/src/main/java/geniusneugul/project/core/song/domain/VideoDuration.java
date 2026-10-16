package geniusneugul.project.core.song.domain;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YouTube 영상 재생 시간 원문({@code m:ss}, {@code h:mm:ss})을 초로 바꾼다(MUSIC_SELECTION_RULE 2-2).
 */
public final class VideoDuration {

    private static final Pattern MINUTES_SECONDS = Pattern.compile("(\\d+):([0-5]\\d)");
    private static final Pattern HOURS_MINUTES_SECONDS = Pattern.compile("(\\d+):([0-5]\\d):([0-5]\\d)");

    private VideoDuration() {
    }

    /**
     * @return 형식이 맞지 않거나 0초면 빈 값. 재생할 수 없는 영상으로 본다
     */
    public static OptionalInt parseSeconds(String text) {
        if (text == null) {
            return OptionalInt.empty();
        }
        String trimmed = text.trim();
        Matcher hms = HOURS_MINUTES_SECONDS.matcher(trimmed);
        if (hms.matches()) {
            return positive(toInt(hms.group(1)) * 3600 + toInt(hms.group(2)) * 60 + toInt(hms.group(3)));
        }
        Matcher ms = MINUTES_SECONDS.matcher(trimmed);
        if (ms.matches()) {
            return positive(toInt(ms.group(1)) * 60 + toInt(ms.group(2)));
        }
        return OptionalInt.empty();
    }

    private static int toInt(String digits) {
        return Integer.parseInt(digits);
    }

    private static OptionalInt positive(int seconds) {
        return seconds > 0 ? OptionalInt.of(seconds) : OptionalInt.empty();
    }
}

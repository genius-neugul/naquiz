package geniusneugul.project.core.question.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 문제의 정답과 보조 정답. 입력값과 둘을 각각 정규화해 비교하고, 하나라도 같으면 정답이다(docs/DOMAIN.md 3-3).
 * 정규화는 공백 제거와 영문 소문자 변환만 한다. 특수문자는 그대로 비교한다(행복하니? ≠ 행복하니).
 */
public record Answer(String answer, String subAnswer) {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public static String normalize(String text) {
        return WHITESPACE.matcher(text).replaceAll("").toLowerCase(Locale.ROOT);
    }

    public boolean isCorrect(String input) {
        if (input == null) {
            return false;
        }
        String normalized = normalize(input);
        if (normalized.isEmpty()) {
            return false;
        }
        return normalized.equals(normalize(answer)) || (subAnswer != null && normalized.equals(normalize(subAnswer)));
    }
}

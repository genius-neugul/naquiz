package geniusneugul.project.core.game.twentyquestions.domain;

import geniusneugul.project.core.movie.domain.Movie;
import geniusneugul.project.core.question.domain.Answer;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 한 영화의 단서 종류별 공개 내용(docs/DOMAIN.md 3-2 「단서 종류 — 영화 스무고개」). 모든 단서 종류의 내용을 가진다.
 * 시놉시스는 본문에 나오는 정답(answer, subAnswer)을 ○○○로 가린다. 대소문자·공백 차이는 무시하고 찾는다.
 */
public record ClueContents(Map<ClueType, String> contents) {

    private static final String TITLE_MASK = "○○○";
    private static final String NO_AUDIENCE = "집계 X";
    private static final String NO_SYNOPSIS = "정보 없음";
    private static final int MAX_ACTORS = 5;
    private static final String SEPARATOR = ", ";
    private static final long TEN_THOUSAND = 10_000L;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public ClueContents {
        if (contents.size() != ClueType.values().length) {
            throw new IllegalArgumentException("모든 단서 종류의 공개 내용이 있어야 합니다. contents=" + contents.keySet());
        }
        contents = Collections.unmodifiableMap(new EnumMap<>(contents));
    }

    public static ClueContents of(Movie movie, Answer answer) {
        Map<ClueType, String> contents = new EnumMap<>(ClueType.class);
        contents.put(ClueType.AUDIENCE, audience(movie.getAudienceCount()));
        contents.put(ClueType.RELEASE_DATE, movie.getReleaseDate().toString());
        contents.put(ClueType.DIRECTOR, String.join(SEPARATOR, movie.getDirectors()));
        contents.put(ClueType.CAST, String.join(SEPARATOR, movie.getActors().stream().limit(MAX_ACTORS).toList()));
        contents.put(ClueType.SYNOPSIS, synopsis(movie.getSynopsis(), answer));
        contents.put(ClueType.GENRE, String.join(SEPARATOR, movie.getGenres()));
        contents.put(ClueType.NATION, String.join(SEPARATOR, movie.getNations()));
        contents.put(ClueType.RATING, movie.getRating());
        return new ClueContents(contents);
    }

    public String get(ClueType clueType) {
        return contents.get(clueType);
    }

    /** 단서 종류 수. 모든 단서를 열었는지 볼 때 쓴다 */
    public int size() {
        return contents.size();
    }

    /** 1만 명 이상은 만 명 단위로 반올림한다(10,313,086 → 약 1,031만 명). 관객 수가 없으면 집계 X */
    private static String audience(Long audienceCount) {
        if (audienceCount == null) {
            return NO_AUDIENCE;
        }
        if (audienceCount < TEN_THOUSAND) {
            return String.format("%,d명", audienceCount);
        }
        return String.format("약 %,d만 명", Math.round((double) audienceCount / TEN_THOUSAND));
    }

    private static String synopsis(String synopsis, Answer answer) {
        if (synopsis == null || synopsis.isBlank()) {
            return NO_SYNOPSIS;
        }
        String masked = synopsis;
        // 짧은 정답이 긴 정답의 일부일 수 있어 긴 것부터 가린다.
        List<String> titles = Stream.of(answer.answer(), answer.subAnswer())
                .filter(Objects::nonNull)
                .map(title -> WHITESPACE.matcher(title).replaceAll(""))
                .filter(title -> !title.isEmpty())
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
        for (String title : titles) {
            masked = titlePattern(title).matcher(masked).replaceAll(Matcher.quoteReplacement(TITLE_MASK));
        }
        return masked;
    }

    // 글자 사이에 공백이 있어도 찾는다(기생충 ↔ 기 생충).
    private static Pattern titlePattern(String title) {
        String regex = title.codePoints()
                .mapToObj(codePoint -> Pattern.quote(Character.toString(codePoint)))
                .collect(Collectors.joining("\\s*"));
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
}

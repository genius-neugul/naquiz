package geniusneugul.project.core.game.domain;

import geniusneugul.project.core.common.domain.GameType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

/**
 * 정답 힌트 단계별 공개 내용을 만드는 순수 함수 모음. 규칙은 docs/DOMAIN.md 「정답 힌트 단계」를 따른다.
 */
public final class AnswerHint {

    // 4단계에서 직전 공개 내용과 자리별로 비교하려면 마스킹 문자가 코드 포인트 하나여야 한다(⚫️처럼 이모지 선택자를 붙이지 않는다).
    public static final int MASK = 0x26AB;

    private static final String CHOSEONG = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ";
    private static final int SYLLABLES_PER_CHOSEONG = 588;
    private static final int PARTIAL_REVEAL_DIVISOR = 3;
    private static final int WHOLE_REVEAL_MAX_LENGTH = 2;

    private static final List<HintType> ANSWER_HINT_STAGES =
            List.of(HintType.ANSWER_MASK, HintType.ANSWER_SYMBOL, HintType.ANSWER_PARTIAL, HintType.ANSWER_RANDOM_CHAR);
    private static final List<HintType> STILL_CUT_STAGES = List.of(HintType.ANSWER_PARTIAL);

    private AnswerHint() {
    }

    /**
     * 다음에 열 정답 힌트 단계와 공개 내용. 게임별 단계 순서를 따르고, 새 정보가 없거나 정답이 전부 드러나는 단계는 건너뛴다.
     *
     * @param previous 이 라운드에서 마지막으로 공개된 정답 힌트. 아직 없으면 null
     * @return 더 열 단계가 없으면 빈 값
     */
    public static Optional<AnswerHintContent> next(GameType gameType, String answer, AnswerHintContent previous,
                                                   RandomGenerator random) {
        List<HintType> stages = gameType == GameType.MOVIE_STILL_CUT ? STILL_CUT_STAGES : ANSWER_HINT_STAGES;
        int start = 0;
        String revealBase = null;
        if (previous != null) {
            // 4단계는 반복해서 열 수 있으므로 4단계 다음도 4단계다.
            start = previous.type() == HintType.ANSWER_RANDOM_CHAR
                    ? stages.indexOf(HintType.ANSWER_RANDOM_CHAR)
                    : stages.indexOf(previous.type()) + 1;
            // 4단계는 3·4단계 공개 내용에서 한 글자를 더 연다.
            if (previous.type() == HintType.ANSWER_PARTIAL || previous.type() == HintType.ANSWER_RANDOM_CHAR) {
                revealBase = previous.content();
            }
        }
        if (start < 0) {
            return Optional.empty();
        }

        for (HintType stage : stages.subList(start, stages.size())) {
            Optional<String> content = switch (stage) {
                case ANSWER_MASK -> Optional.of(mask(answer));
                case ANSWER_SYMBOL -> hasSymbolStageInfo(answer) ? Optional.of(revealSymbols(answer)) : Optional.empty();
                case ANSWER_PARTIAL -> Optional.of(revealPartial(answer, random));
                case ANSWER_RANDOM_CHAR -> revealBase == null ? Optional.empty() : revealRandomChar(answer, revealBase, random);
                default -> throw new IllegalStateException("정답 힌트 단계가 아닙니다: " + stage);
            };
            if (content.isPresent() && !content.get().equals(answer)) {
                return Optional.of(new AnswerHintContent(stage, content.get()));
            }
        }
        return Optional.empty();
    }

    /**
     * 1단계(ANSWER_MASK). 띄어쓰기를 빼고 모든 글자를 가린 뒤 글자 수와 문자 종류 포함 여부를 붙인다.
     * 예: {@code ⚫⚫⚫⚫⚫⚫⚫ (7글자(특수문자 포함), 한글 O, 영어 X, 숫자 X, 특수문자 O)}
     */
    public static String mask(String answer) {
        List<AnswerCharKind> kinds = answer.codePoints().mapToObj(AnswerCharKind::of)
                .filter(kind -> kind != AnswerCharKind.SPACE)
                .toList();
        String masked = Character.toString(MASK).repeat(kinds.size());
        return "%s (%d글자(특수문자 포함), 한글 %s, 영어 %s, 숫자 %s, 특수문자 %s)".formatted(
                masked, kinds.size(),
                mark(kinds, AnswerCharKind.HANGUL), mark(kinds, AnswerCharKind.ENGLISH),
                mark(kinds, AnswerCharKind.DIGIT), mark(kinds, AnswerCharKind.SYMBOL));
    }

    /**
     * 2단계(ANSWER_SYMBOL). 숫자·특수문자·띄어쓰기를 보여주고 한글·영문은 가린다.
     */
    public static String revealSymbols(String answer) {
        return answer.codePoints()
                .map(c -> isLetter(c) ? MASK : c)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    /**
     * 3단계(ANSWER_PARTIAL). 2단계에 더해 한글은 초성을, 영문은 단어마다 일부 글자를 무작위로 보여준다.
     */
    public static String revealPartial(String answer, RandomGenerator random) {
        int[] answerChars = answer.codePoints().toArray();
        int[] revealed = Arrays.stream(answerChars)
                .map(c -> switch (AnswerCharKind.of(c)) {
                    case HANGUL -> choseong(c);
                    case ENGLISH -> MASK;
                    default -> c;
                })
                .toArray();
        for (List<Integer> word : englishWords(answerChars)) {
            for (int index : pickIndices(word, partialRevealCount(word.size()), random)) {
                revealed[index] = answerChars[index];
            }
        }
        return toString(revealed);
    }

    /**
     * 4단계(ANSWER_RANDOM_CHAR). 직전 공개 내용에서 원래 글자가 보이지 않는 자리 하나를 무작위로 원래 글자로 바꾼다.
     * 공개한 뒤에도 그런 자리가 하나 이상 남아야 하므로, 1개 이하만 남았으면 빈 값이다.
     */
    public static Optional<String> revealRandomChar(String answer, String previousContent, RandomGenerator random) {
        int[] answerChars = answer.codePoints().toArray();
        int[] revealed = previousContent.codePoints().toArray();
        if (answerChars.length != revealed.length) {
            throw new IllegalArgumentException("직전 공개 내용과 정답의 글자 수가 다릅니다.");
        }
        List<Integer> hidden = new ArrayList<>();
        for (int i = 0; i < answerChars.length; i++) {
            if (revealed[i] != answerChars[i]) {
                hidden.add(i);
            }
        }
        if (hidden.size() <= 1) {
            return Optional.empty();
        }
        int index = hidden.get(random.nextInt(hidden.size()));
        revealed[index] = answerChars[index];
        return Optional.of(toString(revealed));
    }

    /**
     * 영문 단어 하나에서 공개할 글자 수. 글자 수 ÷ 3 반올림이고, 두 글자 이하는 전부 공개한다.
     */
    static int partialRevealCount(int wordLength) {
        if (wordLength <= WHOLE_REVEAL_MAX_LENGTH) {
            return wordLength;
        }
        return Math.round((float) wordLength / PARTIAL_REVEAL_DIVISOR);
    }

    // 띄어쓰기·숫자·특수문자가 없으면 2단계는 1단계보다 새로 알려주는 게 없다.
    private static boolean hasSymbolStageInfo(String answer) {
        return answer.codePoints().anyMatch(c -> !isLetter(c));
    }

    private static boolean isLetter(int codePoint) {
        AnswerCharKind kind = AnswerCharKind.of(codePoint);
        return kind == AnswerCharKind.HANGUL || kind == AnswerCharKind.ENGLISH;
    }

    private static String mark(List<AnswerCharKind> kinds, AnswerCharKind kind) {
        return kinds.contains(kind) ? "O" : "X";
    }

    private static int choseong(int syllable) {
        return CHOSEONG.charAt((syllable - AnswerCharKind.HANGUL_FIRST) / SYLLABLES_PER_CHOSEONG);
    }

    private static List<List<Integer>> englishWords(int[] chars) {
        List<List<Integer>> words = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        for (int i = 0; i < chars.length; i++) {
            if (AnswerCharKind.of(chars[i]) == AnswerCharKind.ENGLISH) {
                current.add(i);
            } else if (!current.isEmpty()) {
                words.add(current);
                current = new ArrayList<>();
            }
        }
        if (!current.isEmpty()) {
            words.add(current);
        }
        return words;
    }

    private static List<Integer> pickIndices(List<Integer> pool, int count, RandomGenerator random) {
        List<Integer> rest = new ArrayList<>(pool);
        List<Integer> picked = new ArrayList<>();
        while (picked.size() < count && !rest.isEmpty()) {
            picked.add(rest.remove(random.nextInt(rest.size())));
        }
        return picked;
    }

    private static String toString(int[] codePoints) {
        return Arrays.stream(codePoints)
                .mapToObj(Character::toString)
                .collect(Collectors.joining());
    }
}

package geniusneugul.project.core.game.common.implement;

import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.game.common.domain.HintType;
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

    // 2단계에서 직전 공개 내용과 자리별로 비교하려면 마스킹 문자가 코드 포인트 하나여야 한다(⚫️처럼 이모지 선택자를 붙이지 않는다).
    private static final int MASK = 0x26AB;

    private static final String CHOSEONG = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ";
    private static final int SYLLABLES_PER_CHOSEONG = 588;
    private static final int PARTIAL_REVEAL_DIVISOR = 3;

    // 세 게임이 같은 단계를 쓴다. 기본 힌트(ANSWER_MASK) → 1단계(ANSWER_PARTIAL) → 2단계~(ANSWER_RANDOM_CHAR)
    private static final List<HintType> STAGES =
            List.of(HintType.ANSWER_MASK, HintType.ANSWER_PARTIAL, HintType.ANSWER_RANDOM_CHAR);

    private AnswerHint() {
    }

    /**
     * 다음에 열 정답 힌트 단계와 공개 내용. 단계 순서를 따르고, 정답이 전부 드러나는 단계는 건너뛴다.
     *
     * @param previous 이 라운드에서 마지막으로 공개된 정답 힌트. 아직 없으면 null
     * @return 더 열 단계가 없으면 빈 값
     */
    public static Optional<AnswerHintContent> next(String answer, AnswerHintContent previous, RandomGenerator random) {
        int start = 0;
        String revealBase = null;
        if (previous != null) {
            // 2단계는 반복해서 열 수 있으므로 2단계 다음도 2단계다.
            start = previous.type() == HintType.ANSWER_RANDOM_CHAR
                    ? STAGES.indexOf(HintType.ANSWER_RANDOM_CHAR)
                    : STAGES.indexOf(previous.type()) + 1;
            // 2단계는 1·2단계 공개 내용에서 한 글자를 더 연다.
            if (previous.type() == HintType.ANSWER_PARTIAL || previous.type() == HintType.ANSWER_RANDOM_CHAR) {
                revealBase = previous.content();
            }
        }

        for (HintType stage : STAGES.subList(start, STAGES.size())) {
            Optional<String> content = switch (stage) {
                case ANSWER_MASK -> Optional.of(mask(answer));
                case ANSWER_PARTIAL -> Optional.of(revealPartial(answer, random));
                case ANSWER_RANDOM_CHAR -> revealBase == null ? Optional.empty() : revealRandomChar(answer, revealBase, random);
                default -> throw new IllegalStateException(ErrorCode.GAME_NOT_ANSWER_HINT_STAGE.getMessage() + " stage=" + stage);
            };
            if (content.isPresent() && !content.get().equals(answer)) {
                return Optional.of(new AnswerHintContent(stage, content.get()));
            }
        }
        return Optional.empty();
    }

    /**
     * 기본 힌트(ANSWER_MASK). 띄어쓰기를 빼고 한글·영문·숫자는 가리고 특수문자는 보여준 뒤, 글자 수와 문자 종류 포함 여부를 붙인다.
     * 예: {@code ⚫⚫⚫⚫,⚫⚫ (7글자(특수문자 포함), 한글 O, 영어 X, 숫자 X, 특수문자 O)}
     */
    static String mask(String answer) {
        List<AnswerCharKind> kinds = answer.codePoints().mapToObj(AnswerCharKind::of)
                .filter(kind -> kind != AnswerCharKind.SPACE)
                .toList();
        String masked = answer.codePoints()
                .filter(c -> AnswerCharKind.of(c) != AnswerCharKind.SPACE)
                .map(c -> AnswerCharKind.of(c) == AnswerCharKind.SYMBOL ? c : MASK)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        return "%s (%d글자(특수문자 포함), 한글 %s, 영어 %s, 숫자 %s, 특수문자 %s)".formatted(
                masked, kinds.size(),
                mark(kinds, AnswerCharKind.HANGUL), mark(kinds, AnswerCharKind.ENGLISH),
                mark(kinds, AnswerCharKind.DIGIT), mark(kinds, AnswerCharKind.SYMBOL));
    }

    /**
     * 1단계(ANSWER_PARTIAL). 띄어쓰기·숫자·특수문자를 보여주고, 한글은 초성을 보여준다.
     * 영문은 정답 전체의 영문 글자 중 일부를 무작위로 보여준다.
     */
    static String revealPartial(String answer, RandomGenerator random) {
        int[] answerChars = answer.codePoints().toArray();
        int[] revealed = Arrays.stream(answerChars)
                .map(c -> switch (AnswerCharKind.of(c)) {
                    case HANGUL -> choseong(c);
                    case ENGLISH -> MASK;
                    default -> c;
                })
                .toArray();
        List<Integer> englishIndices = englishIndices(answerChars);
        for (int index : pickIndices(englishIndices, partialRevealCount(englishIndices.size()), random)) {
            revealed[index] = answerChars[index];
        }
        return fromCodePoints(revealed);
    }

    /**
     * 2단계(ANSWER_RANDOM_CHAR). 직전 공개 내용에서 원래 글자가 보이지 않는 자리 하나를 무작위로 원래 글자로 바꾼다.
     * 공개한 뒤에도 그런 자리가 하나 이상 남아야 하므로, 1개 이하만 남았으면 빈 값이다.
     */
    static Optional<String> revealRandomChar(String answer, String previousContent, RandomGenerator random) {
        int[] answerChars = answer.codePoints().toArray();
        int[] revealed = previousContent.codePoints().toArray();
        if (answerChars.length != revealed.length) {
            throw new IllegalArgumentException(ErrorCode.GAME_ANSWER_HINT_LENGTH_MISMATCH.getMessage()
                    + " answerLength=" + answerChars.length + " previousLength=" + revealed.length);
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
        return Optional.of(fromCodePoints(revealed));
    }

    /**
     * 1단계에서 공개할 영문 글자 수. 정답 전체의 영문 글자 수(띄어쓰기·숫자·특수문자·한글 제외) ÷ 3, 반올림.
     */
    static int partialRevealCount(int englishLength) {
        return Math.round((float) englishLength / PARTIAL_REVEAL_DIVISOR);
    }

    private static String mark(List<AnswerCharKind> kinds, AnswerCharKind kind) {
        return kinds.contains(kind) ? "O" : "X";
    }

    private static int choseong(int syllable) {
        return CHOSEONG.charAt((syllable - AnswerCharKind.HANGUL_FIRST) / SYLLABLES_PER_CHOSEONG);
    }

    private static List<Integer> englishIndices(int[] chars) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < chars.length; i++) {
            if (AnswerCharKind.of(chars[i]) == AnswerCharKind.ENGLISH) {
                indices.add(i);
            }
        }
        return indices;
    }

    private static List<Integer> pickIndices(List<Integer> pool, int count, RandomGenerator random) {
        List<Integer> rest = new ArrayList<>(pool);
        List<Integer> picked = new ArrayList<>();
        while (picked.size() < count && !rest.isEmpty()) {
            picked.add(rest.remove(random.nextInt(rest.size())));
        }
        return picked;
    }

    private static String fromCodePoints(int[] codePoints) {
        return Arrays.stream(codePoints)
                .mapToObj(Character::toString)
                .collect(Collectors.joining());
    }
}

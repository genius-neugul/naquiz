package geniusneugul.project.core.game.common.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * 한 라운드의 정답 힌트 공개 상태. 정답을 글자 단위로 나눠 글자마다 공개 상태를 갖고, 마지막으로 공개한 우선순위를 기억한다.
 * 진행 중에만 쓰므로 메모리에 둔다. 스스로 동기화하지 않으므로 방 락 안에서만 읽고 바꾼다.
 */
@Getter
public class AnswerHintString {

    private static final int PARTIAL_REVEAL_DIVISOR = 3;

    private final Long roomId;
    private final Long gameId;
    private final int roundNo;
    private final List<AnswerHintCharacter> characters;
    private AnswerHintPriority current;

    private AnswerHintString(Long roomId, Long gameId, int roundNo, List<AnswerHintCharacter> characters) {
        this.roomId = roomId;
        this.gameId = gameId;
        this.roundNo = roundNo;
        this.characters = characters;
        this.current = AnswerHintPriority.ANSWER_MASK;
    }

    /** 정답을 코드 포인트 단위로 잘라 기본 힌트(ANSWER_MASK) 상태로 만든다 */
    public static AnswerHintString of(Long roomId, Long gameId, int roundNo, String answer) {
        List<AnswerHintCharacter> characters = answer.codePoints()
                .mapToObj(Character::toString)
                .map(AnswerHintCharacter::new)
                .toList();
        return new AnswerHintString(roomId, gameId, roundNo, characters);
    }

    /**
     * 지금 우선순위 다음부터 열 수 있는 첫 우선순위를 공개한다. 앞선 우선순위는 다시 보지 않고, 열 수 없는 우선순위는 건너뛴다.
     *
     * @return 공개한 우선순위. 더 열 수 있는 우선순위가 없으면 빈 값
     */
    public Optional<AnswerHintPriority> revealNext(RandomGenerator random) {
        AnswerHintPriority candidate = current.next();
        while (!canReveal(candidate)) {
            if (candidate.next() == candidate) {
                return Optional.empty();
            }
            candidate = candidate.next();
        }
        reveal(candidate, random);
        current = candidate;
        return Optional.of(candidate);
    }

    /** 완전히 공개되지 않은 글자. 초성만 보이는 한글도 포함한다 */
    public List<AnswerHintCharacter> getUnRevealedCharacter() {
        return characters.stream().filter(character -> !character.isRevealed()).toList();
    }

    /** 지금 공개 내용. 기본 힌트면 글자 수와 종류 포함 여부를 붙인다. 예: {@code ⚫⚫ ⚫⚫⚫⚫ ⚫! (8글자(특수문자 포함), 한글 O, 영어 O, 숫자 O, 특수문자 O)} */
    public String content() {
        if (current == AnswerHintPriority.ANSWER_MASK) {
            return display() + " " + summary();
        }
        return display();
    }

    public boolean isFor(Long gameId, int roundNo) {
        return this.gameId.equals(gameId) && this.roundNo == roundNo;
    }

    private String display() {
        return characters.stream().map(AnswerHintCharacter::getDisplay).collect(Collectors.joining());
    }

    private String summary() {
        long letterCount = characters.stream().filter(character -> character.getKind() != CharacterKind.SPACE).count();
        return "(%d글자(특수문자 포함), 한글 %s, 영어 %s, 숫자 %s, 특수문자 %s)".formatted(letterCount,
                mark(CharacterKind.HANGUL), mark(CharacterKind.ALPHABET),
                mark(CharacterKind.NUMBER), mark(CharacterKind.SPECIAL_CHARACTER));
    }

    private String mark(CharacterKind kind) {
        return characters.stream().anyMatch(character -> character.getKind() == kind) ? "O" : "X";
    }

    private boolean canReveal(AnswerHintPriority priority) {
        return switch (priority) {
            // 기본 힌트는 만들 때 이미 공개된 상태다.
            case ANSWER_MASK -> false;
            case ANSWER_NUMBER -> hasMasked(CharacterKind.NUMBER);
            case ANSWER_CHOSUNG -> hasMasked(CharacterKind.HANGUL);
            case ANSWER_PARTIAL -> !getUnRevealedCharacter().isEmpty();
        };
    }

    private void reveal(AnswerHintPriority priority, RandomGenerator random) {
        switch (priority) {
            case ANSWER_MASK -> {
            }
            case ANSWER_NUMBER -> maskedOf(CharacterKind.NUMBER).forEach(AnswerHintCharacter::reveal);
            case ANSWER_CHOSUNG -> maskedOf(CharacterKind.HANGUL).forEach(AnswerHintCharacter::showChosung);
            case ANSWER_PARTIAL -> revealPartial(random);
        }
    }

    /** 완전히 공개되지 않은 글자 수 ÷ 3(반올림)개를 무작위로 공개한다. 1개만 남았으면 그 1개를 공개한다 */
    private void revealPartial(RandomGenerator random) {
        List<AnswerHintCharacter> rest = new ArrayList<>(getUnRevealedCharacter());
        int count = Math.max(1, Math.round((float) rest.size() / PARTIAL_REVEAL_DIVISOR));
        for (int i = 0; i < count; i++) {
            rest.remove(random.nextInt(rest.size())).reveal();
        }
    }

    private boolean hasMasked(CharacterKind kind) {
        return !maskedOf(kind).isEmpty();
    }

    private List<AnswerHintCharacter> maskedOf(CharacterKind kind) {
        return characters.stream()
                .filter(character -> character.getKind() == kind && character.getState() == CharacterState.MASK)
                .toList();
    }
}

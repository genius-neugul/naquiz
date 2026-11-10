package geniusneugul.project.core.game.common.domain;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * 한 라운드의 정답 힌트 공개 상태. 정답을 글자 단위로 나눠 글자마다 공개 상태를 갖고, 마지막으로 공개한 우선순위를 기억한다.
 * 우선순위별 공개 동작은 implement의 AnswerHintPriorityRevealer가 맡는다.
 * 진행 중에만 쓰므로 메모리에 둔다. 스스로 동기화하지 않으므로 방 락 안에서만 읽고 바꾼다.
 */
@Getter
public class AnswerHintString {

    private final Long roomId;
    private final Long gameId;
    private final int roundNo;
    private final List<AnswerHintCharacter> characters;
    /** 마지막으로 공개한 우선순위. 아직 아무것도 공개하지 않았으면 null */
    private AnswerHintPriority current;

    private AnswerHintString(Long roomId, Long gameId, int roundNo, List<AnswerHintCharacter> characters) {
        this.roomId = roomId;
        this.gameId = gameId;
        this.roundNo = roundNo;
        this.characters = characters;
    }

    /** 정답을 코드 포인트 단위로 잘라 모든 글자를 가린 상태로 만든다. 기본 힌트는 아직 공개하지 않은 상태다 */
    public static AnswerHintString of(Long roomId, Long gameId, int roundNo, String answer) {
        List<AnswerHintCharacter> characters = answer.codePoints()
                .mapToObj(Character::toString)
                .map(AnswerHintCharacter::new)
                .toList();
        return new AnswerHintString(roomId, gameId, roundNo, characters);
    }

    /** 우선순위 하나를 공개한 뒤 마지막으로 공개한 우선순위를 바꾼다 */
    public void changeCurrent(AnswerHintPriority priority) {
        this.current = priority;
    }

    /** 완전히 공개되지 않은 글자. 초성만 보이는 한글도 포함한다 */
    public List<AnswerHintCharacter> getUnRevealedCharacter() {
        return characters.stream().filter(character -> !character.isRevealed()).toList();
    }

    /** 아직 가려진(⚫) 그 종류의 글자 */
    public List<AnswerHintCharacter> maskedOf(CharacterKind kind) {
        return characters.stream()
                .filter(character -> character.getKind() == kind && character.getState() == CharacterState.MASK)
                .toList();
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
}

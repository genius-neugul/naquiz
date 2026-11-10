package geniusneugul.project.core.game.common.domain;

import lombok.Getter;

/**
 * 정답 힌트의 글자 하나. 원래 글자와 지금 화면에 보이는 값을 함께 가진다.
 * 원래 글자가 그대로 보일 때만 완전히 공개된 것이고, 초성만 보이는 한글은 아직 공개되지 않은 것이다.
 */
@Getter
public class AnswerHintCharacter {

    static final String MASK = "⚫";

    private static final String CHOSUNG = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ";
    // 초성 하나에 딸린 완성형 음절 수(중성 21 × 종성 28)
    private static final int SYLLABLES_PER_CHOSUNG = 588;

    private final String original;
    private final CharacterKind kind;
    private CharacterState state;
    private String display;

    /** 모든 글자를 가린 채로 만든다. 기본 힌트에서 무엇을 보여줄지는 공개 우선순위가 정한다. original은 코드 포인트 하나로 된 글자다 */
    AnswerHintCharacter(String original) {
        this.original = original;
        this.kind = CharacterKind.of(original);
        this.state = CharacterState.MASK;
        this.display = MASK;
    }

    public boolean isRevealed() {
        return state == CharacterState.REVEALED;
    }

    /** 한글을 초성으로 보여준다. 완전히 공개된 것은 아니다 */
    public void showChosung() {
        this.state = CharacterState.CHOSUNG;
        this.display = String.valueOf(CHOSUNG.charAt((original.charAt(0) - CharacterKind.HANGUL_FIRST) / SYLLABLES_PER_CHOSUNG));
    }

    /** 원래 글자를 그대로 보여준다 */
    public void reveal() {
        this.state = CharacterState.REVEALED;
        this.display = original;
    }
}

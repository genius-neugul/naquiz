package geniusneugul.project.core.game.common.domain;

/**
 * 정답 힌트에서 글자 하나가 화면에 보이는 상태. 가림(⚫) → 초성(한글만) → 원래 글자 순으로만 바뀐다.
 */
public enum CharacterState {
    MASK,
    CHOSUNG,
    REVEALED
}

package geniusneugul.project.core.game.common.domain;

/**
 * 힌트 종류. 세 게임의 코드를 모두 담는다. 정답 힌트(ANSWER_*)는 세 게임이 함께 쓴다.
 */
public enum HintType {
    // 정답 힌트. 기본 힌트(ANSWER_MASK) → 1단계(ANSWER_PARTIAL) → 2단계~(ANSWER_RANDOM_CHAR)
    ANSWER_MASK,
    ANSWER_PARTIAL,
    ANSWER_RANDOM_CHAR,

    // 노래 맞추기
    ALBUM,
    ARTIST,
    RELEASE_DATE,

    // 스틸컷 영화 맞추기
    STILL_CUT;

    public boolean isAnswerHint() {
        return this == ANSWER_MASK || this == ANSWER_PARTIAL || this == ANSWER_RANDOM_CHAR;
    }
}

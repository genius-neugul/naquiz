package geniusneugul.project.core.game.domain;

/**
 * 힌트 종류. 세 게임의 코드를 모두 담는다. ANSWER_INITIAL은 영화 스무고개와 스틸컷이 함께 쓴다.
 */
public enum HintType {
    // 노래 맞추기
    ANSWER_MASK,
    ANSWER_SYMBOL,
    ANSWER_PARTIAL,
    ALBUM,
    ARTIST,
    RELEASE_DATE,

    // 영화 스무고개
    ANSWER_LENGTH,
    ANSWER_INITIAL,
    ANSWER_RANDOM_CHAR,

    // 스틸컷 영화 맞추기
    STILL_CUT
}

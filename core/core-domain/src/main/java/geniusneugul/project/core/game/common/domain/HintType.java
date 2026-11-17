package geniusneugul.project.core.game.common.domain;

/**
 * 힌트 종류. 세 게임의 코드를 모두 담는다. 정답 힌트(ANSWER_HINT)는 세 게임이 함께 쓰고,
 * 그 안의 공개 순서는 AnswerHintPriority가 정한다.
 */
public enum HintType {
    // 세 게임 공통
    ANSWER_HINT,

    // 노래 맞추기
    ALBUM,
    ARTIST,
    RELEASE_DATE,

    // 스틸컷 영화 맞추기
    STILL_CUT
}

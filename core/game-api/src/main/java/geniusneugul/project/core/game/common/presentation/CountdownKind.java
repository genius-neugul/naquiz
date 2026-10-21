package geniusneugul.project.core.game.common.presentation;

/**
 * 서버 카운트다운 종류. 한 방에서 종류마다 하나만 돈다.
 */
public enum CountdownKind {
    /** 정답 공개 뒤 다음 라운드까지 */
    NEXT_ROUND,
    /** 스틸컷 게임의 다음 스틸컷까지 */
    STILL_CUT
}

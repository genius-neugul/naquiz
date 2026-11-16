package geniusneugul.project.core.game.common.presentation;

/**
 * 서버 카운트다운 종류. 한 방에서 종류마다 하나만 돈다.
 */
public enum CountdownKind {
    /** 정답 공개 뒤 다음 라운드까지 */
    NEXT_ROUND,
    /** 스틸컷 게임의 다음 스틸컷까지 */
    STILL_CUT,
    /** 영화 스무고개에서 차례 참가자가 단서를 고르는 시간 */
    TURN,
    /** 영화 스무고개에서 고른 단서를 공개하기까지 */
    CLUE_REVEAL
}

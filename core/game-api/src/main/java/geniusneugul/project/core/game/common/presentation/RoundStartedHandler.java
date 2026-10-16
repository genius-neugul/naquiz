package geniusneugul.project.core.game.common.presentation;

import geniusneugul.project.core.common.domain.GameType;

/**
 * 공통 게임 흐름이 게임별 처리를 부르는 확장점. 게임 패키지(game/stillcut 등)가 구현한다.
 * 공통 흐름은 게임 패키지를 직접 참조하지 않는다(docs/ARCHITECTURE.md 「기본 패키지 구조」).
 */
public interface RoundStartedHandler {

    /** 처리할 게임 종류 */
    boolean supports(GameType gameType);

    /** 라운드가 열려 ROUND_STARTED를 보낸 직후 */
    void onRoundStarted(StartedRound round);

    /** 라운드가 끝나 정답(ROUND_SOLVED)을 보낸 직후. 게임이 끝나는 라운드면 GAME_FINISHED보다 먼저 부른다 */
    default void onRoundEnded(EndedRound round) {
    }

    /** 승자가 나와 게임이 끝났다. 게임별 진행 상태를 정리한다 */
    void onGameFinished(Long roomId);
}

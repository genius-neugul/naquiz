package geniusneugul.project.core.game.common.presentation;

import geniusneugul.project.core.game.common.service.GameStartResult;
import geniusneugul.project.core.game.common.service.RoundStartResult;

/**
 * 막 열린 라운드. 게임별 처리(RoundStartedHandler)에 넘긴다.
 */
public record StartedRound(Long roomId, Long gameId, String gameType, int roundNo, Long questionId) {

    static StartedRound from(GameStartResult result) {
        return new StartedRound(result.roomId(), result.gameId(), result.gameType(), result.roundNo(), result.questionId());
    }

    static StartedRound from(RoundStartResult result) {
        return new StartedRound(result.roomId(), result.gameId(), result.gameType(), result.roundNo(), result.questionId());
    }
}

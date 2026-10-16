package geniusneugul.project.core.game.common.presentation;

import geniusneugul.project.core.common.domain.GameType;

/**
 * 막 끝난 라운드. 게임별 처리(RoundStartedHandler)에 넘긴다.
 */
public record EndedRound(Long roomId, Long gameId, GameType gameType, int roundNo, Long questionId) {

    static EndedRound from(SolvedRound result) {
        return new EndedRound(result.roomId(), result.gameId(), result.gameType(), result.roundNo(), result.questionId());
    }
}

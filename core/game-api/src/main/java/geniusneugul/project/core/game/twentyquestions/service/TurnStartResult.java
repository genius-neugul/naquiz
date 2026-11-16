package geniusneugul.project.core.game.twentyquestions.service;

import geniusneugul.project.core.game.twentyquestions.implement.TurnStart;

/**
 * 새 차례가 시작됐다.
 */
public record TurnStartResult(
        Long roomId,
        Long gameId,
        int roundNo,
        int turnNo,
        Long participantId
) {

    static TurnStartResult from(TurnStart start) {
        return new TurnStartResult(start.roomId(), start.gameId(), start.roundNo(), start.turnNo(),
                start.participantId());
    }
}

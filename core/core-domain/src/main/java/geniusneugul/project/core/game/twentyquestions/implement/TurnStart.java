package geniusneugul.project.core.game.twentyquestions.implement;

import geniusneugul.project.core.game.twentyquestions.domain.ClueBoard;

/**
 * 새 차례가 시작됐다. 방 락 안에서 만든 스냅샷이다.
 */
public record TurnStart(
        Long roomId,
        Long gameId,
        int roundNo,
        int turnNo,
        Long participantId
) {

    static TurnStart of(ClueBoard board) {
        return new TurnStart(board.getRoomId(), board.getGameId(), board.getRoundNo(), board.getTurnNo(),
                board.getTurnHolderId());
    }
}
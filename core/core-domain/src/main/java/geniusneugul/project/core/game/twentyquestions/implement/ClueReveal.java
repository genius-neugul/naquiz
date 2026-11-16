package geniusneugul.project.core.game.twentyquestions.implement;

import geniusneugul.project.core.game.twentyquestions.domain.ClueBoard;
import geniusneugul.project.core.game.twentyquestions.domain.ClueType;
import geniusneugul.project.core.game.twentyquestions.domain.ShownClue;
import java.time.LocalDateTime;

/**
 * 차례 참가자가 단서 하나를 연 결과. nextTurn은 이어서 시작한 차례이고, 단서를 모두 열었으면 null이다.
 * 방 락 안에서 만든 스냅샷이다.
 */
public record ClueReveal(
        Long roomId,
        Long gameId,
        int roundNo,
        ClueType clueType,
        String content,
        Long participantId,
        int order,
        int total,
        LocalDateTime revealedAt,
        TurnStart nextTurn
) {

    static ClueReveal of(ClueBoard board, ShownClue shown, Long participantId, LocalDateTime revealedAt,
            TurnStart nextTurn) {
        return new ClueReveal(board.getRoomId(), board.getGameId(), board.getRoundNo(), shown.clueType(),
                shown.content(), participantId, shown.order(), shown.total(), revealedAt, nextTurn);
    }
}

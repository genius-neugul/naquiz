package geniusneugul.project.core.game.twentyquestions.implement;

import geniusneugul.project.core.game.twentyquestions.domain.ClueBoard;
import geniusneugul.project.core.game.twentyquestions.domain.ClueType;
import java.time.LocalDateTime;

/**
 * 차례 참가자의 단서가 골라졌다. 내용은 잠시 뒤 공개한다. timedOut이면 시간 안에 고르지 않아 서버가 무작위로 골랐다.
 * 방 락 안에서 만든 스냅샷이다.
 */
public record ClueSelect(
        Long roomId,
        Long gameId,
        int roundNo,
        ClueType clueType,
        Long participantId,
        boolean timedOut,
        LocalDateTime selectedAt
) {

    static ClueSelect of(ClueBoard board, boolean timedOut, LocalDateTime selectedAt) {
        return new ClueSelect(board.getRoomId(), board.getGameId(), board.getRoundNo(), board.getSelectedClue(),
                board.getSelectedById(), timedOut, selectedAt);
    }
}
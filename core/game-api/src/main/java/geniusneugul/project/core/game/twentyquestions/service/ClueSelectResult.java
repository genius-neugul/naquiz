package geniusneugul.project.core.game.twentyquestions.service;

import geniusneugul.project.core.game.twentyquestions.implement.ClueSelect;
import java.time.LocalDateTime;

/**
 * 차례 참가자의 단서가 골라졌다. 내용은 잠시 뒤 공개한다. timedOut이면 시간 안에 고르지 않아 서버가 무작위로 골랐다.
 */
public record ClueSelectResult(
        Long roomId,
        Long gameId,
        int roundNo,
        String clueType,
        Long participantId,
        boolean timedOut,
        LocalDateTime selectedAt
) {

    static ClueSelectResult from(ClueSelect select) {
        return new ClueSelectResult(select.roomId(), select.gameId(), select.roundNo(), select.clueType().name(),
                select.participantId(), select.timedOut(), select.selectedAt());
    }
}

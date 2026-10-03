package geniusneugul.project.core.game.service;

import geniusneugul.project.core.game.implement.RoundStart;
import java.time.LocalDateTime;

/**
 * 다음 라운드가 열렸다. 정답은 담지 않는다.
 */
public record RoundStartResult(Long roomId, Long gameId, int roundNo, LocalDateTime startedAt) {

    static RoundStartResult from(RoundStart start) {
        return new RoundStartResult(start.roomId(), start.gameId(), start.roundNo(), start.startedAt());
    }
}

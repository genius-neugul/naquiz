package geniusneugul.project.core.game.common.service;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.game.common.implement.RoundStart;
import java.time.LocalDateTime;

/**
 * 다음 라운드가 열렸다. 정답은 담지 않는다. questionId는 게임별 처리(스틸컷 등)용이고 화면에 보내지 않는다.
 */
public record RoundStartResult(
        Long roomId,
        Long gameId,
        GameType gameType,
        int roundNo,
        Long questionId,
        LocalDateTime startedAt
) {

    static RoundStartResult from(RoundStart start) {
        return new RoundStartResult(start.roomId(), start.gameId(), start.gameType(), start.roundNo(),
                start.questionId(), start.startedAt());
    }
}

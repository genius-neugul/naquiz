package geniusneugul.project.core.game.presentation.dto;

import geniusneugul.project.core.game.service.GameStartResult;
import geniusneugul.project.core.game.service.RoundStartResult;
import java.time.LocalDateTime;

/**
 * 방 토픽으로 나가는 라운드 시작 이벤트. 정답은 담지 않는다.
 */
public record RoundStartedResponse(String type, Long gameId, int roundNo, LocalDateTime startedAt) {

    private static final String TYPE = "ROUND_STARTED";

    public static RoundStartedResponse from(GameStartResult result) {
        return new RoundStartedResponse(TYPE, result.gameId(), result.roundNo(), result.roundStartedAt());
    }

    public static RoundStartedResponse from(RoundStartResult result) {
        return new RoundStartedResponse(TYPE, result.gameId(), result.roundNo(), result.startedAt());
    }
}

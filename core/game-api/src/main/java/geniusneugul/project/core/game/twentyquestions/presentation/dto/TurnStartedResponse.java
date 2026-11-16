package geniusneugul.project.core.game.twentyquestions.presentation.dto;

import geniusneugul.project.core.game.twentyquestions.service.TurnStartResult;
import java.time.LocalDateTime;

/**
 * 방 토픽으로 나가는 차례 시작 이벤트. deadline까지 단서를 고르지 않으면 서버가 남은 단서 중 하나를 무작위로 고른다.
 */
public record TurnStartedResponse(
        String type,
        Long gameId,
        int roundNo,
        Long participantId,
        LocalDateTime deadline
) {

    private static final String TYPE = "TURN_STARTED";

    public static TurnStartedResponse of(TurnStartResult result, LocalDateTime deadline) {
        return new TurnStartedResponse(TYPE, result.gameId(), result.roundNo(), result.participantId(), deadline);
    }
}

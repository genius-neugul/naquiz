package geniusneugul.project.core.game.common.presentation.dto;

import geniusneugul.project.core.game.common.presentation.SkippedRound;
import java.time.LocalDateTime;

/**
 * 방 토픽으로 나가는 라운드 스킵 이벤트. 스킵 투표가 통과해 정답자 없이 끝난 라운드의 정답을 공개한다.
 * nextRoundAt은 다음 라운드가 열릴 시각이다.
 */
public record RoundSkippedResponse(
        String type,
        Long gameId,
        int roundNo,
        String answer,
        String subAnswer,
        LocalDateTime skippedAt,
        LocalDateTime nextRoundAt
) {

    private static final String TYPE = "ROUND_SKIPPED";

    public static RoundSkippedResponse of(SkippedRound result, LocalDateTime nextRoundAt) {
        return new RoundSkippedResponse(TYPE, result.gameId(), result.roundNo(), result.answer(), result.subAnswer(),
                result.skippedAt(), nextRoundAt);
    }
}

package geniusneugul.project.core.game.common.presentation;

import java.time.LocalDateTime;

/**
 * 알릴 라운드 스킵 한 건. 스킵은 투표로 정해지므로 투표 presentation이 이 값으로 바꿔 넘긴다.
 */
public record SkippedRound(
        Long roomId,
        Long gameId,
        int roundNo,
        String answer,
        String subAnswer,
        LocalDateTime skippedAt
) {
}

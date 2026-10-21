package geniusneugul.project.core.game.stillcut.presentation.dto;

import geniusneugul.project.core.game.stillcut.service.StillCutRevealResult;
import java.time.LocalDateTime;

/**
 * 방 토픽으로 나가는 스틸컷 공개 이벤트. 새 스틸컷은 이전 스틸컷을 대체한다.
 * order는 1부터 센 공개 순서이고, nextAt은 다음 스틸컷이 나올 시각이다(마지막 컷이면 null).
 */
public record StillCutRevealedResponse(
        String type,
        Long gameId,
        int roundNo,
        int order,
        int total,
        String imageUrl,
        LocalDateTime revealedAt,
        LocalDateTime nextAt
) {

    private static final String TYPE = "STILL_CUT_REVEALED";

    public static StillCutRevealedResponse of(StillCutRevealResult result, LocalDateTime nextAt) {
        return new StillCutRevealedResponse(TYPE, result.gameId(), result.roundNo(), result.order(), result.total(),
                result.imageUrl(), result.revealedAt(), nextAt);
    }
}

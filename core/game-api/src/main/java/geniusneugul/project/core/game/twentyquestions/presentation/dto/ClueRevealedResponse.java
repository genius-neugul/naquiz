package geniusneugul.project.core.game.twentyquestions.presentation.dto;

import geniusneugul.project.core.game.twentyquestions.service.ClueRevealResult;
import java.time.LocalDateTime;

/**
 * 방 토픽으로 나가는 단서 공개 이벤트. order는 라운드 안에서 1부터 센 공개 순서, total은 단서 종류 수다.
 */
public record ClueRevealedResponse(
        String type,
        Long gameId,
        int roundNo,
        String clueType,
        String content,
        Long revealedById,
        int order,
        int total,
        LocalDateTime revealedAt
) {

    private static final String TYPE = "CLUE_REVEALED";

    public static ClueRevealedResponse from(ClueRevealResult result) {
        return new ClueRevealedResponse(TYPE, result.gameId(), result.roundNo(), result.clueType(), result.content(),
                result.participantId(), result.order(), result.total(), result.revealedAt());
    }
}

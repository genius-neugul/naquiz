package geniusneugul.project.core.game.twentyquestions.service;

import geniusneugul.project.core.game.twentyquestions.implement.ClueReveal;
import java.time.LocalDateTime;

/**
 * 단서 하나를 열었다. nextTurn은 이어서 시작한 차례이고, 단서를 모두 열었으면 null이다.
 */
public record ClueRevealResult(
        Long roomId,
        Long gameId,
        int roundNo,
        String clueType,
        String content,
        Long participantId,
        int order,
        int total,
        LocalDateTime revealedAt,
        TurnStartResult nextTurn
) {

    static ClueRevealResult from(ClueReveal reveal) {
        TurnStartResult nextTurn = reveal.nextTurn() == null ? null : TurnStartResult.from(reveal.nextTurn());
        return new ClueRevealResult(reveal.roomId(), reveal.gameId(), reveal.roundNo(), reveal.clueType().name(),
                reveal.content(), reveal.participantId(), reveal.order(), reveal.total(), reveal.revealedAt(), nextTurn);
    }
}

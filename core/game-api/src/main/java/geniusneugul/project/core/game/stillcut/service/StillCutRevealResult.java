package geniusneugul.project.core.game.stillcut.service;

import geniusneugul.project.core.game.stillcut.implement.StillCutReveal;
import java.time.LocalDateTime;

/**
 * 스틸컷 하나를 공개했다. hasNext면 다음 컷이 남았다.
 */
public record StillCutRevealResult(
        Long roomId,
        Long gameId,
        int roundNo,
        int order,
        int total,
        String imageUrl,
        boolean hasNext,
        LocalDateTime revealedAt
) {

    static StillCutRevealResult from(StillCutReveal reveal) {
        return new StillCutRevealResult(reveal.roomId(), reveal.gameId(), reveal.roundNo(), reveal.order(),
                reveal.total(), reveal.imageUrl(), reveal.hasNext(), reveal.revealedAt());
    }
}

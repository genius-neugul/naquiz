package geniusneugul.project.core.game.stillcut.implement;

import geniusneugul.project.core.game.stillcut.domain.ShownStillCut;
import geniusneugul.project.core.game.stillcut.domain.StillCutReel;
import java.time.LocalDateTime;

/**
 * 스틸컷 하나를 공개한 결과. hasNext면 다음 컷이 남았다. 방 락 안에서 만든 스냅샷이다.
 */
public record StillCutReveal(
        Long roomId,
        Long gameId,
        int roundNo,
        int order,
        int total,
        String imageUrl,
        boolean hasNext,
        LocalDateTime revealedAt
) {

    static StillCutReveal of(StillCutReel reel, ShownStillCut shown, LocalDateTime revealedAt) {
        return new StillCutReveal(reel.getRoomId(), reel.getGameId(), reel.getRoundNo(), shown.order(), shown.total(),
                shown.imageUrl(), reel.hasNext(), revealedAt);
    }
}

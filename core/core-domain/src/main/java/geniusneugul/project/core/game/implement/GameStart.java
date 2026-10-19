package geniusneugul.project.core.game.implement;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.room.implement.ParticipantScore;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 게임이 시작되고 첫 라운드가 열린 결과. 방 락 안에서 만든 스냅샷이다.
 */
public record GameStart(
        Long roomId,
        Long gameId,
        GameType gameType,
        int targetScore,
        int roundNo,
        LocalDateTime roundStartedAt,
        List<ParticipantScore> scores
) {
}

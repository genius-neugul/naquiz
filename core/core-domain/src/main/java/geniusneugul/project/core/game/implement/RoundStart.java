package geniusneugul.project.core.game.implement;

import java.time.LocalDateTime;

/**
 * 다음 라운드가 열린 결과. 방 락 안에서 만든 스냅샷이다.
 */
public record RoundStart(Long roomId, Long gameId, int roundNo, Long questionId, LocalDateTime startedAt) {
}

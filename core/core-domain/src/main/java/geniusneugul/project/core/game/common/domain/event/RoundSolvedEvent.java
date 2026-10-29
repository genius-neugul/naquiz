package geniusneugul.project.core.game.common.domain.event;

import geniusneugul.project.core.common.domain.event.DomainEvent;
import java.time.LocalDateTime;

/**
 * 정답자가 나와 라운드가 끝났다. 통과하지 못한 투표는 만료된다(docs/DOMAIN.md 3-4).
 */
public record RoundSolvedEvent(Long roomId, Long gameId, int roundNo, LocalDateTime occurredAt) implements DomainEvent {
}

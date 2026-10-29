package geniusneugul.project.core.room.domain.event;

import geniusneugul.project.core.common.domain.event.DomainEvent;
import java.time.LocalDateTime;

/**
 * 방장이 나가 방이 닫혔다. 진행 중이던 게임은 승자 없이 끝난다(docs/DOMAIN.md 3-1).
 */
public record RoomClosedEvent(Long roomId, LocalDateTime occurredAt) implements DomainEvent {
}

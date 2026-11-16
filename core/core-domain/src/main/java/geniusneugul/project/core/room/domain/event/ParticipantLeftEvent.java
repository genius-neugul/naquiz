package geniusneugul.project.core.room.domain.event;

import geniusneugul.project.core.common.domain.event.DomainEvent;
import java.time.LocalDateTime;

/**
 * 게스트가 방을 나갔다. 방장이 나가 방이 닫힌 경우는 RoomClosedEvent로 따로 알린다.
 */
public record ParticipantLeftEvent(Long roomId, Long participantId, LocalDateTime occurredAt) implements DomainEvent {
}

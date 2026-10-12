package geniusneugul.project.core.room.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import geniusneugul.project.core.room.service.LeaveResult;
import geniusneugul.project.core.room.service.RoomResult;
import java.util.List;

/**
 * 방 토픽(/topic/rooms/{roomId})으로 나가는 이벤트. participantId는 들어오거나 나간 참가자이고, ROOM_CLOSED에는 participants가 없다.
 * 같은 토픽으로 채팅(type CHAT)도 나간다(chat/presentation/dto/ChatMessageResponse).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoomEventResponse(RoomEventType type, Long participantId, List<ParticipantResponse> participants) {

    public static RoomEventResponse joined(RoomResult result) {
        return new RoomEventResponse(RoomEventType.PARTICIPANT_JOINED, result.meId(),
                ParticipantResponse.listOf(result.participants()));
    }

    public static RoomEventResponse from(LeaveResult result) {
        if (result.closed()) {
            return new RoomEventResponse(RoomEventType.ROOM_CLOSED, result.participantId(), null);
        }
        return new RoomEventResponse(RoomEventType.PARTICIPANT_LEFT, result.participantId(),
                ParticipantResponse.listOf(result.participants()));
    }

    public enum RoomEventType {
        PARTICIPANT_JOINED,
        PARTICIPANT_LEFT,
        ROOM_CLOSED
    }
}

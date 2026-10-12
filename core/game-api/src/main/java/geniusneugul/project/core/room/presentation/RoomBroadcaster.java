package geniusneugul.project.core.room.presentation;

import geniusneugul.project.core.room.presentation.dto.RoomEventResponse;
import geniusneugul.project.core.room.service.LeaveResult;
import geniusneugul.project.core.room.service.RoomResult;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 방에 있는 참가자 모두에게 방 이벤트를 보낸다.
 */
@Component
@RequiredArgsConstructor
public class RoomBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastJoin(RoomResult result) {
        messagingTemplate.convertAndSend(topic(result.roomId()), RoomEventResponse.joined(result));
    }

    public void broadcastLeave(LeaveResult result) {
        messagingTemplate.convertAndSend(topic(result.roomId()), RoomEventResponse.from(result));
    }

    private String topic(Long roomId) {
        return "/topic/rooms/" + roomId;
    }
}

package geniusneugul.project.core.room.presentation;

import geniusneugul.project.core.room.service.RoomService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * 재접속을 지원하지 않으므로 연결이 끊기면 바로 방에서 내보낸다(docs/DOMAIN.md).
 */
@Component
@RequiredArgsConstructor
public class RoomDisconnectListener {

    private final RoomService roomService;
    private final RoomBroadcaster roomBroadcaster;

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal == null) {
            return;
        }
        roomService.leave(principal.getName()).ifPresent(roomBroadcaster::broadcastLeave);
    }
}

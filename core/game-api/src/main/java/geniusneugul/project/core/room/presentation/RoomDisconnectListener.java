package geniusneugul.project.core.room.presentation;

import geniusneugul.project.core.room.service.LeaveResult;
import geniusneugul.project.core.room.service.RoomService;
import geniusneugul.project.core.vote.presentation.VoteRejudger;
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
    private final VoteRejudger voteRejudger;

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal == null) {
            return;
        }
        roomService.leave(principal.getName()).ifPresent(this::announceLeave);
    }

    // 게스트가 나가 인원이 줄면 진행 중인 투표를 다시 판정한다. 퇴장을 먼저 알린다.
    private void announceLeave(LeaveResult result) {
        roomBroadcaster.broadcastLeave(result);
        if (!result.closed()) {
            voteRejudger.rejudge(result.roomId());
        }
    }
}

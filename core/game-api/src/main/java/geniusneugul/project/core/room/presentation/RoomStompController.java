package geniusneugul.project.core.room.presentation;

import geniusneugul.project.core.room.presentation.dto.CreateRoomRequest;
import geniusneugul.project.core.room.presentation.dto.JoinRoomRequest;
import geniusneugul.project.core.room.presentation.dto.RoomResponse;
import geniusneugul.project.core.room.service.RoomResult;
import geniusneugul.project.core.room.service.RoomService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

/**
 * 방 만들기·참가하기·나가기 실시간 메시지. 참가자는 연결의 Principal(참가자 토큰)로 식별한다.
 */
@Controller
@RequiredArgsConstructor
public class RoomStompController {

    private final RoomService roomService;
    private final RoomBroadcaster roomBroadcaster;

    @MessageMapping("/rooms/create")
    @SendToUser(destinations = "/queue/room", broadcast = false)
    public RoomResponse create(@Valid @Payload CreateRoomRequest request, Principal principal) {
        return RoomResponse.from(roomService.create(request.toCommand(principal.getName())));
    }

    /** 들어온 사람에게는 방 상태를, 이미 있던 참가자에게는 PARTICIPANT_JOINED를 보낸다 */
    @MessageMapping("/rooms/join")
    @SendToUser(destinations = "/queue/room", broadcast = false)
    public RoomResponse join(@Valid @Payload JoinRoomRequest request, Principal principal) {
        RoomResult result = roomService.join(request.toCommand(principal.getName()));
        roomBroadcaster.broadcastJoin(result);
        return RoomResponse.from(result);
    }

    @MessageMapping("/rooms/leave")
    public void leave(Principal principal) {
        roomService.leave(principal.getName()).ifPresent(roomBroadcaster::broadcastLeave);
    }
}

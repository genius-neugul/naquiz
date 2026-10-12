package geniusneugul.project.core.room.service;

import geniusneugul.project.core.room.domain.InviteCode;
import geniusneugul.project.core.room.domain.Room;
import geniusneugul.project.core.room.implement.InviteCodeIssuer;
import geniusneugul.project.core.room.implement.RoomAppender;
import geniusneugul.project.core.room.implement.RoomJoin;
import geniusneugul.project.core.room.implement.RoomJoiner;
import geniusneugul.project.core.room.implement.RoomLeaver;
import geniusneugul.project.core.room.implement.RoomParticipationValidator;
import geniusneugul.project.core.room.implement.RoomReader;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomParticipationValidator roomParticipationValidator;
    private final InviteCodeIssuer inviteCodeIssuer;
    private final RoomAppender roomAppender;
    private final RoomLeaver roomLeaver;
    private final RoomReader roomReader;
    private final RoomJoiner roomJoiner;

    public RoomResult create(CreateRoomCommand command) {
        roomParticipationValidator.validateNotJoined(command.participantToken());
        InviteCode inviteCode = inviteCodeIssuer.issue();
        Room room = roomAppender.append(inviteCode, command.nickname(), command.participantToken());

        // 초대 코드를 돌려주기 전이라 다른 참가자는 이 방에 닿을 수 없다. 방장 연결이 동시에 끊겨도
        // 방장 퇴장은 참가자 목록을 바꾸지 않으므로(상태만 CLOSED) 락 없이 읽어도 방장 정보는 그대로다.

        log.info("[RoomService.create] Room created. roomId={}, hostId={}", room.getId(), room.getHostId());
        return RoomResult.of(room, room.getHostId());
    }

    /** 초대 코드로 방에 들어간다. 결과의 meId는 들어온 참가자다 */
    public RoomResult join(JoinRoomCommand command) {
        roomParticipationValidator.validateNotJoined(command.participantToken());
        Room room = roomReader.readByInviteCode(InviteCode.from(command.inviteCode()));
        RoomJoin join = roomJoiner.join(room, command.nickname(), command.participantToken());

        log.info("[RoomService.join] Participant joined. roomId={}, participantId={}",
                join.roomId(), join.participant().getId());
        return RoomResult.of(join);
    }

    /** 명시적으로 나가거나 연결이 끊긴 참가자를 내보낸다. 이미 나갔으면 비어 있다 */
    public Optional<LeaveResult> leave(String participantToken) {
        return roomLeaver.leave(participantToken)
                .map(LeaveResult::from)
                .map(result -> {
                    log.info("[RoomService.leave] Participant left. roomId={}, participantId={}, roomClosed={}",
                            result.roomId(), result.participantId(), result.closed());
                    return result;
                });
    }
}

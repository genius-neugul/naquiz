package geniusneugul.project.core.room.service;

import geniusneugul.project.core.room.domain.Room;
import geniusneugul.project.core.room.implement.RoomJoin;
import java.util.List;

/**
 * 방에 들어간 참가자에게 보여줄 방 상태. meId는 이 결과를 받는 참가자다.
 */
public record RoomResult(
        Long roomId,
        String inviteCode,
        String status,
        Long meId,
        List<ParticipantResult> participants
) {

    static RoomResult of(RoomJoin join) {
        return new RoomResult(
                join.roomId(),
                join.inviteCode().value(),
                join.status().name(),
                join.participant().getId(),
                ParticipantResult.listOf(join.participants()));
    }

    static RoomResult of(Room room, Long meId) {
        return new RoomResult(
                room.getId(),
                room.getInviteCode().value(),
                room.getStatus().name(),
                meId,
                ParticipantResult.listOf(room));
    }
}

package geniusneugul.project.core.room.implement;

import geniusneugul.project.core.room.domain.InviteCode;
import geniusneugul.project.core.room.domain.Participant;
import geniusneugul.project.core.room.domain.Room;
import geniusneugul.project.core.room.domain.RoomStatus;
import java.util.List;

/**
 * 참가자 한 명이 방에 들어온 결과. 방 락 안에서 만든 스냅샷이라 락 밖에서 읽어도 안전하다.
 */
public record RoomJoin(
        Long roomId,
        InviteCode inviteCode,
        RoomStatus status,
        Participant participant,
        List<Participant> participants
) {

    static RoomJoin of(Room room, Participant participant) {
        return new RoomJoin(room.getId(), room.getInviteCode(), room.getStatus(), participant, room.getParticipants());
    }
}

package geniusneugul.project.core.room.implement;

import geniusneugul.project.core.room.domain.Participant;
import geniusneugul.project.core.room.domain.Room;
import java.util.List;

/**
 * 참가자 한 명이 방을 나간 결과. 방 락 안에서 만든 스냅샷이라 락 밖에서 읽어도 안전하다.
 */
public record RoomLeave(Long roomId, Participant participant, boolean roomClosed, List<Participant> participants) {

    static RoomLeave of(Room room, Participant participant) {
        return new RoomLeave(room.getId(), participant, room.isClosed(), room.getParticipants());
    }
}

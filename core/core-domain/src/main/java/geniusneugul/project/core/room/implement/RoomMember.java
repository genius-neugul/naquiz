package geniusneugul.project.core.room.implement;

import geniusneugul.project.core.room.domain.Participant;
import geniusneugul.project.core.room.domain.Room;

/**
 * 방에 들어가 있는 참가자 한 명. 방 락 안에서 만든 스냅샷이라 락 밖에서 읽어도 안전하다.
 */
public record RoomMember(
        Long roomId,
        Long participantId,
        String nickname,
        int tag
) {

    static RoomMember of(Room room, Participant participant) {
        return new RoomMember(room.getId(), participant.getId(), participant.getNickname(), participant.getTag());
    }
}

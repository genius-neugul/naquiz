package geniusneugul.project.core.room.service;

import geniusneugul.project.core.room.implement.RoomLeave;
import java.util.List;

/**
 * 참가자가 나간 뒤 방에 남은 사람들에게 알릴 내용. closed면 방장이 나가 방이 끝났다.
 */
public record LeaveResult(
        Long roomId,
        Long participantId,
        boolean closed,
        List<ParticipantResult> participants
) {

    static LeaveResult from(RoomLeave leave) {
        return new LeaveResult(
                leave.roomId(),
                leave.participant().getId(),
                leave.roomClosed(),
                ParticipantResult.listOf(leave.participants()));
    }
}

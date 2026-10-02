package geniusneugul.project.core.room.presentation.dto;

import geniusneugul.project.core.room.service.RoomResult;
import java.util.List;

public record RoomResponse(
        Long roomId,
        String inviteCode,
        String status,
        Long meId,
        List<ParticipantResponse> participants
) {

    public static RoomResponse from(RoomResult result) {
        return new RoomResponse(
                result.roomId(),
                result.inviteCode(),
                result.status(),
                result.meId(),
                ParticipantResponse.listOf(result.participants()));
    }
}

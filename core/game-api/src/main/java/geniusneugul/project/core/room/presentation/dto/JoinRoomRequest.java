package geniusneugul.project.core.room.presentation.dto;

import geniusneugul.project.core.room.service.JoinRoomCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRoomRequest(
        @NotBlank
        String inviteCode,

        @NotBlank
        @Size(max = 10)
        String nickname
) {

    public JoinRoomCommand toCommand(String participantToken) {
        return new JoinRoomCommand(inviteCode, nickname.strip(), participantToken);
    }
}

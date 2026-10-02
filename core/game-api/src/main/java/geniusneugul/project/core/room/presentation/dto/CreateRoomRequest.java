package geniusneugul.project.core.room.presentation.dto;

import geniusneugul.project.core.room.service.CreateRoomCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank
        @Size(max = 10)
        String nickname
) {

    public CreateRoomCommand toCommand(String participantToken) {
        return new CreateRoomCommand(nickname.strip(), participantToken);
    }
}

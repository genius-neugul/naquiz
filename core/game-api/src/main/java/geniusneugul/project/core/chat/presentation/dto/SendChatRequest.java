package geniusneugul.project.core.chat.presentation.dto;

import geniusneugul.project.core.chat.service.SendChatCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendChatRequest(
        @NotBlank
        @Size(max = 100)
        String text
) {

    public SendChatCommand toCommand(String participantToken) {
        return new SendChatCommand(text.strip(), participantToken);
    }
}

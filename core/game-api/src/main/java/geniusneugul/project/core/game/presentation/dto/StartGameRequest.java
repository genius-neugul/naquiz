package geniusneugul.project.core.game.presentation.dto;

import geniusneugul.project.core.game.service.StartGameCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StartGameRequest(
        @NotBlank
        String gameType,

        @NotNull
        @Min(1)
        @Max(50)
        Integer targetScore
) {

    public StartGameCommand toCommand(String participantToken) {
        return new StartGameCommand(gameType, targetScore, participantToken);
    }
}

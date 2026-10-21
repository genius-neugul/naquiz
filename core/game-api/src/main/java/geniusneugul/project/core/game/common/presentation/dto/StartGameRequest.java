package geniusneugul.project.core.game.common.presentation.dto;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.game.common.service.StartGameCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StartGameRequest(
        @NotNull
        GameType gameType,

        @NotNull
        @Min(1)
        @Max(50)
        Integer targetScore
) {

    public StartGameCommand toCommand(String participantToken) {
        return new StartGameCommand(gameType, targetScore, participantToken);
    }
}

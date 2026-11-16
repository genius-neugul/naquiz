package geniusneugul.project.core.game.twentyquestions.presentation.dto;

import geniusneugul.project.core.game.twentyquestions.service.RevealClueCommand;
import jakarta.validation.constraints.NotBlank;

public record RevealClueRequest(
        @NotBlank
        String clueType
) {

    public RevealClueCommand toCommand(String participantToken) {
        return new RevealClueCommand(clueType, participantToken);
    }
}

package geniusneugul.project.core.vote.presentation.dto;

import geniusneugul.project.core.vote.service.VoteCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * 투표 열기·찬성 요청. targetHintType은 힌트 투표일 때만 보낸다.
 */
public record VoteRequest(
        @NotBlank
        String voteType,

        String targetHintType
) {

    public VoteCommand toCommand(String participantToken) {
        return new VoteCommand(voteType, targetHintType, participantToken);
    }
}

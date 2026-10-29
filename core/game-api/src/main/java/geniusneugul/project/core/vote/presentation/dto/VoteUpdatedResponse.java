package geniusneugul.project.core.vote.presentation.dto;

import geniusneugul.project.core.vote.service.VoteUpdatedResult;
import java.util.List;

/**
 * 방 토픽으로 나가는 투표 현황 이벤트. 투표를 열거나 찬성하거나 참가자가 나가 다시 판정했는데 아직 통과하지 않았을 때 간다.
 */
public record VoteUpdatedResponse(
        String type,
        Long gameId,
        int roundNo,
        String voteType,
        String targetHintType,
        Long initiatorId,
        List<Long> approverIds,
        int requiredApprovals,
        int participantCount
) {

    private static final String TYPE = "VOTE_UPDATED";

    public static VoteUpdatedResponse from(VoteUpdatedResult result) {
        return new VoteUpdatedResponse(TYPE, result.gameId(), result.roundNo(), result.voteType(),
                result.targetHintType(), result.initiatorId(), result.approverIds(), result.requiredApprovals(),
                result.participantCount());
    }
}

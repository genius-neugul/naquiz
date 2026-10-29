package geniusneugul.project.core.vote.service;

import geniusneugul.project.core.vote.implement.VoteCast;
import java.util.List;

/**
 * 아직 통과하지 않은 투표의 현황. approverIds는 접속 중인 참가자의 찬성만 담는다.
 */
public record VoteUpdatedResult(
        Long roomId,
        Long gameId,
        int roundNo,
        String voteType,
        String targetHintType,
        Long initiatorId,
        List<Long> approverIds,
        int requiredApprovals,
        int participantCount,
        EndedVotes endedVotes
) implements VoteResult {

    static VoteUpdatedResult from(VoteCast cast) {
        return new VoteUpdatedResult(cast.roomId(), cast.gameId(), cast.roundNo(), cast.voteType().name(),
                cast.targetHintType() == null ? null : cast.targetHintType().name(), cast.initiatorId(),
                cast.approverIds(), cast.requiredApprovals(), cast.participantCount(),
                new EndedVotes(cast.endedVotes()));
    }
}

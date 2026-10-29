package geniusneugul.project.core.vote.implement;

import geniusneugul.project.core.game.common.domain.HintType;
import geniusneugul.project.core.vote.domain.PassThreshold;
import geniusneugul.project.core.vote.domain.Vote;
import geniusneugul.project.core.vote.domain.VoteType;
import java.util.List;

/**
 * 투표를 열거나 찬성하거나 다시 판정한 결과. 방 락 안에서 만든 스냅샷이다.
 * approverIds는 접속 중인 참가자의 찬성만 담는다. 통과했으면 pass에 종류별 통과 결과가 있고, 아니면 null이다.
 * endedVotes는 이번에 끝나(통과·만료) 열린 투표 저장소에서 꺼낸 투표로, DB에 남긴다.
 */
public record VoteCast(
        Long roomId,
        Long gameId,
        int roundNo,
        VoteType voteType,
        HintType targetHintType,
        Long initiatorId,
        List<Long> approverIds,
        int requiredApprovals,
        int participantCount,
        VotePass pass,
        List<Vote> endedVotes
) {

    static VoteCast of(Long roomId, Vote vote, List<Long> participantIds, VotePass pass, List<Vote> endedVotes) {
        return new VoteCast(roomId, vote.getGameId(), vote.getRoundNo(), vote.getVoteType(), vote.getTargetHintType(),
                vote.getInitiatorId(), vote.approverIds(participantIds), PassThreshold.required(participantIds.size()),
                participantIds.size(), pass, endedVotes);
    }

    public boolean passed() {
        return pass != null;
    }
}

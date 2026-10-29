package geniusneugul.project.core.vote.implement;

import geniusneugul.project.core.game.common.domain.GameProgress;
import geniusneugul.project.core.game.common.implement.RoundSkip;
import geniusneugul.project.core.vote.domain.Vote;
import geniusneugul.project.core.vote.domain.VoteType;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * 스킵 투표가 통과하면 라운드를 정답자 없이 끝낸다. 이후 정답 제출은 인정하지 않고 다음 라운드를 열 수 있다.
 */
@Component
public class SkipVotePassHandler implements VotePassHandler {

    @Override
    public VoteType voteType() {
        return VoteType.SKIP;
    }

    @Override
    public VotePass pass(Long roomId, GameProgress progress, Vote vote, LocalDateTime now) {
        progress.skipRound();
        return new RoundSkipPass(new RoundSkip(roomId, progress.getGameId(), progress.getRoundNo(), progress.getAnswer(), now));
    }
}

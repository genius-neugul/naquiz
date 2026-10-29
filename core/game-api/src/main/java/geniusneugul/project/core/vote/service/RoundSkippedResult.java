package geniusneugul.project.core.vote.service;

import geniusneugul.project.core.game.common.implement.RoundSkip;
import java.time.LocalDateTime;

/**
 * 스킵 투표가 통과해 라운드가 정답자 없이 끝났다. 라운드가 끝났으므로 정답을 함께 공개한다.
 */
public record RoundSkippedResult(
        Long roomId,
        Long gameId,
        int roundNo,
        String answer,
        String subAnswer,
        LocalDateTime skippedAt,
        EndedVotes endedVotes
) implements VoteResult {

    static RoundSkippedResult of(RoundSkip skip, EndedVotes endedVotes) {
        return new RoundSkippedResult(skip.roomId(), skip.gameId(), skip.roundNo(), skip.answer().answer(),
                skip.answer().subAnswer(), skip.skippedAt(), endedVotes);
    }
}

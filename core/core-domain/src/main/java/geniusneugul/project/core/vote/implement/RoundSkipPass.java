package geniusneugul.project.core.vote.implement;

import geniusneugul.project.core.game.common.implement.RoundSkip;

/**
 * 스킵 투표가 통과해 라운드가 정답자 없이 끝났다.
 */
public record RoundSkipPass(RoundSkip roundSkip) implements VotePass {

    @Override
    public boolean endsRound() {
        return true;
    }
}

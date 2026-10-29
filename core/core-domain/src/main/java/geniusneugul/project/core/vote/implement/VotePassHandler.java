package geniusneugul.project.core.vote.implement;

import geniusneugul.project.core.game.common.domain.GameProgress;
import geniusneugul.project.core.vote.domain.Vote;
import geniusneugul.project.core.vote.domain.VoteType;
import java.time.LocalDateTime;

/**
 * 투표 종류별 통과 처리(확장점). 투표 열기·찬성·판정은 종류를 모르는 공통 흐름(VoteCaster)이 하고,
 * 통과했을 때 무엇을 하는지만 종류마다 구현한다. 처리가 없는 종류의 투표는 열 수 없다.
 * 방 락 안에서 불리므로 메모리 상태만 바꾸고 I/O(DB, 메시지 발송)는 하지 않는다. 기록은 결과(VotePass)를 받은 쪽이 락 밖에서 한다.
 */
public interface VotePassHandler {

    VoteType voteType();

    VotePass pass(Long roomId, GameProgress progress, Vote vote, LocalDateTime now);
}

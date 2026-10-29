package geniusneugul.project.core.vote.implement;

/**
 * 투표가 통과해 생긴 결과. 투표 종류마다 하나씩 두고, 통과 처리(VotePassHandler)가 방 락 안에서 만든다.
 * 힌트 공개처럼 새 투표 종류를 붙이면 여기에 결과를 추가한다.
 */
public sealed interface VotePass permits RoundSkipPass {

    /** 이 통과로 라운드가 끝났다. 라운드의 남은 투표는 더 통과할 수 없으므로 만료시킨다 */
    boolean endsRound();
}

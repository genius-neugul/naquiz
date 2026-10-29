package geniusneugul.project.core.vote.service;

import geniusneugul.project.core.vote.domain.Vote;
import java.util.List;

/**
 * 이번 판정으로 끝난(통과·만료) 투표 묶음. 알림을 보낸 뒤 DB에 남기도록 결과에 실어 VoteService.record로 돌려받는다.
 * presentation이 domain을 보지 않도록 안의 투표는 service 패키지에서만 꺼낸다.
 */
public final class EndedVotes {

    private final List<Vote> votes;

    EndedVotes(List<Vote> votes) {
        this.votes = List.copyOf(votes);
    }

    List<Vote> votes() {
        return votes;
    }
}

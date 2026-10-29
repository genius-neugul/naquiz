package geniusneugul.project.core.vote.implement;

import geniusneugul.project.core.vote.domain.Vote;
import geniusneugul.project.core.vote.infra.VoteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 끝난(통과·만료) 투표를 통계용으로 DB에 남긴다. 판정은 메모리에서 이미 끝났으므로 기록이 실패해도 게임은 계속돼야 한다.
 * 그래서 독립 트랜잭션으로 쓰고, 실패는 호출한 쪽이 로그로 남기고 넘어간다.
 */
@Component
@RequiredArgsConstructor
public class VoteRecorder {

    private final VoteRepository voteRepository;

    @Transactional
    public void record(List<Vote> votes) {
        if (votes.isEmpty()) {
            return;
        }
        voteRepository.saveAll(votes);
    }
}

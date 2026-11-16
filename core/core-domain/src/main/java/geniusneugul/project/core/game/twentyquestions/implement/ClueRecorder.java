package geniusneugul.project.core.game.twentyquestions.implement;

import geniusneugul.project.core.game.twentyquestions.domain.RevealedClue;
import geniusneugul.project.core.game.twentyquestions.infra.RevealedClueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 방 락 안에서 확정한 단서 공개를 공개된 단서 기록으로 DB에 남긴다. 정답 기록(GameRecorder)과 같은 이유로
 * 독립 트랜잭션으로 쓰고, 실패는 호출한 쪽이 로그로 남기고 넘어간다. 공개 순번은 보드가 락 안에서 정한 값을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class ClueRecorder {

    private final RevealedClueRepository revealedClueRepository;

    @Transactional
    public void record(ClueReveal reveal) {
        revealedClueRepository.save(RevealedClue.create(reveal.gameId(), reveal.roundNo(), reveal.clueType(),
                reveal.content(), reveal.order(), reveal.participantId(), reveal.revealedAt()));
    }
}

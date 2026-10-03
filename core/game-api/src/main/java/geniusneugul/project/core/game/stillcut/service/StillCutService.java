package geniusneugul.project.core.game.stillcut.service;

import geniusneugul.project.core.game.stillcut.implement.StillCutRevealer;
import geniusneugul.project.core.movie.implement.StillCutReader;
import geniusneugul.project.core.question.domain.Question;
import geniusneugul.project.core.question.implement.QuestionReader;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스틸컷 영화 맞추기. 라운드가 열리면 그 영화의 스틸컷을 노출 순서대로 하나씩 공개한다(docs/DOMAIN.md 3-2).
 * 공개 간격(10초)은 presentation의 카운트다운이 정한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StillCutService {

    private final QuestionReader questionReader;
    private final StillCutReader stillCutReader;
    private final StillCutRevealer stillCutRevealer;

    /** 라운드 문제의 영화 스틸컷을 올리고 첫 컷을 공개한다. 라운드가 이미 끝났거나 스틸컷이 없으면 비어 있다 */
    @Transactional(readOnly = true)
    public Optional<StillCutRevealResult> startReel(Long roomId, Long gameId, int roundNo, Long questionId) {
        Question question = questionReader.read(questionId);
        List<String> imageUrls = stillCutReader.readImageUrls(question.getContentId());
        if (imageUrls.isEmpty()) {
            log.warn("[StillCutService.startReel] Movie has no still cut. roomId={}, questionId={}, movieId={}",
                    roomId, questionId, question.getContentId());
            return Optional.empty();
        }
        return stillCutRevealer.startReel(roomId, gameId, roundNo, imageUrls).map(StillCutRevealResult::from);
    }

    /** 다음 컷을 공개한다. 라운드가 끝났거나 바뀌었거나 모두 공개했으면 비어 있다 */
    public Optional<StillCutRevealResult> revealNext(Long roomId, Long gameId, int roundNo) {
        return stillCutRevealer.revealNext(roomId, gameId, roundNo).map(StillCutRevealResult::from);
    }

    /** 게임이 끝났거나 방이 닫혔다 */
    public void clear(Long roomId) {
        stillCutRevealer.clear(roomId);
    }
}

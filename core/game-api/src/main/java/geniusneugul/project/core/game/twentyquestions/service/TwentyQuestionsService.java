package geniusneugul.project.core.game.twentyquestions.service;

import geniusneugul.project.core.game.twentyquestions.domain.ClueContents;
import geniusneugul.project.core.game.twentyquestions.domain.ClueType;
import geniusneugul.project.core.game.twentyquestions.implement.ClueRecorder;
import geniusneugul.project.core.game.twentyquestions.implement.ClueReveal;
import geniusneugul.project.core.game.twentyquestions.implement.ClueTurnKeeper;
import geniusneugul.project.core.movie.implement.MovieReader;
import geniusneugul.project.core.question.domain.Question;
import geniusneugul.project.core.question.implement.QuestionReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 영화 스무고개. 라운드마다 차례를 무작위로 정하고, 차례 참가자가 고른 단서를 잠시 뒤 하나씩 연다(docs/DOMAIN.md 3-2).
 * 차례 제한 시간(10초)과 공개까지의 시간(3초)은 presentation의 카운트다운이 정한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TwentyQuestionsService {

    private final QuestionReader questionReader;
    private final MovieReader movieReader;
    private final ClueTurnKeeper clueTurnKeeper;
    private final ClueRecorder clueRecorder;

    /**
     * 라운드 문제의 영화로 단서를 만들고 첫 차례를 준다. 라운드가 이미 끝났으면 비어 있다
     */
    @Transactional(readOnly = true)
    public Optional<TurnStartResult> startBoard(Long roomId, Long gameId, int roundNo, Long questionId) {
        Question question = questionReader.read(questionId);
        ClueContents contents = ClueContents.of(movieReader.read(question.getContentId()), question.toAnswer());
        return clueTurnKeeper.startBoard(roomId, gameId, roundNo, contents).map(TurnStartResult::from);
    }

    /**
     * 차례 참가자가 단서를 고른다. 진행 중인 스무고개 라운드가 없으면 비어 있다
     */
    public Optional<ClueSelectResult> select(RevealClueCommand command) {
        ClueType clueType = ClueType.from(command.clueType());
        return clueTurnKeeper.select(command.participantToken(), clueType)
        .map(select -> {
                    log.info("[TwentyQuestionsService.select] Clue selected. roomId={}, gameId={}, roundNo={}, clueType={}, participantId={}",
                            select.roomId(), select.gameId(), select.roundNo(), select.clueType(), select.participantId());
                    return ClueSelectResult.from(select);
                });
    }

    /**
     * 고른 단서를 공개하고 다음 차례를 준다. 그 사이 정답이 나왔거나 라운드가 바뀌었으면 비어 있다
     */
    public Optional<ClueRevealResult> revealSelected(Long roomId, Long gameId, int roundNo) {
        return clueTurnKeeper.revealSelected(roomId, gameId, roundNo)
                .map(reveal -> {
                    log.info("[TwentyQuestionsService.revealSelected] Clue revealed. roomId={}, gameId={}, roundNo={}, clueType={}, participantId={}",
                            reveal.roomId(), reveal.gameId(), reveal.roundNo(), reveal.clueType(), reveal.participantId());
                    record(reveal);
                    return ClueRevealResult.from(reveal);
                });
    }

    /**
     * 차례 참가자가 시간 안에 고르지 않아 남은 단서 중 하나를 무작위로 대신 고른다. 그 차례가 이미 지나갔으면 비어 있다
     */
    public Optional<ClueSelectResult> selectOnTimeout(Long roomId, Long gameId, int roundNo, int turnNo) {
        return clueTurnKeeper.selectOnTimeout(roomId, gameId, roundNo, turnNo)
                .map(select -> {
                    log.info("[TwentyQuestionsService.selectOnTimeout] Clue selected at random. roomId={}, gameId={}, roundNo={}, clueType={}, participantId={}",
                            select.roomId(), select.gameId(), select.roundNo(), select.clueType(), select.participantId());
                    return ClueSelectResult.from(select);
                });
    }

    /**
     * 게스트가 나갔다. 그 참가자의 차례였으면 다음 차례를 준다
     */
    public Optional<TurnStartResult> participantLeft(Long roomId, Long participantId) {
        return clueTurnKeeper.participantLeft(roomId, participantId).map(TurnStartResult::from);
    }

    /**
     * 게임이 끝났거나 방이 닫혔다
     */
    public void clear(Long roomId) {
        clueTurnKeeper.clear(roomId);
    }

    private void record(ClueReveal reveal) {
        try {
            clueRecorder.record(reveal);
        } catch (RuntimeException e) {
            // 단서는 메모리에서 이미 열렸다. 기록 실패로 공개와 다음 차례까지 막으면 라운드가 멈춘다.
            log.error("[TwentyQuestionsService.revealSelected] Failed to record revealed clue. roomId={}, gameId={}, roundNo={}, clueType={}",
                    reveal.roomId(), reveal.gameId(), reveal.roundNo(), reveal.clueType(), e);
        }
    }
}

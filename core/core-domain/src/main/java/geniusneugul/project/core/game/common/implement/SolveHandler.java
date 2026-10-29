package geniusneugul.project.core.game.common.implement;

import geniusneugul.project.core.common.infra.event.EventPublisher;
import geniusneugul.project.core.game.common.domain.event.RoundSolvedEvent;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 라운드 진행 중 채팅을 정답 제출로 판정하고, 정답자가 확정되면 게임·라운드 기록을 남긴 뒤 라운드 종료 이벤트를 발행한다.
 * 판정은 AnswerSubmitter가 방 락 안에서 끝내고, 기록과 이벤트(리스너가 DB에 쓴다)는 락을 놓은 뒤에 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SolveHandler {

    private final AnswerSubmitter answerSubmitter;
    private final GameRecorder gameRecorder;
    private final EventPublisher eventPublisher;

    /** 정답자가 확정되면 그 결과를, 진행 중인 라운드가 없거나 오답이면 빈 값을 돌려준다 */
    public Optional<RoundSolve> handle(String participantToken, String text) {
        Optional<RoundSolve> solve = answerSubmitter.submit(participantToken, text);
        solve.ifPresent(this::afterSolve);
        return solve;
    }

    private void afterSolve(RoundSolve solve) {
        log.info("[SolveHandler.handle] Round solved. roomId={}, gameId={}, roundNo={}, solverId={}, isFinished={}",
                solve.roomId(), solve.gameId(), solve.roundNo(), solve.solverId(), solve.gameFinished());
        try {
            gameRecorder.recordSolve(solve);
        } catch (RuntimeException e) {
            log.error("[SolveHandler.handle] Failed to record solved round. roomId={}, gameId={}, roundNo={}",
                    solve.roomId(), solve.gameId(), solve.roundNo(), e);
        }
        eventPublisher.publish(new RoundSolvedEvent(solve.roomId(), solve.gameId(), solve.roundNo(), solve.solvedAt()));
    }
}

package geniusneugul.project.core.game.implement;

import geniusneugul.project.core.game.domain.Game;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 방 락 안에서 확정한 정답자를 게임·라운드 기록으로 DB에 남긴다.
 * 판정은 메모리에서 이미 끝났으므로 기록이 실패해도 게임은 계속돼야 한다. 그래서 정답 제출 흐름과 분리된
 * 독립 트랜잭션으로 쓰고, 실패는 호출한 쪽이 로그로 남기고 넘어간다.
 */
@Component
@RequiredArgsConstructor
public class GameRecorder {

    private final GameReader gameReader;

    @Transactional
    public void recordSolve(RoundSolve solve) {
        Game game = gameReader.readForUpdate(solve.gameId());
        game.solveRound(solve.roundNo(), solve.solverId(), solve.solvedAt());
        if (solve.gameFinished()) {
            game.finish(solve.solverId(), solve.solvedAt());
        }
    }
}

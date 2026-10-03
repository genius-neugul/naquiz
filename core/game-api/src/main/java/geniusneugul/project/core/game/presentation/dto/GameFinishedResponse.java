package geniusneugul.project.core.game.presentation.dto;

import geniusneugul.project.core.game.service.RoundSolvedResult;
import java.util.List;

/**
 * 방 토픽으로 나가는 게임 종료 이벤트. 승자가 목표 점수에 도달했다. 방은 대기 상태로 돌아간다.
 */
public record GameFinishedResponse(String type, Long gameId, Long winnerId, List<ScoreResponse> scores) {

    private static final String TYPE = "GAME_FINISHED";

    public static GameFinishedResponse from(RoundSolvedResult result) {
        return new GameFinishedResponse(TYPE, result.gameId(), result.solverId(), ScoreResponse.listOf(result.scores()));
    }
}

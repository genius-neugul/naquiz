package geniusneugul.project.core.game.common.presentation.dto;

import geniusneugul.project.core.game.common.presentation.SolvedRound;
import geniusneugul.project.core.game.common.service.GameAbortResult;
import java.util.List;

/**
 * 방 토픽으로 나가는 게임 종료 이벤트. 방은 대기 상태로 돌아간다.
 * 승자가 목표 점수에 도달했거나, 다음 라운드 문제를 준비하지 못해 승자 없이(winnerId null) 끝났다.
 */
public record GameFinishedResponse(String type, Long gameId, Long winnerId, List<ScoreResponse> scores) {

    private static final String TYPE = "GAME_FINISHED";

    public static GameFinishedResponse from(SolvedRound result) {
        return new GameFinishedResponse(TYPE, result.gameId(), result.solverId(), ScoreResponse.listOfSolved(result.scores()));
    }

    public static GameFinishedResponse from(GameAbortResult result) {
        return new GameFinishedResponse(TYPE, result.gameId(), null, ScoreResponse.listOf(result.scores()));
    }
}

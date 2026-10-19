package geniusneugul.project.core.game.presentation.dto;

import geniusneugul.project.core.game.service.GameStartResult;
import java.util.List;

/**
 * 방 토픽으로 나가는 게임 시작 이벤트. 모든 참가자 점수는 0이다.
 */
public record GameStartedResponse(String type, Long gameId, String gameType, int targetScore, List<ScoreResponse> scores) {

    private static final String TYPE = "GAME_STARTED";

    public static GameStartedResponse from(GameStartResult result) {
        return new GameStartedResponse(TYPE, result.gameId(), result.gameType(), result.targetScore(),
                ScoreResponse.listOf(result.scores()));
    }
}

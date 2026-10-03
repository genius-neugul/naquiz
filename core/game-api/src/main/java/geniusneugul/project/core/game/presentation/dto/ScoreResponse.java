package geniusneugul.project.core.game.presentation.dto;

import geniusneugul.project.core.game.presentation.SolvedRound;
import geniusneugul.project.core.game.service.ScoreResult;
import java.util.List;

public record ScoreResponse(Long participantId, int score) {

    static List<ScoreResponse> listOf(List<ScoreResult> scores) {
        return scores.stream().map(score -> new ScoreResponse(score.participantId(), score.score())).toList();
    }

    static List<ScoreResponse> listOfSolved(List<SolvedRound.Score> scores) {
        return scores.stream().map(score -> new ScoreResponse(score.participantId(), score.score())).toList();
    }
}

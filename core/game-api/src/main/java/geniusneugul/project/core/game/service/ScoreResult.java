package geniusneugul.project.core.game.service;

import geniusneugul.project.core.room.implement.ParticipantScore;
import java.util.List;

public record ScoreResult(Long participantId, int score) {

    static List<ScoreResult> listOf(List<ParticipantScore> scores) {
        return scores.stream().map(score -> new ScoreResult(score.participantId(), score.score())).toList();
    }
}

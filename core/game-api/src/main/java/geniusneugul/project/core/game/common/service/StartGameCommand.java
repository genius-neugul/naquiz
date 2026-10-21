package geniusneugul.project.core.game.common.service;

public record StartGameCommand(String gameType, int targetScore, String participantToken) {
}

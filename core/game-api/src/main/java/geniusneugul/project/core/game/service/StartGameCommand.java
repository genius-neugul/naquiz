package geniusneugul.project.core.game.service;

public record StartGameCommand(String gameType, int targetScore, String participantToken) {
}

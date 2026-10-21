package geniusneugul.project.core.game.common.service;

import geniusneugul.project.core.common.domain.GameType;

public record StartGameCommand(GameType gameType, int targetScore, String participantToken) {
}

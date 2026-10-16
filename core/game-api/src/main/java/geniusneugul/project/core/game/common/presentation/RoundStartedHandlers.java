package geniusneugul.project.core.game.common.presentation;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 게임 종류에 맞는 게임별 처리를 찾아 부른다. 처리할 것이 없는 게임 종류면 아무것도 하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class RoundStartedHandlers {

    private final List<RoundStartedHandler> handlers;

    public void roundStarted(StartedRound round) {
        handlers.stream()
                .filter(handler -> handler.supports(round.gameType()))
                .forEach(handler -> handler.onRoundStarted(round));
    }

    public void roundEnded(EndedRound round) {
        handlers.stream()
                .filter(handler -> handler.supports(round.gameType()))
                .forEach(handler -> handler.onRoundEnded(round));
    }

    public void gameFinished(Long roomId) {
        handlers.forEach(handler -> handler.onGameFinished(roomId));
    }
}

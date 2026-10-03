package geniusneugul.project.core.game.implement;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.game.domain.Game;
import geniusneugul.project.core.game.infra.GameRepository;
import geniusneugul.project.core.question.domain.Question;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameAppender {

    private final GameRepository gameRepository;

    /** 게임과 첫 라운드를 저장한다. 방 락을 잡기 전에 저장 오류를 드러내려고 바로 flush한다 */
    public Game append(Long roomId, GameType gameType, int targetScore, Question firstQuestion, LocalDateTime now) {
        Game game = Game.start(roomId, gameType, targetScore, now);
        game.startRound(firstQuestion.getId(), now);
        return gameRepository.saveAndFlush(game);
    }
}

package geniusneugul.project.core.game.implement;

import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.game.domain.Game;
import geniusneugul.project.core.game.infra.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameReader {

    private final GameRepository gameRepository;

    /** 바꾸기 위해 읽는다. 트랜잭션이 끝날 때까지 같은 게임을 쓰는 다른 트랜잭션은 기다린다 */
    public Game readForUpdate(Long gameId) {
        return gameRepository.findByIdForUpdate(gameId)
                .orElseThrow(() -> new IllegalStateException(ErrorCode.GAME_NOT_FOUND.getMessage() + " gameId=" + gameId));
    }
}

package geniusneugul.project.core.game.implement;

import geniusneugul.project.core.game.domain.Game;
import geniusneugul.project.core.game.domain.GameProgress;
import geniusneugul.project.core.game.domain.Round;
import geniusneugul.project.core.game.infra.GameProgressRepository;
import geniusneugul.project.core.question.domain.Question;
import geniusneugul.project.core.room.implement.ParticipantScore;
import geniusneugul.project.core.room.implement.RoomGameUpdater;
import geniusneugul.project.core.room.implement.RoomLock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 저장한 게임으로 방을 게임 중으로 바꾸고 정답 판정 상태를 등록한다. 방장인지·이미 게임 중인지 확인과
 * 상태 변경이 한 덩어리여야 하므로 방 락 안에서 한다.
 */
@Component
@RequiredArgsConstructor
public class GameStarter {

    private final RoomLock roomLock;
    private final RoomGameUpdater roomGameUpdater;
    private final GameProgressRepository gameProgressRepository;

    public GameStart start(String participantToken, Game game, Question firstQuestion) {
        Round firstRound = game.getRounds().getFirst();
        return roomLock.withLock(game.getRoomId(), () -> {
            List<ParticipantScore> scores = roomGameUpdater.startGame(game.getRoomId(), participantToken);
            gameProgressRepository.save(new GameProgress(game.getRoomId(), game.getId(), game.getGameType(),
                    game.getTargetScore(), firstRound.getRoundNo(), firstQuestion.getId(), firstQuestion.toAnswer()));
            return new GameStart(game.getRoomId(), game.getId(), game.getGameType(), game.getTargetScore(),
                    firstRound.getRoundNo(), firstRound.getStartedAt(), scores);
        });
    }
}

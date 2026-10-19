package geniusneugul.project.core.game.implement;

import geniusneugul.project.core.game.domain.GameProgress;
import geniusneugul.project.core.game.infra.GameProgressRepository;
import geniusneugul.project.core.question.domain.Question;
import geniusneugul.project.core.room.implement.RoomLock;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 정답자가 나온 라운드 다음 라운드를 연다. 그 사이 게임이 끝났으면(방장 퇴장, 승자 확정) 열지 않는다.
 */
@Component
@RequiredArgsConstructor
public class RoundStarter {

    private final RoomLock roomLock;
    private final GameProgressRepository gameProgressRepository;

    public Optional<RoundStart> start(Long roomId, Long gameId, Question question, LocalDateTime now) {
        return roomLock.withLock(roomId, () -> gameProgressRepository.findByRoomId(roomId)
                .filter(progress -> progress.getGameId().equals(gameId) && progress.isRoundSolved())
                .map(progress -> startNext(progress, question, now)));
    }

    private RoundStart startNext(GameProgress progress, Question question, LocalDateTime now) {
        int roundNo = progress.getRoundNo() + 1;
        progress.startRound(roundNo, question.getId(), question.toAnswer());
        return new RoundStart(progress.getRoomId(), progress.getGameId(), roundNo, question.getId(), now);
    }
}

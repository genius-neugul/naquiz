package geniusneugul.project.core.game.implement;

import geniusneugul.project.core.game.domain.GameProgress;
import geniusneugul.project.core.game.infra.GameProgressRepository;
import geniusneugul.project.core.room.implement.RoomGameUpdater;
import geniusneugul.project.core.room.implement.RoomLock;
import geniusneugul.project.core.room.implement.RoomMember;
import geniusneugul.project.core.room.implement.RoomMemberReader;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 정답 제출(라운드 진행 중 채팅)을 판정한다. 가장 먼저 맞힌 한 명만 정답자가 되도록 판정·점수·승자 확정을
 * 방 락 안에서 한 번에 한다.
 */
@Component
@RequiredArgsConstructor
public class AnswerSubmitter {

    private final RoomMemberReader roomMemberReader;
    private final RoomLock roomLock;
    private final RoomGameUpdater roomGameUpdater;
    private final GameProgressRepository gameProgressRepository;
    private final Clock clock;

    /** 정답자가 확정되면 그 결과를, 진행 중인 라운드가 없거나 오답이면 빈 값을 돌려준다 */
    public Optional<RoundSolve> submit(String participantToken, String text) {
        Long roomId = roomMemberReader.read(participantToken).roomId();
        return roomLock.withLock(roomId, () -> {
            // 방을 찾은 뒤 락을 잡기 전에 나갔을 수 있어 락 안에서 다시 확인한다.
            RoomMember solver = roomMemberReader.readWithLock(participantToken);
            return gameProgressRepository.findByRoomId(roomId)
                    .filter(progress -> progress.submit(text))
                    .map(progress -> solve(progress, solver, text));
        });
    }

    private RoundSolve solve(GameProgress progress, RoomMember solver, String text) {
        int score = roomGameUpdater.addScore(progress.getRoomId(), solver.participantId());
        boolean gameFinished = progress.reachedTarget(score);
        if (gameFinished) {
            roomGameUpdater.endGame(progress.getRoomId(), solver.participantId());
            gameProgressRepository.deleteByRoomId(progress.getRoomId());
        }
        return new RoundSolve(progress.getRoomId(), progress.getGameId(), progress.getRoundNo(),
                solver.participantId(), solver.nickname(), solver.tag(), text, progress.getAnswer(),
                LocalDateTime.now(clock), roomGameUpdater.readScores(progress.getRoomId()), gameFinished);
    }
}

package geniusneugul.project.core.game.stillcut.implement;

import geniusneugul.project.core.game.common.implement.RoundInPlayChecker;
import geniusneugul.project.core.game.stillcut.domain.StillCutReel;
import geniusneugul.project.core.game.stillcut.infra.StillCutReelRepository;
import geniusneugul.project.core.room.implement.RoomLock;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 스틸컷 라운드를 열고 스틸컷을 하나씩 공개한다. 정답이 나오거나 라운드가 바뀐 뒤 늦게 도착한 타이머가
 * 끝난 라운드의 스틸컷을 공개하지 않도록, 라운드가 진행 중인지 확인과 공개를 방 락 안에서 한 번에 한다.
 */
@Component
@RequiredArgsConstructor
public class StillCutRevealer {

    private final RoomLock roomLock;
    private final RoundInPlayChecker roundInPlayChecker;
    private final StillCutReelRepository stillCutReelRepository;
    private final Clock clock;

    /** 라운드의 스틸컷 묶음을 올리고 첫 컷을 공개한다. 라운드가 이미 끝났으면 비어 있다 */
    public Optional<StillCutReveal> startReel(Long roomId, Long gameId, int roundNo, List<String> imageUrls) {
        return roomLock.withLock(roomId, () -> {
            if (!roundInPlayChecker.isPlaying(roomId, gameId, roundNo)) {
                return Optional.empty();
            }
            StillCutReel reel = new StillCutReel(roomId, gameId, roundNo, imageUrls);
            stillCutReelRepository.save(reel);
            return reveal(reel);
        });
    }

    /** 다음 컷을 공개한다. 라운드가 끝났거나 바뀌었거나 모두 공개했으면 비어 있다 */
    public Optional<StillCutReveal> revealNext(Long roomId, Long gameId, int roundNo) {
        return roomLock.withLock(roomId, () -> stillCutReelRepository.findByRoomId(roomId)
                .filter(reel -> reel.isFor(gameId, roundNo))
                .filter(reel -> roundInPlayChecker.isPlaying(roomId, gameId, roundNo))
                .flatMap(this::reveal));
    }

    /**
     * 게임이 끝났거나 방이 닫혀 더 공개할 일이 없다. 지우기만 하므로 락을 잡지 않는다.
     * 닫힌 방의 락을 다시 만들지 않기 위해서다. 지운 뒤 도착한 공개 요청은 묶음이 없어 아무것도 하지 않는다.
     */
    public void clear(Long roomId) {
        stillCutReelRepository.deleteByRoomId(roomId);
    }

    private Optional<StillCutReveal> reveal(StillCutReel reel) {
        return reel.revealNext().map(shown -> StillCutReveal.of(reel, shown, LocalDateTime.now(clock)));
    }
}

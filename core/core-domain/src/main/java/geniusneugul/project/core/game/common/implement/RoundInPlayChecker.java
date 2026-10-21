package geniusneugul.project.core.game.common.implement;

import geniusneugul.project.core.game.common.infra.GameProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 그 방에서 그 게임의 그 라운드가 아직 진행 중인지 확인한다. 게임별 타이머(스틸컷 교체, 스무고개 차례)가
 * 끝난 라운드를 건드리지 않게 할 때 쓴다. 확인과 변경이 한 덩어리여야 하므로 호출하는 쪽이 방 락 안에서 부른다.
 */
@Component
@RequiredArgsConstructor
public class RoundInPlayChecker {

    private final GameProgressRepository gameProgressRepository;

    public boolean isPlaying(Long roomId, Long gameId, int roundNo) {
        return gameProgressRepository.findByRoomId(roomId)
                .filter(progress -> progress.isPlaying(gameId, roundNo))
                .isPresent();
    }
}

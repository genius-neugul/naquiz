package geniusneugul.project.core.game.infra;

import geniusneugul.project.core.game.domain.GameProgress;
import java.util.Optional;

/**
 * 진행 중인 게임의 판정 상태 저장소. 메모리에 두고 방 락 안에서만 쓴다.
 */
public interface GameProgressRepository {

    void save(GameProgress progress);

    Optional<GameProgress> findByRoomId(Long roomId);

    void deleteByRoomId(Long roomId);
}

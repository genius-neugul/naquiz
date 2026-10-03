package geniusneugul.project.core.game.stillcut.infra;

import geniusneugul.project.core.game.stillcut.domain.StillCutReel;
import java.util.Optional;

/**
 * 진행 중인 스틸컷 라운드의 공개 상태 저장소. 메모리에 두고 방 락 안에서만 쓴다.
 */
public interface StillCutReelRepository {

    void save(StillCutReel reel);

    Optional<StillCutReel> findByRoomId(Long roomId);

    void deleteByRoomId(Long roomId);
}

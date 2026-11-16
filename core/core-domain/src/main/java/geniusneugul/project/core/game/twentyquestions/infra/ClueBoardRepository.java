package geniusneugul.project.core.game.twentyquestions.infra;

import geniusneugul.project.core.game.twentyquestions.domain.ClueBoard;
import java.util.Optional;

/**
 * 진행 중인 영화 스무고개 라운드의 차례·단서 공개 상태 저장소. 메모리에 두고 방 락 안에서만 쓴다.
 */
public interface ClueBoardRepository {

    void save(ClueBoard board);

    Optional<ClueBoard> findByRoomId(Long roomId);

    void deleteByRoomId(Long roomId);
}

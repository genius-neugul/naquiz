package geniusneugul.project.core.game.infra;

import geniusneugul.project.core.game.domain.Game;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRepository extends JpaRepository<Game, Long> {

    /**
     * 게임을 바꾸기 위해 읽는다. 정답 저장·다음 라운드 저장과 방장 퇴장으로 인한 종료가 동시에 같은 게임을 쓰면
     * 나중에 저장한 쪽이 먼저 저장한 상태를 덮으므로, 행 잠금으로 한 번에 하나씩 쓰게 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Game g where g.id = :gameId")
    Optional<Game> findByIdForUpdate(@Param("gameId") Long gameId);
}

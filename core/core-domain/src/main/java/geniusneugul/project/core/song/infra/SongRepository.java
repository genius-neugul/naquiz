package geniusneugul.project.core.song.infra;

import geniusneugul.project.core.song.domain.Song;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongRepository extends JpaRepository<Song, Long> {

    /** 트랜잭션 밖에서 가수·음원까지 읽을 수 있도록 함께 가져온다 */
    @EntityGraph(attributePaths = {"artists", "audio"})
    Optional<Song> findWithAudioById(Long id);
}

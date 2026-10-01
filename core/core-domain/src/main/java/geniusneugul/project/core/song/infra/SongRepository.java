package geniusneugul.project.core.song.infra;

import geniusneugul.project.core.song.domain.Song;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongRepository extends JpaRepository<Song, Long> {
}

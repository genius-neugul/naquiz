package geniusneugul.project.core.song.infra;

import geniusneugul.project.core.song.domain.Song;
import geniusneugul.project.core.song.domain.SongSourceKey;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SongRepository extends JpaRepository<Song, Long> {

    @Query("select new geniusneugul.project.core.song.domain.SongSourceKey("
            + "s.sourceProgram, s.sourceSeq, s.rawTitle, s.rawArtist) from Song s")
    List<SongSourceKey> findAllSourceKeys();
}

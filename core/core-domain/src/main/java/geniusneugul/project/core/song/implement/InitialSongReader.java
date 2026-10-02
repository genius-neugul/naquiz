package geniusneugul.project.core.song.implement;

import geniusneugul.project.core.song.domain.Song;
import geniusneugul.project.core.song.infra.InitialSongFileReader;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InitialSongReader {

    private final InitialSongFileReader initialSongFileReader;

    public List<Song> read(Path dataDir) {
        return initialSongFileReader.read(dataDir);
    }
}

package geniusneugul.project.core.movie.implement;

import geniusneugul.project.core.movie.domain.MovieWithStillCuts;
import geniusneugul.project.core.movie.infra.InitialMovieFileReader;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InitialMovieReader {

    private final InitialMovieFileReader initialMovieFileReader;

    public List<MovieWithStillCuts> read(Path dataDir) {
        return initialMovieFileReader.read(dataDir);
    }
}

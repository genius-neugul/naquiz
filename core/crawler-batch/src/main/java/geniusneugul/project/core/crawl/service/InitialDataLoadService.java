package geniusneugul.project.core.crawl.service;

import geniusneugul.project.core.movie.domain.MovieWithStillCuts;
import geniusneugul.project.core.movie.implement.InitialMovieReader;
import geniusneugul.project.core.movie.implement.MovieAppender;
import geniusneugul.project.core.song.domain.Song;
import geniusneugul.project.core.song.implement.InitialSongReader;
import geniusneugul.project.core.song.implement.SongAppender;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 초기 데이터(initial_crawler/data/)의 노래·영화·스틸컷을 DB에 적재한다. 이미 적재한 콘텐츠는 건너뛰므로 다시 실행해도 된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InitialDataLoadService {

    private final InitialSongReader initialSongReader;
    private final SongAppender songAppender;
    private final InitialMovieReader initialMovieReader;
    private final MovieAppender movieAppender;

    /**
     * 곡이 많아 한 트랜잭션으로 묶지 않는다. SongAppender가 묶음 단위로 커밋한다.
     */
    public ContentLoadResult loadSongs(Path dataDir) {
        List<Song> songs = initialSongReader.read(dataDir);
        int addedCount = songAppender.appendNew(songs);

        log.info("[InitialDataLoadService.loadSongs] Initial songs loaded. readCount={}, addedCount={}",
                songs.size(), addedCount);
        return new ContentLoadResult(songs.size(), addedCount);
    }

    /**
     * 영화와 스틸컷을 한 트랜잭션에 저장해, 스틸컷 없이 영화만 남는 일이 없게 한다.
     */
    @Transactional
    public ContentLoadResult loadMovies(Path dataDir) {
        List<MovieWithStillCuts> movies = initialMovieReader.read(dataDir);
        int addedCount = movieAppender.appendNew(movies);

        log.info("[InitialDataLoadService.loadMovies] Initial movies loaded. readCount={}, addedCount={}",
                movies.size(), addedCount);
        return new ContentLoadResult(movies.size(), addedCount);
    }
}

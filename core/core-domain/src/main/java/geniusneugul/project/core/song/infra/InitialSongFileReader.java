package geniusneugul.project.core.song.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import geniusneugul.project.core.song.domain.ArtistName;
import geniusneugul.project.core.song.domain.Song;
import geniusneugul.project.core.song.domain.SourceProgram;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * 초기 데이터 노래 목록(radio_songs_final.json)을 읽는다. 형식은 docs/MUSIC_PARSING_RULE.md 1-11.
 */
@Component
@RequiredArgsConstructor
public class InitialSongFileReader {

    private static final String FILE_NAME = "radio_songs_final.json";

    private final JsonMapper jsonMapper;

    public List<Song> read(Path dataDir) {
        List<SongJson> songs = jsonMapper.readValue(dataDir.resolve(FILE_NAME).toFile(), new TypeReference<>() {
        });
        return songs.stream()
                .map(SongJson::toSong)
                .toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SongJson(
            SourceProgram radio,
            int seq,
            LocalDate date,
            String title,
            String subtitle,
            List<ArtistJson> artists,
            @JsonProperty("raw_title") String rawTitle,
            @JsonProperty("raw_artist") String rawArtist,
            @JsonProperty("play_count") int playCount
    ) {

        Song toSong() {
            List<ArtistName> artistNames = artists.stream()
                    .map(artist -> new ArtistName(artist.artist(), artist.artistSub()))
                    .toList();
            return Song.create(title, subtitle, artistNames, rawTitle, rawArtist, playCount, radio, seq, date);
        }
    }

    private record ArtistJson(String artist, @JsonProperty("artist_sub") String artistSub) {
    }
}

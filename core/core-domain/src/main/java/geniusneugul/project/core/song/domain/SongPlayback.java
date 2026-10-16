package geniusneugul.project.core.song.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * 출제할 노래의 재생·공개 정보. 음원이 있는 노래만 만든다.
 *
 * @param artists       가수의 한글 이름. 원본 순서대로다
 * @param albumImageUrl Spotify 정보가 없으면 null
 * @param releaseDate   Spotify 정보가 없으면 null
 */
public record SongPlayback(Long songId, String videoId, int durationSeconds, List<String> artists,
                           String albumImageUrl, LocalDate releaseDate) {

    public static SongPlayback from(Song song) {
        Audio audio = song.getAudio();
        List<String> artists = song.getArtists().stream()
                .map(ArtistName::getArtist)
                .toList();
        return new SongPlayback(song.getId(), audio.getVideoId(), audio.getDurationSeconds(), artists,
                song.getAlbumImageUrl(), song.getReleaseDate());
    }
}

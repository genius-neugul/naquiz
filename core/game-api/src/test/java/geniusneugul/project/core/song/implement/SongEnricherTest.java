package geniusneugul.project.core.song.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.song.domain.ArtistName;
import geniusneugul.project.core.song.domain.Audio;
import geniusneugul.project.core.song.domain.Song;
import geniusneugul.project.core.song.domain.SongPlayback;
import geniusneugul.project.core.song.domain.SourceProgram;
import geniusneugul.project.core.song.domain.SpotifyArtist;
import geniusneugul.project.core.song.domain.SpotifyTrack;
import geniusneugul.project.core.song.domain.VideoCandidate;
import geniusneugul.project.core.song.infra.SongRepository;
import geniusneugul.project.core.support.IntegrationTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SongEnricherTest extends IntegrationTestSupport {

    private static final VideoCandidate LYRICS_VIDEO =
            new VideoCandidate("lyrics-video", "아이유 밤편지 가사", "4:14", "채널", "1,000 views", "4 years ago");
    private static final SpotifyTrack TRACK = new SpotifyTrack("track-1", "밤편지", "Palette",
            "https://i.scdn.co/image/palette", "artist-iu", LocalDate.of(2017, 3, 24));

    @Autowired
    private SongEnricher songEnricher;

    @Autowired
    private SongReader songReader;

    @Autowired
    private SongRepository songRepository;

    @DisplayName("음원과 Spotify 정보가 모두 DB에 있으면 외부 API를 부르지 않고 재생 정보를 돌려준다.")
    @Test
    void prepare_alreadyPrepared() {
        // given
        Song song = newSong();
        song.attachAudio(Audio.create(LYRICS_VIDEO, LocalDateTime.of(2026, 10, 1, 0, 0)));
        song.enrich(TRACK);
        Long songId = songRepository.save(song).getId();

        // when
        Optional<SongPlayback> playback = songEnricher.prepare(songId);

        // then
        assertThat(playback).hasValueSatisfying(it -> {
            assertThat(it.videoId()).isEqualTo("lyrics-video");
            assertThat(it.albumImageUrl()).isEqualTo("https://i.scdn.co/image/palette");
        });
        verifyNoInteractions(youTubeSearchClient, spotifyApiClient);
    }

    @DisplayName("음원이 없으면 YouTube에서 고른 영상을 음원으로 저장하고, 다음 준비 때는 검색하지 않는다.")
    @Test
    void prepare_savesAudio() {
        // given
        Long songId = songRepository.save(newSong()).getId();
        given(youTubeSearchClient.search(anyString())).willReturn(List.of(
                new VideoCandidate("mv-video", "아이유 밤편지 MV", "4:40", null, null, null),
                LYRICS_VIDEO));

        // when
        Optional<SongPlayback> first = songEnricher.prepare(songId);
        Optional<SongPlayback> second = songEnricher.prepare(songId);

        // then
        assertThat(first).map(SongPlayback::videoId).hasValue("lyrics-video");
        assertThat(first).map(SongPlayback::durationSeconds).hasValue(254);
        assertThat(second).map(SongPlayback::videoId).hasValue("lyrics-video");
        verify(youTubeSearchClient, times(1)).search(anyString());
    }

    @DisplayName("음원을 확보한 노래에 Spotify 정보가 없으면 고른 트랙의 앨범 이미지와 발매일을 저장한다.")
    @Test
    void prepare_savesSpotifyInfo() {
        // given
        Long songId = songRepository.save(newSong()).getId();
        given(youTubeSearchClient.search(anyString())).willReturn(List.of(LYRICS_VIDEO));
        given(spotifyApiClient.searchArtists(anyString())).willReturn(List.of(new SpotifyArtist("artist-iu", "아이유")));
        given(spotifyApiClient.searchTracks(anyString())).willReturn(List.of(TRACK));

        // when
        songEnricher.prepare(songId);

        // then
        Song saved = songReader.readWithAudio(songId);
        assertThat(saved.getAlbumImageUrl()).isEqualTo("https://i.scdn.co/image/palette");
        assertThat(saved.getReleaseDate()).isEqualTo(LocalDate.of(2017, 3, 24));
        assertThat(saved.isEnriched()).isTrue();
    }

    @DisplayName("YouTube 검색이 실패하거나 고를 영상이 없으면 3번까지 시도하고, 모두 실패하면 출제할 수 없고 Spotify는 부르지 않는다.")
    @Test
    void prepare_audioUnavailable() {
        // given
        Long songId = songRepository.save(newSong()).getId();
        given(youTubeSearchClient.search(anyString()))
                .willThrow(new BusinessException(ErrorCode.SONG_LOOKUP_FAILED))
                .willReturn(List.of())
                .willReturn(List.of(new VideoCandidate("live-stream", "밤편지", "LIVE", null, null, null)));

        // when
        Optional<SongPlayback> playback = songEnricher.prepare(songId);

        // then
        assertThat(playback).isEmpty();
        assertThat(songReader.readWithAudio(songId).isPlayable()).isFalse();
        verify(youTubeSearchClient, times(3)).search(anyString());
        verifyNoInteractions(spotifyApiClient);
    }

    @DisplayName("Spotify 조회가 실패하면 다시 조회하지 않고, 곡 정보 없이 음원으로 재생 정보를 돌려준다.")
    @Test
    void prepare_spotifyFailed() {
        // given
        Long songId = songRepository.save(newSong()).getId();
        given(youTubeSearchClient.search(anyString())).willReturn(List.of(LYRICS_VIDEO));
        given(spotifyApiClient.searchArtists(anyString()))
                .willThrow(new BusinessException(ErrorCode.SONG_LOOKUP_FAILED));

        // when
        Optional<SongPlayback> playback = songEnricher.prepare(songId);

        // then
        assertThat(playback).hasValueSatisfying(it -> {
            assertThat(it.videoId()).isEqualTo("lyrics-video");
            assertThat(it.albumImageUrl()).isNull();
            assertThat(it.releaseDate()).isNull();
        });
        assertThat(songReader.readWithAudio(songId).isEnriched()).isFalse();
        verify(spotifyApiClient, times(1)).searchArtists(anyString());
    }

    private static Song newSong() {
        return Song.create("밤편지", null, List.of(new ArtistName("아이유", "IU")), "밤편지", "아이유(IU)", 1,
                SourceProgram.MUSIC_PARTY, 1, LocalDate.of(2026, 10, 1));
    }
}

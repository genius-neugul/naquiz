package geniusneugul.project.core.song.implement;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.song.domain.ArtistName;
import geniusneugul.project.core.song.domain.Audio;
import geniusneugul.project.core.song.domain.AudioSelector;
import geniusneugul.project.core.song.domain.Song;
import geniusneugul.project.core.song.domain.SongPlayback;
import geniusneugul.project.core.song.domain.SpotifyArtist;
import geniusneugul.project.core.song.domain.SpotifyMatcher;
import geniusneugul.project.core.song.domain.SpotifyTrack;
import geniusneugul.project.core.song.domain.VideoCandidate;
import geniusneugul.project.core.song.infra.SpotifyApiClient;
import geniusneugul.project.core.song.infra.YouTubeSearchClient;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 출제할 노래의 음원과 Spotify 정보를 준비한다(MUSIC_SELECTION_RULE). DB에 있는 것은 다시 조회하지 않는다.
 * 외부 API를 기다리므로 트랜잭션과 방 락 밖에서 부른다.
 * <ul>
 *   <li>음원이 없으면 출제할 수 없으므로 YouTube를 먼저 조회하고, 음원을 확보한 노래만 Spotify를 조회한다.</li>
 *   <li>YouTube는 최대 {@code youtube-max-attempts}번 시도한다.</li>
 *   <li>Spotify 정보는 없어도 출제할 수 있으므로 한 번만 조회하고, 실패하면 저장하지 않는다. 다음에 출제될 때 다시 조회한다.</li>
 * </ul>
 */
@Slf4j
@Component
public class SongEnricher {

    private final SongReader songReader;
    private final SongUpdater songUpdater;
    private final YouTubeSearchClient youTubeSearchClient;
    private final SpotifyApiClient spotifyApiClient;
    private final Clock clock;
    private final int youTubeMaxAttempts;

    public SongEnricher(SongReader songReader, SongUpdater songUpdater, YouTubeSearchClient youTubeSearchClient,
                        SpotifyApiClient spotifyApiClient, Clock clock,
                        @Value("${naquiz.song.youtube-max-attempts}") int youTubeMaxAttempts) {
        this.songReader = songReader;
        this.songUpdater = songUpdater;
        this.youTubeSearchClient = youTubeSearchClient;
        this.spotifyApiClient = spotifyApiClient;
        this.clock = clock;
        this.youTubeMaxAttempts = youTubeMaxAttempts;
    }

    /**
     * @return 음원을 준비하지 못하면 빈 값. 이 노래는 출제할 수 없다
     */
    public Optional<SongPlayback> prepare(Long songId) {
        Song song = songReader.readWithAudio(songId);
        if (!song.isPlayable()) {
            Optional<Audio> audio = searchAudio(song);
            if (audio.isEmpty()) {
                return Optional.empty();
            }
            songUpdater.attachAudio(songId, audio.get());
        }
        if (!song.isEnriched()) {
            searchTrack(song).ifPresent(track -> songUpdater.enrich(songId, track));
        }
        return songReader.readPlayback(songId);
    }

    private Optional<Audio> searchAudio(Song song) {
        String query = song.getTitle() + " " + song.getArtists().stream()
                .map(ArtistName::getArtist)
                .collect(Collectors.joining(" "));
        for (int attempt = 1; attempt <= youTubeMaxAttempts; attempt++) {
            try {
                Optional<VideoCandidate> selected = AudioSelector.select(youTubeSearchClient.search(query));
                if (selected.isPresent()) {
                    return Optional.of(Audio.create(selected.get(), LocalDateTime.now(clock)));
                }
                log.warn("[SongEnricher.searchAudio] No playable video found. songId={}, attempt={}",
                        song.getId(), attempt);
            } catch (BusinessException e) {
                // 실패 원인은 YouTubeSearchClient가 남긴다.
                log.warn("[SongEnricher.searchAudio] YouTube search attempt failed. songId={}, attempt={}",
                        song.getId(), attempt);
            }
        }
        log.warn("[SongEnricher.searchAudio] Audio unavailable. songId={}, attempts={}", song.getId(),
                youTubeMaxAttempts);
        return Optional.empty();
    }

    private Optional<SpotifyTrack> searchTrack(Song song) {
        try {
            String artistId = searchArtistId(song.getArtists().getFirst());
            List<SpotifyTrack> tracks = spotifyApiClient.searchTracks(song.getTitle());
            if (tracks.isEmpty() && song.getSubtitle() != null && !song.getSubtitle().isBlank()) {
                tracks = spotifyApiClient.searchTracks(song.getSubtitle());
            }
            return SpotifyMatcher.matchTrack(tracks, artistId)
                    .map(match -> {
                        if (!match.artistMatched()) {
                            log.warn("[SongEnricher.searchTrack] Spotify track artist unmatched, may be a cover. songId={}, trackId={}",
                                    song.getId(), match.track().id());
                        }
                        return match.track();
                    });
        } catch (BusinessException e) {
            // 실패 원인은 SpotifyApiClient가 남긴다. 곡 정보 없이 출제한다.
            log.warn("[SongEnricher.searchTrack] Spotify info unavailable, preparing without it. songId={}",
                    song.getId());
            return Optional.empty();
        }
    }

    // 한글 이름 → 병기 이름 순으로 정확히 일치하는 아티스트를 찾고, 없으면 첫 검색의 첫 결과를 쓴다(1-2).
    private String searchArtistId(ArtistName name) {
        List<SpotifyArtist> byArtist = spotifyApiClient.searchArtists(name.getArtist());
        Optional<SpotifyArtist> matched = SpotifyMatcher.matchArtist(byArtist, name);
        if (matched.isEmpty() && !name.getArtistSub().equals(name.getArtist())) {
            matched = SpotifyMatcher.matchArtist(spotifyApiClient.searchArtists(name.getArtistSub()), name);
        }
        return matched.or(() -> byArtist.stream().findFirst())
                .map(SpotifyArtist::id)
                .orElse(null);
    }
}

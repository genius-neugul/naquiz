package geniusneugul.project.core.song.domain;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Spotify 검색 결과에서 곡의 아티스트와 트랙을 고른다(MUSIC_SELECTION_RULE 1-2, 1-3).
 * 다시 검색할지(artistSub로 재검색, subtitle로 재검색)는 호출하는 쪽이 정한다.
 */
public final class SpotifyMatcher {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private SpotifyMatcher() {
    }

    /**
     * @return 이름이 한글 이름이나 병기 이름과 같은(대소문자·공백 무시) 첫 번째 아티스트. 없으면 빈 값
     */
    public static Optional<SpotifyArtist> matchArtist(List<SpotifyArtist> artists, ArtistName name) {
        String artist = normalize(name.getArtist());
        String artistSub = normalize(name.getArtistSub());
        return artists.stream()
                .filter(candidate -> candidate.name() != null)
                .filter(candidate -> {
                    String normalized = normalize(candidate.name());
                    return normalized.equals(artist) || normalized.equals(artistSub);
                })
                .findFirst();
    }

    /**
     * 앨범의 첫 번째 아티스트 id가 같은 첫 번째 트랙을 고른다. 없으면 첫 번째 트랙을 고르되 다른 가수의 동명곡일 수 있음을 표시한다.
     *
     * @param artistId 1-2에서 정한 아티스트 id. 정하지 못했으면 null
     * @return 트랙이 하나도 없으면 빈 값
     */
    public static Optional<TrackMatch> matchTrack(List<SpotifyTrack> tracks, String artistId) {
        if (tracks.isEmpty()) {
            return Optional.empty();
        }
        return tracks.stream()
                .filter(track -> artistId != null && Objects.equals(track.albumArtistId(), artistId))
                .findFirst()
                .map(track -> new TrackMatch(track, true))
                .or(() -> Optional.of(new TrackMatch(tracks.getFirst(), false)));
    }

    private static String normalize(String text) {
        return WHITESPACE.matcher(text).replaceAll("").toLowerCase(Locale.ROOT);
    }

    /**
     * @param artistMatched 앨범 아티스트가 곡의 아티스트와 같은지. false면 다른 가수의 동명곡일 수 있다
     */
    public record TrackMatch(SpotifyTrack track, boolean artistMatched) {
    }
}

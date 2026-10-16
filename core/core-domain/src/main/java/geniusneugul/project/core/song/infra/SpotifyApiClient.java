package geniusneugul.project.core.song.infra;

import com.fasterxml.jackson.annotation.JsonProperty;
import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.song.domain.SpotifyArtist;
import geniusneugul.project.core.song.domain.SpotifyTrack;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Spotify Web API로 아티스트·트랙을 검색한다(MUSIC_SELECTION_RULE 1장). 토큰은 Client Credentials로 받아 만료 전까지 재사용한다.
 * 검색이 실패하면 {@link ErrorCode#SONG_LOOKUP_FAILED}를 던지고, 다시 시도할지는 호출하는 쪽이 정한다.
 */
@Slf4j
@Component
public class SpotifyApiClient {

    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String SEARCH_URL =
            "https://api.spotify.com/v1/search?q={query}&type={type}&market=KR&limit=5&offset=0";
    // 만료 직전에 받은 토큰이 요청 도중 만료되지 않도록 여유를 두고 새로 받는다.
    private static final Duration TOKEN_REFRESH_MARGIN = Duration.ofSeconds(60);

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final Duration maxRetryAfter;
    private final Clock clock;
    private final Waiter waiter;

    private String accessToken;
    private Instant tokenRefreshAt;

    /**
     * @param requestTimeout 429의 retry-after가 이 시간을 넘으면 기다리지 않는다. 출제할 때 참가자가 라운드를 기다리고 있기 때문이다
     */
    @Autowired
    public SpotifyApiClient(@Qualifier("songApiRestClient") RestClient restClient,
                            @Value("${naquiz.song.spotify.client-id}") String clientId,
                            @Value("${naquiz.song.spotify.client-secret}") String clientSecret,
                            @Value("${naquiz.song.request-timeout}") Duration requestTimeout,
                            Clock clock) {
        this(restClient, clientId, clientSecret, requestTimeout, clock, Thread::sleep);
    }

    SpotifyApiClient(RestClient restClient, String clientId, String clientSecret, Duration maxRetryAfter,
                     Clock clock, Waiter waiter) {
        this.restClient = restClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.maxRetryAfter = maxRetryAfter;
        this.clock = clock;
        this.waiter = waiter;
    }

    public List<SpotifyArtist> searchArtists(String name) {
        ArtistSearchResponse response = search(name, "artist", ArtistSearchResponse.class);
        return itemsOf(Optional.ofNullable(response).map(ArtistSearchResponse::artists).map(ArtistPage::items))
                .stream()
                .map(artist -> new SpotifyArtist(artist.id(), artist.name()))
                .toList();
    }

    public List<SpotifyTrack> searchTracks(String query) {
        TrackSearchResponse response = search(query, "track", TrackSearchResponse.class);
        return itemsOf(Optional.ofNullable(response).map(TrackSearchResponse::tracks).map(TrackPage::items))
                .stream()
                .map(SpotifyApiClient::toTrack)
                .toList();
    }

    private <T> T search(String query, String type, Class<T> responseType) {
        long startedAt = System.nanoTime();
        try {
            return searchRetryingOnce(query, type, responseType);
        } catch (RestClientException e) {
            log.warn("[SpotifyApiClient.search] Spotify search failed. provider=spotify, type={}, statusCode={}, elapsedMs={}",
                    type, statusCodeOf(e), elapsedMs(startedAt), e);
            throw new BusinessException(ErrorCode.SONG_LOOKUP_FAILED, Map.of("provider", "spotify", "type", type), e);
        }
    }

    private <T> T searchRetryingOnce(String query, String type, Class<T> responseType) {
        try {
            return requestSearch(query, type, responseType);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.info("[SpotifyApiClient.searchRetryingOnce] Spotify token rejected, retrying with new token. type={}", type);
            expireToken();
            return requestSearch(query, type, responseType);
        } catch (HttpClientErrorException.TooManyRequests e) {
            waitRetryAfter(e, type);
            return requestSearch(query, type, responseType);
        }
    }

    private <T> T requestSearch(String query, String type, Class<T> responseType) {
        return restClient.get()
                .uri(SEARCH_URL, Map.of("query", query, "type", type))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
                .header(HttpHeaders.ACCEPT_LANGUAGE, "ko")
                .retrieve()
                .body(responseType);
    }

    private void waitRetryAfter(HttpClientErrorException.TooManyRequests e, String type) {
        Optional<Duration> retryAfter = retryAfterOf(e);
        if (retryAfter.isEmpty() || retryAfter.get().compareTo(maxRetryAfter) > 0) {
            // 오래 기다리면 그동안 라운드가 열리지 않는다. 이번 조회는 실패로 보고 호출하는 쪽의 재시도에 맡긴다.
            throw e;
        }
        log.info("[SpotifyApiClient.waitRetryAfter] Spotify rate limited, waiting. type={}, retryAfterSeconds={}",
                type, retryAfter.get().toSeconds());
        try {
            waiter.await(retryAfter.get());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SONG_LOOKUP_FAILED, Map.of("provider", "spotify", "type", type),
                    interrupted);
        }
    }

    private synchronized String accessToken() {
        Instant now = clock.instant();
        if (accessToken == null || !now.isBefore(tokenRefreshAt)) {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            TokenResponse token = restClient.post()
                    .uri(TOKEN_URL)
                    .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (token == null || token.accessToken() == null) {
                throw new RestClientException("Spotify token response has no access_token");
            }
            accessToken = token.accessToken();
            tokenRefreshAt = now.plusSeconds(token.expiresIn()).minus(TOKEN_REFRESH_MARGIN);
        }
        return accessToken;
    }

    private synchronized void expireToken() {
        accessToken = null;
    }

    private static Optional<Duration> retryAfterOf(HttpClientErrorException e) {
        String value = Optional.ofNullable(e.getResponseHeaders())
                .map(headers -> headers.getFirst(HttpHeaders.RETRY_AFTER))
                .orElse(null);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Duration.ofSeconds(Long.parseLong(value.trim())));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private static SpotifyTrack toTrack(TrackItem track) {
        AlbumItem album = Optional.ofNullable(track.album()).orElse(AlbumItem.EMPTY);
        String imageUrl = itemsOf(Optional.ofNullable(album.images())).stream()
                .findFirst().map(Image::url).orElse(null);
        String albumArtistId = itemsOf(Optional.ofNullable(album.artists())).stream()
                .findFirst().map(ArtistItem::id).orElse(null);
        return new SpotifyTrack(track.id(), track.name(), album.name(), imageUrl, albumArtistId,
                parseReleaseDate(album.releaseDate()));
    }

    // Spotify 발매일은 yyyy-MM-dd, yyyy-MM, yyyy 중 하나로 온다(MUSIC_SELECTION_RULE 1-4).
    private static LocalDate parseReleaseDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return switch (value.length()) {
                case 10 -> LocalDate.parse(value);
                case 7 -> YearMonth.parse(value).atDay(1);
                case 4 -> Year.parse(value).atDay(1);
                default -> null;
            };
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static <T> List<T> itemsOf(Optional<List<T>> items) {
        return items.orElse(List.of());
    }

    private static Object statusCodeOf(RestClientException e) {
        return e instanceof RestClientResponseException response ? response.getStatusCode().value() : "none";
    }

    private static long elapsedMs(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    /**
     * 429 응답 뒤 다시 요청하기 전까지 기다린다. 테스트는 실제로 기다리지 않도록 바꿔 넣는다.
     */
    @FunctionalInterface
    interface Waiter {
        void await(Duration duration) throws InterruptedException;
    }

    record TokenResponse(@JsonProperty("access_token") String accessToken,
                         @JsonProperty("expires_in") long expiresIn) {
    }

    record ArtistSearchResponse(ArtistPage artists) {
    }

    record ArtistPage(List<ArtistItem> items) {
    }

    record ArtistItem(String id, String name) {
    }

    record TrackSearchResponse(TrackPage tracks) {
    }

    record TrackPage(List<TrackItem> items) {
    }

    record TrackItem(String id, String name, AlbumItem album) {
    }

    record AlbumItem(String name, List<Image> images, List<ArtistItem> artists,
                     @JsonProperty("release_date") String releaseDate) {

        static final AlbumItem EMPTY = new AlbumItem(null, null, null, null);
    }

    record Image(String url) {
    }
}

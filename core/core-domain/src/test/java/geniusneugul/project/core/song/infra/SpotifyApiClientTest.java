package geniusneugul.project.core.song.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.song.domain.SpotifyArtist;
import geniusneugul.project.core.song.domain.SpotifyTrack;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestClient;

class SpotifyApiClientTest {

    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String SEARCH_URL = "https://api.spotify.com/v1/search";
    private static final Duration MAX_RETRY_AFTER = Duration.ofSeconds(5);
    private static final String EMPTY_ARTISTS = """
            {"artists": {"items": []}}
            """;

    private final List<Duration> waits = new ArrayList<>();
    private MockRestServiceServer server;
    private SpotifyApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T00:00:00Z"), ZoneOffset.UTC);
        client = new SpotifyApiClient(builder.build(), "client-id", "client-secret", MAX_RETRY_AFTER, clock,
                waits::add);
    }

    @DisplayName("토큰이 만료되기 전에는 새로 받지 않고 재사용한다.")
    @Test
    void searchArtists_reusesToken() {
        // given
        expectToken("token-1");
        expectSearch("token-1").andRespond(withSuccess(EMPTY_ARTISTS, MediaType.APPLICATION_JSON));
        expectSearch("token-1").andRespond(withSuccess(EMPTY_ARTISTS, MediaType.APPLICATION_JSON));

        // when
        client.searchArtists("아이유");
        client.searchArtists("방탄소년단");

        // then
        server.verify();
    }

    @DisplayName("401을 받으면 토큰을 새로 받아 한 번 다시 요청한다.")
    @Test
    void searchArtists_retriesWithNewTokenOnUnauthorized() {
        // given
        expectToken("token-1");
        expectSearch("token-1").andRespond(withUnauthorizedRequest());
        expectToken("token-2");
        expectSearch("token-2").andRespond(withSuccess("""
                {"artists": {"items": [{"id": "artist-1", "name": "아이유"}]}}
                """, MediaType.APPLICATION_JSON));

        // when
        List<SpotifyArtist> artists = client.searchArtists("아이유");

        // then
        assertThat(artists).containsExactly(new SpotifyArtist("artist-1", "아이유"));
        server.verify();
    }

    @DisplayName("새 토큰으로 다시 요청해도 401이면 더 시도하지 않고 실패한다.")
    @Test
    void searchArtists_unauthorizedTwice() {
        // given
        expectToken("token-1");
        expectSearch("token-1").andRespond(withUnauthorizedRequest());
        expectToken("token-2");
        expectSearch("token-2").andRespond(withUnauthorizedRequest());

        // when & then
        assertThatThrownBy(() -> client.searchArtists("아이유"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.SONG_LOOKUP_FAILED.getMessage());
        server.verify();
    }

    @DisplayName("429의 retry-after가 상한(5초) 이하이면 그만큼 기다린 뒤 다시 요청한다.")
    @Test
    void searchArtists_waitsRetryAfterWithinLimit() {
        // given
        expectToken("token-1");
        expectSearch("token-1").andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, "5"));
        expectSearch("token-1").andRespond(withSuccess(EMPTY_ARTISTS, MediaType.APPLICATION_JSON));

        // when
        client.searchArtists("아이유");

        // then
        assertThat(waits).containsExactly(Duration.ofSeconds(5));
        server.verify();
    }

    @DisplayName("429의 retry-after가 상한(5초)을 넘으면 기다리지 않고 실패한다.")
    @Test
    void searchArtists_retryAfterExceedsLimit() {
        // given
        expectToken("token-1");
        expectSearch("token-1").andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, "6"));

        // when & then
        assertThatThrownBy(() -> client.searchArtists("아이유"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.SONG_LOOKUP_FAILED.getMessage());
        assertThat(waits).isEmpty();
        server.verify();
    }

    @DisplayName("트랙의 id, 이름, 앨범 이름·첫 번째 이미지·첫 번째 아티스트 id를 읽는다.")
    @Test
    void searchTracks() {
        // given
        expectToken("token-1");
        expectSearch("token-1").andRespond(withSuccess(trackResponse("2019-04-12"), MediaType.APPLICATION_JSON));

        // when
        List<SpotifyTrack> tracks = client.searchTracks("작은 것들을 위한 시");

        // then
        assertThat(tracks).containsExactly(new SpotifyTrack("track-1", "작은 것들을 위한 시", "MAP OF THE SOUL : PERSONA",
                "https://i.scdn.co/image/large", "artist-1", LocalDate.of(2019, 4, 12)));
    }

    @DisplayName("발매일은 yyyy-MM-dd, yyyy-MM, yyyy 형식을 모두 읽고 빠진 월·일은 1로 채운다.")
    @ParameterizedTest
    @CsvSource({
            "2019-04-12, 2019-04-12",
            "2019-04, 2019-04-01",
            "2019, 2019-01-01"
    })
    void searchTracks_releaseDatePrecision(String releaseDate, LocalDate expected) {
        // given
        expectToken("token-1");
        expectSearch("token-1").andRespond(withSuccess(trackResponse(releaseDate), MediaType.APPLICATION_JSON));

        // when
        List<SpotifyTrack> tracks = client.searchTracks("작은 것들을 위한 시");

        // then
        assertThat(tracks).extracting(SpotifyTrack::releaseDate).containsExactly(expected);
    }

    private void expectToken(String accessToken) {
        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"access_token": "%s", "token_type": "Bearer", "expires_in": 3600}
                        """.formatted(accessToken), MediaType.APPLICATION_JSON));
    }

    private ResponseActions expectSearch(String accessToken) {
        return server.expect(requestTo(startsWith(SEARCH_URL)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
    }

    private static String trackResponse(String releaseDate) {
        return """
                {"tracks": {"items": [{
                  "id": "track-1",
                  "name": "작은 것들을 위한 시",
                  "album": {
                    "name": "MAP OF THE SOUL : PERSONA",
                    "images": [{"url": "https://i.scdn.co/image/large"}, {"url": "https://i.scdn.co/image/small"}],
                    "artists": [{"id": "artist-1", "name": "방탄소년단"}, {"id": "artist-2", "name": "Halsey"}],
                    "release_date": "%s"
                  }
                }]}}
                """.formatted(releaseDate);
    }
}

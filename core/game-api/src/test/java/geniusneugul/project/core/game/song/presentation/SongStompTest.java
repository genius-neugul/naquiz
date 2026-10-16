package geniusneugul.project.core.game.song.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import geniusneugul.project.core.song.domain.ArtistName;
import geniusneugul.project.core.song.domain.Song;
import geniusneugul.project.core.song.domain.SourceProgram;
import geniusneugul.project.core.song.domain.SpotifyArtist;
import geniusneugul.project.core.song.domain.SpotifyTrack;
import geniusneugul.project.core.song.domain.VideoCandidate;
import geniusneugul.project.core.song.infra.SongRepository;
import geniusneugul.project.core.support.WebSocketTestSupport;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.stomp.StompSession;

class SongStompTest extends WebSocketTestSupport {

    // 모든 노래 문제의 정답을 같게 두어, 어느 노래가 나와도 같은 채팅으로 맞힌다.
    private static final String ANSWER = "밤편지";
    private static final VideoCandidate VIDEO = new VideoCandidate("video-1", "밤편지 가사", "4:14", null, null, null);
    private static final Duration SLOW_PREPARE = Duration.ofMillis(800);
    private static final String ALBUM_IMAGE_URL = "https://i.scdn.co/image/palette";

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 소켓 테스트는 테이블을 비우지 않는다. 음원이 없는 새 노래 4곡으로 노래 문제를 매번 새로 만든다.
    // 1라운드 노래를 빼도 음원 없는 노래가 3곡 남아야, 다음 라운드에서 3곡 연속 실패를 만들 수 있다.
    @BeforeEach
    void setUpSongQuestions() {
        jdbcTemplate.update("DELETE FROM question WHERE game_type = 'SONG'");
        for (int i = 1; i <= 4; i++) {
            Song song = songRepository.save(Song.create("노래" + i, null, List.of(new ArtistName("가수", "Singer")),
                    "노래" + i, "가수", 1, SourceProgram.MUSIC_PARTY, 1, LocalDate.of(2026, 10, 1)));
            jdbcTemplate.update("""
                    INSERT INTO question (game_type, content_id, answer, sub_answer, active, review_status)
                    VALUES ('SONG', ?, ?, NULL, TRUE, 'APPROVED')
                    """, song.getId(), ANSWER);
        }
    }

    @DisplayName("노래 라운드가 열리면 라운드 시작 다음에 재생할 음원과 라운드 시작 시각, 서버 시각이 간다.")
    @Test
    void start_sendsSongAudio() throws Exception {
        // given
        given(youTubeSearchClient.search(anyString())).willReturn(List.of(VIDEO));
        StompSession host = connect();
        BlockingQueue<Map<String, Object>> events = createRoomAndSubscribe(host);

        // when
        host.send("/app/games/start", Map.of("gameType", "SONG", "targetScore", 3));

        // then
        Map<String, Object> round = awaitEvent(events, "ROUND_STARTED");
        Map<String, Object> audio = awaitMessage(events);
        assertThat(audio.get("type")).isEqualTo("SONG_AUDIO");
        assertThat(audio.get("roundNo")).isEqualTo(1);
        assertThat(audio.get("videoId")).isEqualTo("video-1");
        assertThat(audio.get("durationSeconds")).isEqualTo(254);
        assertThat(audio.get("startedAt")).isEqualTo(round.get("startedAt"));
        assertThat(audio.get("serverTime")).isNotNull();
    }

    @DisplayName("정답이 나오면 정답자 확정 다음에 가수와 앨범 이미지가 가고, 발매일은 가지 않는다.")
    @Test
    void submit_sendsAnswerRevealed() throws Exception {
        // given
        givenPlayableWithAlbum();
        StompSession host = connect();
        BlockingQueue<Map<String, Object>> events = createRoomAndSubscribe(host);
        host.send("/app/games/start", Map.of("gameType", "SONG", "targetScore", 3));
        awaitEvent(events, "ROUND_STARTED");

        // when
        host.send("/app/rooms/chat", Map.of("text", ANSWER));

        // then
        awaitEvent(events, "ROUND_SOLVED");
        Map<String, Object> revealed = awaitMessage(events);
        assertThat(revealed.get("type")).isEqualTo("SONG_ANSWER_REVEALED");
        assertThat(revealed.get("roundNo")).isEqualTo(1);
        assertThat(revealed.get("artists")).isEqualTo(List.of("가수"));
        assertThat(revealed.get("albumImageUrl")).isEqualTo(ALBUM_IMAGE_URL);
        assertThat(revealed).doesNotContainKey("releaseDate");
    }

    @DisplayName("목표 점수에 도달해 게임이 끝나는 라운드에서도 노래 정보가 게임 종료보다 먼저 간다.")
    @Test
    void submit_reachTargetScore_sendsAnswerRevealedBeforeFinish() throws Exception {
        // given
        givenPlayableWithAlbum();
        StompSession host = connect();
        BlockingQueue<Map<String, Object>> events = createRoomAndSubscribe(host);
        host.send("/app/games/start", Map.of("gameType", "SONG", "targetScore", 1));
        awaitEvent(events, "ROUND_STARTED");

        // when
        host.send("/app/rooms/chat", Map.of("text", ANSWER));

        // then
        awaitEvent(events, "ROUND_SOLVED");
        assertThat(awaitMessage(events).get("type")).isEqualTo("SONG_ANSWER_REVEALED");
        assertThat(awaitMessage(events).get("type")).isEqualTo("GAME_FINISHED");
    }

    @DisplayName("다음 라운드 노래를 3곡 연속 준비하지 못하면 다음 라운드 시각 뒤에 승자 없이 게임이 끝나고 방은 대기 상태로 돌아간다.")
    @Test
    void nextRound_questionUnavailable() throws Exception {
        // given
        AtomicBoolean youTubeAvailable = new AtomicBoolean(true);
        given(youTubeSearchClient.search(anyString()))
                .willAnswer(invocation -> youTubeAvailable.get() ? List.of(VIDEO) : List.of());
        StompSession host = connect();
        BlockingQueue<Map<String, Object>> events = createRoomAndSubscribe(host);
        host.send("/app/games/start", Map.of("gameType", "SONG", "targetScore", 3));
        Map<String, Object> started = awaitEvent(events, "GAME_STARTED");
        awaitEvent(events, "ROUND_STARTED");
        youTubeAvailable.set(false);

        // when
        host.send("/app/rooms/chat", Map.of("text", ANSWER));

        // then
        LocalDateTime nextRoundAt = LocalDateTime.parse((String) awaitEvent(events, "ROUND_SOLVED").get("nextRoundAt"));
        Map<String, Object> finished = awaitEvent(events, "GAME_FINISHED");
        assertThat(LocalDateTime.now()).isAfterOrEqualTo(nextRoundAt);
        assertThat(finished.get("gameId")).isEqualTo(started.get("gameId"));
        assertThat(finished.get("winnerId")).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM game WHERE id = ?", String.class,
                ((Number) started.get("gameId")).longValue())).isEqualTo("FINISHED");

        // 방이 대기 상태로 돌아왔으므로 다시 시작할 수 있다.
        youTubeAvailable.set(true);
        host.send("/app/games/start", Map.of("gameType", "SONG", "targetScore", 3));
        assertThat(awaitEvent(events, "GAME_STARTED").get("gameId")).isNotEqualTo(started.get("gameId"));
    }

    @DisplayName("다음 라운드 노래 준비가 다음 라운드 시각보다 오래 걸리면 준비가 끝난 뒤 라운드가 열린다.")
    @Test
    void nextRound_slowPrepare() throws Exception {
        // given
        AtomicBoolean slow = new AtomicBoolean(false);
        given(youTubeSearchClient.search(anyString())).willAnswer(invocation -> {
            if (slow.get()) {
                Thread.sleep(SLOW_PREPARE);
            }
            return List.of(VIDEO);
        });
        StompSession host = connect();
        BlockingQueue<Map<String, Object>> events = createRoomAndSubscribe(host);
        host.send("/app/games/start", Map.of("gameType", "SONG", "targetScore", 3));
        awaitEvent(events, "ROUND_STARTED");
        slow.set(true);

        // when
        host.send("/app/rooms/chat", Map.of("text", ANSWER));

        // then
        LocalDateTime nextRoundAt = LocalDateTime.parse((String) awaitEvent(events, "ROUND_SOLVED").get("nextRoundAt"));
        Map<String, Object> round = awaitEvent(events, "ROUND_STARTED");
        LocalDateTime startedAt = LocalDateTime.parse((String) round.get("startedAt"));
        assertThat(round.get("roundNo")).isEqualTo(2);
        // 다음 라운드 시각(100ms)보다 준비(800ms)가 길다. 준비가 정답 공개와 함께 시작했으므로 그 차이만큼 늦게 열린다.
        assertThat(Duration.between(nextRoundAt, startedAt)).isGreaterThan(Duration.ofMillis(500));
    }

    private void givenPlayableWithAlbum() {
        given(youTubeSearchClient.search(anyString())).willReturn(List.of(VIDEO));
        given(spotifyApiClient.searchArtists(anyString())).willReturn(List.of(new SpotifyArtist("artist-1", "가수")));
        given(spotifyApiClient.searchTracks(anyString())).willReturn(List.of(new SpotifyTrack("track-1", ANSWER,
                "Palette", ALBUM_IMAGE_URL, "artist-1", LocalDate.of(2017, 3, 24))));
    }

    private BlockingQueue<Map<String, Object>> createRoomAndSubscribe(StompSession host) {
        BlockingQueue<Map<String, Object>> rooms = subscribe(host, "/user/queue/room");
        host.send("/app/rooms/create", Map.of("nickname", "방장"));
        Map<String, Object> created = awaitMessage(rooms);
        return subscribe(host, "/topic/rooms/" + created.get("roomId"));
    }

    private Map<String, Object> awaitEvent(BlockingQueue<Map<String, Object>> events, String type) {
        Map<String, Object> event = awaitMessage(events);
        while (!type.equals(event.get("type"))) {
            event = awaitMessage(events);
        }
        return event;
    }
}

package geniusneugul.project.core.game.twentyquestions.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import geniusneugul.project.core.movie.domain.Movie;
import geniusneugul.project.core.movie.infra.MovieRepository;
import geniusneugul.project.core.support.WebSocketTestSupport;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.stomp.StompSession;

/**
 * 방장 혼자 하는 스무고개. 혼자면 차례가 늘 방장에게 오므로 차례 패스 타이머(테스트 300ms)와 엇갈려도 결과가 같다.
 */
class TwentyQuestionsStompTest extends WebSocketTestSupport {

    private static final String GAME_TYPE = "MOVIE_TWENTY_QUESTIONS";
    private static final String ANSWER = "기생충";
    private static final String KOBIS_MOVIE_CODE = "TQ-TEST-1";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MovieRepository movieRepository;

    private StompSession host;
    private BlockingQueue<Map<String, Object>> events;
    private BlockingQueue<Map<String, Object>> errors;
    private Object hostId;

    // 소켓 테스트는 테이블을 비우지 않으므로 같은 영화·문제가 이미 있으면 다시 넣지 않는다.
    // 같은 H2를 쓰는 다른 통합 테스트가 남긴 스무고개 문제는 지워, 이 테스트의 문제만 출제되게 한다.
    @BeforeEach
    void setUp() throws Exception {
        Long movieId = jdbcTemplate.queryForList("SELECT id FROM movie WHERE kobis_movie_code = ?", Long.class,
                        KOBIS_MOVIE_CODE).stream()
                .findFirst()
                .orElseGet(() -> movieRepository.save(Movie.create(KOBIS_MOVIE_CODE, ANSWER, "Parasite", 10_313_086L,
                        LocalDate.of(2019, 5, 30), List.of("봉준호"), List.of("송강호", "이선균"),
                        "기생충 가족의 이야기", List.of("드라마"), List.of("한국"), "15세이상관람가")).getId());
        jdbcTemplate.update("DELETE FROM question WHERE game_type = ? AND content_id <> ?", GAME_TYPE, movieId);
        jdbcTemplate.update("""
                INSERT INTO question (game_type, content_id, answer, sub_answer, active, review_status)
                SELECT ?, ?, ?, 'Parasite', TRUE, 'APPROVED'
                WHERE NOT EXISTS (SELECT 1 FROM question WHERE game_type = ?)
                """, GAME_TYPE, movieId, ANSWER, GAME_TYPE);

        host = connect();
        errors = subscribe(host, "/user/queue/errors");
        BlockingQueue<Map<String, Object>> rooms = subscribe(host, "/user/queue/room");
        host.send("/app/rooms/create", Map.of("nickname", "방장"));
        Map<String, Object> created = awaitMessage(rooms);
        hostId = created.get("meId");
        events = subscribe(host, "/topic/rooms/" + created.get("roomId"));
    }

    @DisplayName("라운드가 열리면 첫 차례가 오고, 시간 안에 고르지 않으면 서버가 단서를 무작위로 골라 공개한 뒤 다음 차례가 온다.")
    @Test
    void turn_timedOut() {
        // when
        host.send("/app/games/start", Map.of("gameType", GAME_TYPE, "targetScore", 3));

        // then
        awaitEvent("ROUND_STARTED");
        Map<String, Object> first = awaitMessage(events);
        assertThat(first).containsEntry("type", "TURN_STARTED").containsEntry("participantId", hostId);
        assertThat(first.get("deadline")).isNotNull();
        Map<String, Object> selected = awaitMessage(events);
        assertThat(selected).containsEntry("type", "CLUE_SELECTED").containsEntry("timedOut", true)
                .containsEntry("participantId", hostId);
        Map<String, Object> revealed = awaitMessage(events);
        assertThat(revealed).containsEntry("type", "CLUE_REVEALED").containsEntry("clueType", selected.get("clueType"))
                .containsEntry("revealedById", hostId);
        assertThat(awaitMessage(events)).containsEntry("type", "TURN_STARTED");
    }

    @DisplayName("차례 참가자가 단서를 고르면 고른 칸이 먼저 오고, 잠시 뒤 공개되고 기록이 남는다. 시놉시스의 제목은 가린다.")
    @Test
    void reveal() {
        // given
        host.send("/app/games/start", Map.of("gameType", GAME_TYPE, "targetScore", 3));
        awaitEvent("TURN_STARTED");
        int recordedBefore = countRevealedClues();

        // when
        host.send("/app/games/clues/reveal", Map.of("clueType", "SYNOPSIS"));

        // then
        Map<String, Object> selected = awaitEvent("CLUE_SELECTED");
        assertThat(selected).containsEntry("clueType", "SYNOPSIS").containsEntry("participantId", hostId)
                .doesNotContainKey("content");
        assertThat(selected.get("revealAt")).isNotNull();
        Map<String, Object> clue = awaitMessage(events);
        assertThat(clue).containsEntry("type", "CLUE_REVEALED");
        assertThat(clue).containsEntry("clueType", "SYNOPSIS").containsEntry("content", "○○○ 가족의 이야기")
                .containsEntry("revealedById", hostId).containsEntry("order", 1).containsEntry("total", 8);
        assertThat(awaitMessage(events)).containsEntry("type", "TURN_STARTED");
        await().atMost(MESSAGE_TIMEOUT).until(() -> countRevealedClues() == recordedBefore + 1);
        Map<String, Object> recorded = jdbcTemplate.queryForMap(
                "SELECT game_id, reveal_order FROM revealed_clue ORDER BY id DESC LIMIT 1");
        assertThat(((Number) recorded.get("REVEAL_ORDER")).intValue()).isEqualTo(1);
        assertThat(recorded.get("GAME_ID")).isNotNull();
    }

    @DisplayName("이미 열린 단서를 다시 열면 에러가 온다.")
    @Test
    void reveal_alreadyRevealed() {
        // given
        host.send("/app/games/start", Map.of("gameType", GAME_TYPE, "targetScore", 3));
        awaitEvent("TURN_STARTED");
        host.send("/app/games/clues/reveal", Map.of("clueType", "DIRECTOR"));
        awaitEvent("CLUE_REVEALED");

        // when
        host.send("/app/games/clues/reveal", Map.of("clueType", "DIRECTOR"));

        // then
        assertThat(awaitMessage(errors)).containsEntry("code", "GAME_CLUE_ALREADY_REVEALED");
    }

    @DisplayName("단서를 고른 뒤 공개 전에 정답이 나오면 그 단서는 공개하지 않는다.")
    @Test
    void reveal_solvedBeforeReveal() {
        // given
        host.send("/app/games/start", Map.of("gameType", GAME_TYPE, "targetScore", 3));
        awaitEvent("TURN_STARTED");
        host.send("/app/games/clues/reveal", Map.of("clueType", "DIRECTOR"));
        awaitEvent("CLUE_SELECTED");

        // when
        host.send("/app/rooms/chat", Map.of("text", ANSWER));

        // then
        awaitEvent("ROUND_SOLVED");
        assertThat(collectUntil("ROUND_STARTED")).extracting(event -> event.get("type"))
                .doesNotContain("CLUE_REVEALED");
    }

    @DisplayName("정답이 나오면 그 라운드의 차례는 더 오지 않고, 다음 라운드에서 새 차례가 온다.")
    @Test
    void turn_afterSolved() {
        // given
        host.send("/app/games/start", Map.of("gameType", GAME_TYPE, "targetScore", 3));
        awaitEvent("TURN_STARTED");

        // when
        host.send("/app/rooms/chat", Map.of("text", ANSWER));

        // then
        awaitEvent("ROUND_SOLVED");
        List<Map<String, Object>> untilNextRound = collectUntil("ROUND_STARTED");
        assertThat(untilNextRound).extracting(event -> event.get("type")).doesNotContain("TURN_STARTED");
        assertThat(awaitMessage(events)).containsEntry("type", "TURN_STARTED").containsEntry("roundNo", 2);
    }

    private int countRevealedClues() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM revealed_clue", Integer.class);
    }

    private Map<String, Object> awaitEvent(String type) {
        Map<String, Object> event = awaitMessage(events);
        while (!type.equals(event.get("type"))) {
            event = awaitMessage(events);
        }
        return event;
    }

    // type 이벤트가 올 때까지 받은 이벤트(그 이벤트 포함)를 모은다.
    private List<Map<String, Object>> collectUntil(String type) {
        List<Map<String, Object>> collected = new ArrayList<>();
        Map<String, Object> event = awaitMessage(events);
        collected.add(event);
        while (!type.equals(event.get("type"))) {
            event = awaitMessage(events);
            collected.add(event);
        }
        return collected;
    }
}

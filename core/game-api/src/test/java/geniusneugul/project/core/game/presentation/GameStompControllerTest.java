package geniusneugul.project.core.game.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import geniusneugul.project.core.support.WebSocketTestSupport;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.stomp.StompSession;

class GameStompControllerTest extends WebSocketTestSupport {

    private static final String ANSWER = "기생충";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 소켓 테스트는 테이블을 비우지 않으므로 같은 문제가 이미 있으면 다시 넣지 않는다.
    @BeforeEach
    void setUpQuestion() {
        jdbcTemplate.update("""
                INSERT INTO question (game_type, content_id, answer, sub_answer, active, review_status)
                SELECT 'MOVIE_STILL_CUT', 1, ?, 'Parasite', TRUE, 'APPROVED'
                WHERE NOT EXISTS (SELECT 1 FROM question WHERE game_type = 'MOVIE_STILL_CUT')
                """, ANSWER);
    }

    @DisplayName("방장이 게임을 시작하면 모두에게 게임 시작과 1라운드 시작이 간다.")
    @Test
    void start() throws Exception {
        // given
        StompSession host = connect();
        BlockingQueue<Map<String, Object>> events = createRoomAndSubscribe(host);

        // when
        host.send("/app/games/start", Map.of("gameType", "MOVIE_STILL_CUT", "targetScore", 3));

        // then
        Map<String, Object> started = awaitEvent(events, "GAME_STARTED");
        assertThat(started.get("targetScore")).isEqualTo(3);
        Map<String, Object> round = awaitEvent(events, "ROUND_STARTED");
        assertThat(round.get("roundNo")).isEqualTo(1);
        assertThat(round).doesNotContainKey("answer");
    }

    @DisplayName("라운드 중 정답 채팅은 채팅 대신 정답자 확정으로 가고, 잠시 뒤 다음 라운드가 열린다.")
    @Test
    void submit() throws Exception {
        // given
        StompSession host = connect();
        BlockingQueue<Map<String, Object>> events = createRoomAndSubscribe(host);
        host.send("/app/games/start", Map.of("gameType", "MOVIE_STILL_CUT", "targetScore", 3));
        awaitEvent(events, "ROUND_STARTED");

        // when
        host.send("/app/rooms/chat", Map.of("text", ANSWER));

        // then
        Map<String, Object> solved = awaitMessage(events);
        assertThat(solved.get("type")).isEqualTo("ROUND_SOLVED");
        assertThat(solved.get("answer")).isEqualTo(ANSWER);
        assertThat(solved.get("nextRoundAt")).isNotNull();
        assertThat(awaitEvent(events, "ROUND_STARTED").get("roundNo")).isEqualTo(2);
    }

    @DisplayName("정답으로 목표 점수에 도달하면 정답자 확정 뒤 게임 종료가 간다.")
    @Test
    void submit_reachTargetScore() throws Exception {
        // given
        StompSession host = connect();
        BlockingQueue<Map<String, Object>> events = createRoomAndSubscribe(host);
        host.send("/app/games/start", Map.of("gameType", "MOVIE_STILL_CUT", "targetScore", 1));
        awaitEvent(events, "ROUND_STARTED");

        // when
        host.send("/app/rooms/chat", Map.of("text", "parasite"));

        // then
        Map<String, Object> solved = awaitEvent(events, "ROUND_SOLVED");
        assertThat(solved.get("nextRoundAt")).isNull();
        Map<String, Object> finished = awaitEvent(events, "GAME_FINISHED");
        assertThat(finished.get("winnerId")).isEqualTo(solved.get("solverId"));
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

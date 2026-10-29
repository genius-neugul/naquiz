package geniusneugul.project.core.game.stillcut.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import geniusneugul.project.core.support.WebSocketTestSupport;
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

class StillCutStompTest extends WebSocketTestSupport {

    private static final String GAME_TYPE = "MOVIE_STILL_CUT";
    private static final String ANSWER = "기생충";
    private static final long MOVIE_ID = 7L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 소켓 테스트는 테이블을 비우지 않으므로 같은 문제·스틸컷이 이미 있으면 다시 넣지 않는다.
    // 같은 H2를 쓰는 다른 통합 테스트가 남긴 스틸컷 문제는 지워, 이 테스트의 문제만 출제되게 한다.
    @BeforeEach
    void setUpQuestion() {
        jdbcTemplate.update("DELETE FROM question WHERE game_type = ? AND content_id <> ?", GAME_TYPE, MOVIE_ID);
        jdbcTemplate.update("""
                INSERT INTO question (game_type, content_id, answer, sub_answer, active, review_status)
                SELECT ?, ?, ?, 'Parasite', TRUE, 'APPROVED'
                WHERE NOT EXISTS (SELECT 1 FROM question WHERE game_type = ?)
                """, GAME_TYPE, MOVIE_ID, ANSWER, GAME_TYPE);
        for (int order = 1; order <= 3; order++) {
            jdbcTemplate.update("""
                    INSERT INTO still_cut (movie_id, image_url, display_order)
                    SELECT ?, ?, ?
                    WHERE NOT EXISTS (SELECT 1 FROM still_cut WHERE movie_id = ? AND display_order = ?)
                    """, MOVIE_ID, "cut-" + order, order, MOVIE_ID, order);
        }
    }

    @DisplayName("스틸컷 라운드가 열리면 첫 스틸컷이 오고, 간격마다 다음 스틸컷이 온다.")
    @Test
    void reveal() throws Exception {
        // given
        StompSession host = connect();
        BlockingQueue<Map<String, Object>> events = createRoomAndSubscribe(host);

        // when
        host.send("/app/games/start", Map.of("gameType", GAME_TYPE, "targetScore", 3));

        // then
        awaitEvent(events, "ROUND_STARTED");
        Map<String, Object> first = awaitMessage(events);
        assertThat(first.get("type")).isEqualTo("STILL_CUT_REVEALED");
        assertThat(first).containsEntry("order", 1).containsEntry("total", 3).containsEntry("imageUrl", "cut-1");
        assertThat(first.get("nextAt")).isNotNull();
        Map<String, Object> second = awaitEvent(events, "STILL_CUT_REVEALED");
        assertThat(second).containsEntry("order", 2).containsEntry("imageUrl", "cut-2");
    }

    @DisplayName("정답이 나오면 그 라운드의 스틸컷은 더 오지 않고, 다음 라운드에서 첫 스틸컷부터 다시 온다.")
    @Test
    void reveal_afterSolved() throws Exception {
        // given
        StompSession host = connect();
        BlockingQueue<Map<String, Object>> events = createRoomAndSubscribe(host);
        host.send("/app/games/start", Map.of("gameType", GAME_TYPE, "targetScore", 3));
        awaitEvent(events, "STILL_CUT_REVEALED");

        // when
        host.send("/app/rooms/chat", Map.of("text", ANSWER));

        // then
        awaitEvent(events, "ROUND_SOLVED");
        List<Map<String, Object>> untilNextRound = collectUntil(events, "ROUND_STARTED");
        assertThat(untilNextRound).extracting(event -> event.get("type")).doesNotContain("STILL_CUT_REVEALED");
        Map<String, Object> next = awaitMessage(events);
        assertThat(next).containsEntry("type", "STILL_CUT_REVEALED").containsEntry("roundNo", 2).containsEntry("order", 1);
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

    // type 이벤트가 올 때까지 받은 이벤트(그 이벤트 포함)를 모은다.
    private List<Map<String, Object>> collectUntil(BlockingQueue<Map<String, Object>> events, String type) {
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

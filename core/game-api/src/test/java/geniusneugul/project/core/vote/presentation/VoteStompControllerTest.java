package geniusneugul.project.core.vote.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import geniusneugul.project.core.support.WebSocketTestSupport;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.stomp.StompSession;

class VoteStompControllerTest extends WebSocketTestSupport {

    /** 공통 흐름만 보려고 게임별 이벤트(스틸컷 등)가 없는 게임 종류를 쓴다 */
    private static final String GAME_TYPE = "MOVIE_TWENTY_QUESTIONS";
    private static final String ANSWER = "기생충";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 소켓 테스트는 테이블을 비우지 않으므로 같은 문제가 이미 있으면 다시 넣지 않는다.
    // 같은 H2를 쓰는 다른 통합 테스트가 남긴 문제는 지워, 이 테스트의 문제만 출제되게 한다.
    @BeforeEach
    void setUpQuestion() {
        jdbcTemplate.update("DELETE FROM question WHERE game_type = ? AND answer <> ?", GAME_TYPE, ANSWER);
        jdbcTemplate.update("""
                INSERT INTO question (game_type, content_id, answer, sub_answer, active, review_status)
                SELECT ?, 1, ?, 'Parasite', TRUE, 'APPROVED'
                WHERE NOT EXISTS (SELECT 1 FROM question WHERE game_type = ?)
                """, GAME_TYPE, ANSWER, GAME_TYPE);
    }

    @DisplayName("혼자 있는 방에서 스킵 투표를 열면 바로 통과해 정답이 공개되고, 잠시 뒤 다음 라운드가 열린다.")
    @Test
    void open_skipPassed() throws Exception {
        // given
        StompSession host = connect();
        BlockingQueue<Map<String, Object>> rooms = subscribe(host, "/user/queue/room");
        host.send("/app/rooms/create", Map.of("nickname", "방장"));
        BlockingQueue<Map<String, Object>> events = subscribe(host, "/topic/rooms/" + awaitMessage(rooms).get("roomId"));
        host.send("/app/games/start", Map.of("gameType", GAME_TYPE, "targetScore", 3));
        awaitEvent(events, "ROUND_STARTED");

        // when
        host.send("/app/votes/open", Map.of("voteType", "SKIP"));

        // then
        Map<String, Object> skipped = awaitMessage(events);
        assertThat(skipped.get("type")).isEqualTo("ROUND_SKIPPED");
        assertThat(skipped.get("roundNo")).isEqualTo(1);
        assertThat(skipped.get("answer")).isEqualTo(ANSWER);
        assertThat(skipped.get("nextRoundAt")).isNotNull();
        assertThat(awaitEvent(events, "ROUND_STARTED").get("roundNo")).isEqualTo(2);
    }

    @DisplayName("통과 기준을 채우지 못한 스킵 투표는 모두에게 찬성 현황과 필요한 찬성 수를 알린다.")
    @Test
    void open_notPassed() throws Exception {
        // given 3명이 있는 방
        StompSession host = connect();
        BlockingQueue<Map<String, Object>> rooms = subscribe(host, "/user/queue/room");
        host.send("/app/rooms/create", Map.of("nickname", "방장"));
        Map<String, Object> created = awaitMessage(rooms);
        BlockingQueue<Map<String, Object>> events = subscribe(host, "/topic/rooms/" + created.get("roomId"));
        for (String nickname : List.of("감자", "고구마")) {
            StompSession guest = connect();
            BlockingQueue<Map<String, Object>> guestRooms = subscribe(guest, "/user/queue/room");
            guest.send("/app/rooms/join", Map.of("inviteCode", created.get("inviteCode"), "nickname", nickname));
            awaitMessage(guestRooms);
        }
        host.send("/app/games/start", Map.of("gameType", GAME_TYPE, "targetScore", 3));
        awaitEvent(events, "ROUND_STARTED");

        // when
        host.send("/app/votes/open", Map.of("voteType", "SKIP"));

        // then
        Map<String, Object> updated = awaitEvent(events, "VOTE_UPDATED");
        assertThat(updated.get("voteType")).isEqualTo("SKIP");
        assertThat(updated.get("initiatorId")).isEqualTo(created.get("meId"));
        assertThat(updated.get("approverIds")).isEqualTo(List.of(created.get("meId")));
        assertThat(updated.get("requiredApprovals")).isEqualTo(2);
        assertThat(updated.get("participantCount")).isEqualTo(3);
    }

    private Map<String, Object> awaitEvent(BlockingQueue<Map<String, Object>> events, String type) {
        Map<String, Object> event = awaitMessage(events);
        while (!type.equals(event.get("type"))) {
            event = awaitMessage(events);
        }
        return event;
    }
}

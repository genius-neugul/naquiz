package geniusneugul.project.core.chat.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import geniusneugul.project.core.support.WebSocketTestSupport;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.simp.stomp.StompSession;

class ChatStompControllerTest extends WebSocketTestSupport {

    @DisplayName("참가자가 채팅을 보내면 방에 있는 모두에게 보낸 사람의 닉네임·태그와 앞뒤 공백을 뺀 본문이 간다.")
    @Test
    void send() throws Exception {
        // given
        StompSession host = connect();
        BlockingQueue<Map<String, Object>> hostRooms = subscribe(host, "/user/queue/room");
        host.send("/app/rooms/create", Map.of("nickname", "방장"));
        Map<String, Object> created = awaitMessage(hostRooms);
        String roomTopic = "/topic/rooms/" + created.get("roomId");
        BlockingQueue<Map<String, Object>> hostEvents = subscribe(host, roomTopic);

        StompSession guest = connect();
        BlockingQueue<Map<String, Object>> guestRooms = subscribe(guest, "/user/queue/room");
        guest.send("/app/rooms/join", Map.of("inviteCode", created.get("inviteCode"), "nickname", "감자"));
        Map<String, Object> joined = awaitMessage(guestRooms);
        BlockingQueue<Map<String, Object>> guestEvents = subscribe(guest, roomTopic);

        // when
        guest.send("/app/rooms/chat", Map.of("text", "  안녕하세요  "));

        // then
        for (Map<String, Object> chat : List.of(awaitChat(hostEvents), awaitChat(guestEvents))) {
            assertThat(chat.get("participantId")).isEqualTo(joined.get("meId"));
            assertThat(chat.get("nickname")).isEqualTo("감자");
            assertThat(chat.get("tag")).isEqualTo(2);
            assertThat(chat.get("text")).isEqualTo("안녕하세요");
            assertThat(chat.get("sentAt")).isNotNull();
        }
    }

    @DisplayName("채팅이 비었거나 100자를 넘으면 보낸 사람의 에러 큐로 요청 값 오류가 온다.")
    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void send_invalidLength(int length) throws Exception {
        // given
        StompSession session = connect();
        BlockingQueue<Map<String, Object>> rooms = subscribe(session, "/user/queue/room");
        BlockingQueue<Map<String, Object>> errors = subscribe(session, "/user/queue/errors");
        session.send("/app/rooms/create", Map.of("nickname", "방장"));
        awaitMessage(rooms);

        // when
        session.send("/app/rooms/chat", Map.of("text", "가".repeat(length)));

        // then
        assertThat(awaitMessage(errors).get("code")).isEqualTo("COMMON_INVALID_REQUEST");
    }

    @DisplayName("방에 들어가 있지 않은 연결이 채팅을 보내면 보낸 사람의 에러 큐로 방 미참가 오류가 온다.")
    @Test
    void send_notJoined() throws Exception {
        // given
        StompSession session = connect();
        BlockingQueue<Map<String, Object>> errors = subscribe(session, "/user/queue/errors");

        // when
        session.send("/app/rooms/chat", Map.of("text", "안녕하세요"));

        // then
        assertThat(awaitMessage(errors).get("code")).isEqualTo("ROOM_NOT_JOINED");
    }

    // 방 토픽에는 입장 이벤트도 오므로 채팅이 나올 때까지 건너뛴다.
    private Map<String, Object> awaitChat(BlockingQueue<Map<String, Object>> events) {
        Map<String, Object> event = awaitMessage(events);
        while (!"CHAT".equals(event.get("type"))) {
            event = awaitMessage(events);
        }
        return event;
    }
}

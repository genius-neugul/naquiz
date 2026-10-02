package geniusneugul.project.core.room.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import geniusneugul.project.core.room.domain.InviteCode;
import geniusneugul.project.core.room.infra.RoomRepository;
import geniusneugul.project.core.support.WebSocketTestSupport;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;

class RoomStompControllerTest extends WebSocketTestSupport {

    @Autowired
    private RoomRepository roomRepository;

    @DisplayName("방을 만들면 만든 사람에게 초대 코드와 방장 정보가 담긴 방 상태가 온다.")
    @Test
    @SuppressWarnings("unchecked")
    void create() throws Exception {
        // given
        StompSession session = connect();
        BlockingQueue<Map<String, Object>> rooms = subscribe(session, "/user/queue/room");

        // when
        session.send("/app/rooms/create", Map.of("nickname", "방장"));

        // then
        Map<String, Object> room = awaitMessage(rooms);
        assertThat((String) room.get("inviteCode")).matches("[A-Z0-9]{6}");
        assertThat(room.get("status")).isEqualTo("WAITING");
        List<Map<String, Object>> participants = (List<Map<String, Object>>) room.get("participants");
        assertThat(participants).singleElement().satisfies(host -> {
            assertThat(host.get("nickname")).isEqualTo("방장");
            assertThat(host.get("role")).isEqualTo("HOST");
            assertThat(host.get("participantId")).isEqualTo(room.get("meId"));
        });
    }

    @DisplayName("초대 코드로 들어가면 들어온 사람에게 방 상태가, 방장에게 입장 이벤트가 온다.")
    @Test
    @SuppressWarnings("unchecked")
    void join() throws Exception {
        // given
        StompSession host = connect();
        BlockingQueue<Map<String, Object>> hostRooms = subscribe(host, "/user/queue/room");
        host.send("/app/rooms/create", Map.of("nickname", "방장"));
        Map<String, Object> created = awaitMessage(hostRooms);
        BlockingQueue<Map<String, Object>> hostEvents = subscribe(host, "/topic/rooms/" + created.get("roomId"));

        StompSession guest = connect();
        BlockingQueue<Map<String, Object>> guestRooms = subscribe(guest, "/user/queue/room");

        // when
        guest.send("/app/rooms/join", Map.of("inviteCode", created.get("inviteCode"), "nickname", "감자"));

        // then
        Map<String, Object> joined = awaitMessage(guestRooms);
        List<Map<String, Object>> participants = (List<Map<String, Object>>) joined.get("participants");
        assertThat(participants).extracting(p -> p.get("nickname"), p -> p.get("tag"))
                .containsExactly(tuple("방장", 1), tuple("감자", 2));
        assertThat(joined.get("meId")).isEqualTo(participants.get(1).get("participantId"));

        Map<String, Object> event = awaitMessage(hostEvents);
        assertThat(event.get("type")).isEqualTo("PARTICIPANT_JOINED");
        assertThat(event.get("participantId")).isEqualTo(joined.get("meId"));
    }

    @DisplayName("없는 초대 코드로 들어가면 보낸 사람의 에러 큐로 방 없음 오류가 온다.")
    @Test
    void join_roomNotFound() throws Exception {
        // given
        StompSession session = connect();
        BlockingQueue<Map<String, Object>> errors = subscribe(session, "/user/queue/errors");

        // when
        session.send("/app/rooms/join", Map.of("inviteCode", "ZZZZZZ", "nickname", "감자"));

        // then
        assertThat(awaitMessage(errors).get("code")).isEqualTo("ROOM_NOT_FOUND");
    }

    @DisplayName("닉네임이 비어 있으면 보낸 사람의 에러 큐로 요청 값 오류가 온다.")
    @Test
    void create_blankNickname() throws Exception {
        // given
        StompSession session = connect();
        BlockingQueue<Map<String, Object>> errors = subscribe(session, "/user/queue/errors");

        // when
        session.send("/app/rooms/create", Map.of("nickname", " "));

        // then
        Map<String, Object> error = awaitMessage(errors);
        assertThat(error.get("code")).isEqualTo("COMMON_INVALID_REQUEST");
    }

    @DisplayName("방장의 연결이 끊기면 방이 사라진다.")
    @Test
    void disconnect_host() throws Exception {
        // given
        StompSession session = connect();
        BlockingQueue<Map<String, Object>> rooms = subscribe(session, "/user/queue/room");
        session.send("/app/rooms/create", Map.of("nickname", "방장"));
        InviteCode inviteCode = new InviteCode((String) awaitMessage(rooms).get("inviteCode"));

        // when
        session.disconnect();

        // then
        await().atMost(MESSAGE_TIMEOUT).until(() -> roomRepository.findByInviteCode(inviteCode).isEmpty());
    }

    private BlockingQueue<Map<String, Object>> subscribe(StompSession session, String destination) {
        BlockingQueue<Map<String, Object>> messages = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return jsonObject();
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.add((Map<String, Object>) payload);
            }
        });
        return messages;
    }

    private Map<String, Object> awaitMessage(BlockingQueue<Map<String, Object>> messages) {
        await().atMost(MESSAGE_TIMEOUT).until(() -> !messages.isEmpty());
        return messages.poll();
    }
}

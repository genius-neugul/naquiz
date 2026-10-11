package geniusneugul.project.core.support;

import static org.awaitility.Awaitility.await;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * WebSocket(STOMP) 테스트 공통 상위 클래스. 실제 포트로 서버를 띄우고 STOMP 클라이언트로 붙는다(docs/TEST.md 「WebSocket 테스트」).
 * 모든 소켓 테스트가 이 설정을 공유해야 context가 한 번만 뜬다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class WebSocketTestSupport {

    /** 메시지 도착을 기다리는 상한. 한곳에서만 조정한다 */
    protected static final Duration MESSAGE_TIMEOUT = Duration.ofSeconds(5);

    @LocalServerPort
    private int port;

    /** 새 연결 하나를 연다. 서버는 연결마다 다른 참가자로 본다 */
    protected StompSession connect() throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        return client.connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {
        }).get(MESSAGE_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    }

    /** destination을 구독하고 받은 JSON 본문을 쌓는 큐를 돌려준다 */
    protected BlockingQueue<Map<String, Object>> subscribe(StompSession session, String destination) {
        BlockingQueue<Map<String, Object>> messages = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.add((Map<String, Object>) payload);
            }
        });
        return messages;
    }

    protected Map<String, Object> awaitMessage(BlockingQueue<Map<String, Object>> messages) {
        await().atMost(MESSAGE_TIMEOUT).until(() -> !messages.isEmpty());
        return messages.poll();
    }
}

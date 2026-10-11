package geniusneugul.project.core.support;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
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

    @SuppressWarnings("unchecked")
    protected static Class<Map<String, Object>> jsonObject() {
        return (Class<Map<String, Object>>) (Class<?>) Map.class;
    }
}

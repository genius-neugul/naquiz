package geniusneugul.project.core.common.presentation;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * 비회원 참가자를 실시간 연결 단위로 식별한다. 연결마다 새 Principal을 붙이고, 그 이름을 참가자 토큰으로 쓴다.
 * 재접속을 지원하지 않으므로(docs/DOMAIN.md) 연결이 바뀌면 다른 참가자다.
 */
public class ConnectionHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        return new ConnectionPrincipal(UUID.randomUUID().toString());
    }

    record ConnectionPrincipal(String participantToken) implements Principal {

        @Override
        public String getName() {
            return participantToken;
        }
    }
}

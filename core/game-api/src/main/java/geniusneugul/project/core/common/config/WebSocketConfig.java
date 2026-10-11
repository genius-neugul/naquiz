package geniusneugul.project.core.common.config;

import geniusneugul.project.core.common.presentation.ConnectionHandshakeHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String[] allowedOrigins;

    public WebSocketConfig(@Value("${app.websocket.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setHandshakeHandler(new ConnectionHandshakeHandler())
                .setAllowedOriginPatterns(allowedOrigins);
        // 한 참가자가 보낸 메시지는 보낸 순서대로 처리한다(구독 후 요청, 채팅·정답 제출 순서).
        registry.setPreserveReceiveOrder(true);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
        // 같은 연결로 나가는 메시지는 보낸 순서대로 전달한다(한 사람이 연달아 보낸 채팅, 방 이벤트).
        registry.setPreservePublishOrder(true);
    }
}

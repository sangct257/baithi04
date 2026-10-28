package ra.demo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import ra.demo.security.filter.WebSocketAuthInterceptor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix để Client (Web/Mobile) subscribe nhận tin nhắn từ Server
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix cho các request gửi dữ liệu từ Client lên Server
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix dành cho gửi tin nhắn cá nhân (User-specific)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint 1: Hỗ trợ SockJS (Dành cho Web Frontend)
        registry.addEndpoint("/ws")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Endpoint 2: Raw WebSocket (Dành cho Postman / Native App Mobile)
        registry.addEndpoint("/ws-raw")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
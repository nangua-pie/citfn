package com.forum.common.config;

import com.forum.common.security.JwtTokenProvider;
import com.forum.message.websocket.ImChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * STOMP over WebSocket with Spring's simple in-memory broker.
 *
 * <p>Addressing follows AGENTS.md section 6.4: the handshake lives at
 * {@code /ws}, clients send to {@code /app/im.send} and each user has a private
 * destination at {@code /user/queue/im}. The simple broker is deliberate: an
 * external relay is only needed once more than one application instance runs.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ImChannelInterceptor imChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // SockJS fallback for clients that cannot use a raw WebSocket.
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setApplicationDestinationPrefixes("/app");
        // Turns "/user/queue/im" into a per-principal destination.
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(imChannelInterceptor);
    }

    /** Room for the payload size of an image message. */
    @org.springframework.context.annotation.Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(256 * 1024);
        container.setMaxBinaryMessageBufferSize(256 * 1024);
        container.setMaxSessionIdleTimeout(30 * 60 * 1000L);
        return container;
    }
}

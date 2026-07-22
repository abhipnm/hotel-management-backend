package com.restaurantmanager.config;

import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.security.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Authenticates the staff JWT from the STOMP CONNECT frame's own headers rather than the HTTP
 * handshake URL. A browser's WebSocket API can't set custom headers on the upgrade request, but
 * the STOMP CONNECT frame travels over the already-open socket, so the token never has to appear
 * in a URL - and therefore never ends up in server access logs, proxy logs, or browser history.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        String token = firstHeader(accessor, AUTH_HEADER);
        if (token != null && token.startsWith(BEARER_PREFIX)) {
            token = token.substring(BEARER_PREFIX.length());
        }

        if (token == null || token.isBlank() || sessionAttributes == null) {
            log.debug("Rejected websocket CONNECT: missing Authorization header");
            return null;
        }

        try {
            AuthPrincipal principal = jwtService.parse(token);
            if (principal.isGuest()) {
                log.debug("Rejected websocket CONNECT: guest tokens cannot open the kitchen feed");
                return null;
            }
            sessionAttributes.put("restaurantId", principal.restaurantId());
            sessionAttributes.put("userId", principal.id());
            return message;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected websocket CONNECT: invalid token ({})", e.getMessage());
            return null;
        }
    }

    private String firstHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }
}

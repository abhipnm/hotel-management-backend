package com.restaurantmanager.config;

import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.security.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * Kitchen/staff dashboards connect to the websocket with a staff JWT passed
 * as a query parameter (browsers cannot set custom headers during the
 * SockJS/STOMP handshake). Only valid STAFF/ADMIN tokens are allowed to
 * establish a connection; the resolved restaurantId is stashed in the
 * session attributes so handlers can scope subscriptions if needed...
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes) {

        List<String> tokenParams = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .get("token");

        if (tokenParams == null || tokenParams.isEmpty()) {

            log.debug("Rejected websocket handshake: missing token");
            return false;
        }

        try {
            AuthPrincipal principal = jwtService.parse(tokenParams.get(0));
            if (principal.isGuest()) {
                log.debug("Rejected websocket handshake: guest tokens cannot open the kitchen feed");
                return false;
            }
            attributes.put("restaurantId", principal.restaurantId());
            attributes.put("userId", principal.id());
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected websocket handshake: invalid token ({})", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            Exception exception) {
        // no-op
    }
}

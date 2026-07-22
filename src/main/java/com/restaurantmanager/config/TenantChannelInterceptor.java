package com.restaurantmanager.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every STOMP destination in this app is scoped as /topic/restaurants/{restaurantId}/...
 * The JWT handshake interceptor stashes the connecting principal's own restaurantId in the
 * session attributes; this rejects any SUBSCRIBE naming a DIFFERENT restaurantId, so one
 * tenant's staff can never listen in on another tenant's live order/alert/notification feed.
 */
@Slf4j
@Component
public class TenantChannelInterceptor implements ChannelInterceptor {

    private static final Pattern RESTAURANT_TOPIC = Pattern.compile("^/topic/restaurants/([^/]+)/.+");

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        String destination = accessor.getDestination();
        Matcher matcher = destination == null ? null : RESTAURANT_TOPIC.matcher(destination);
        if (matcher == null || !matcher.matches()) {
            return message;
        }

        String requestedRestaurantId = matcher.group(1);
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        Object ownRestaurantId = sessionAttributes == null ? null : sessionAttributes.get("restaurantId");

        if (ownRestaurantId == null || !ownRestaurantId.toString().equals(requestedRestaurantId)) {
            log.warn("Rejected cross-tenant websocket subscription to {} (session restaurantId={})",
                    destination, ownRestaurantId);
            return null;
        }
        return message;
    }
}

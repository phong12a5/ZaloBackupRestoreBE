package io.bomtech.device.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebUpdatesWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;

    // Store active sessions, mapping userId to a list of sessions (multiple tabs/browsers)
    private final Map<String, List<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    // Store sinks mapping userId to a list of sinks
    private final Map<String, List<Sinks.Many<String>>> userSinks = new ConcurrentHashMap<>();

    @Override
    @NonNull
    public Mono<Void> handle(@NonNull WebSocketSession session) {
        // Extract userId from the X-User-Name header added by the gateway
        HttpHeaders headers = session.getHandshakeInfo().getHeaders();
        String userId = headers.getFirst("X-User-Name");

        if (userId == null || userId.isEmpty()) {
            log.warn("WebSocket connection attempt without X-User-Name header. Closing session {}. URI: {}", session.getId(), session.getHandshakeInfo().getUri());
            // Consider if a different close status is more appropriate
            return session.close(org.springframework.web.reactive.socket.CloseStatus.POLICY_VIOLATION.withReason("User identification missing"));
        }

        log.info("Web client connected: User {} with session ID: {}", userId, session.getId());

        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        final String finalUserId = userId; // Need final variable for lambda

        // Add session and sink to the user's list
        userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(session);
        userSinks.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(sink);

        Mono<Void> input = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(message -> log.debug("Received message from web client {}: {}", finalUserId, message)) // Currently just logging web client messages
                .doOnError(error -> log.error("Error receiving message from web client {}: {}", finalUserId, error.getMessage()))
                .doFinally(signalType -> {
                    log.info("Web client disconnected: User {} (Session ID: {}), Signal: {}", finalUserId, session.getId(), signalType);
                    // Remove session and sink from the user's list
                    List<WebSocketSession> sessions = userSessions.get(finalUserId);
                    if (sessions != null) {
                        sessions.remove(session);
                        if (sessions.isEmpty()) {
                            userSessions.remove(finalUserId);
                        }
                    }
                    List<Sinks.Many<String>> sinks = userSinks.get(finalUserId);
                     if (sinks != null) {
                         // Remove the user's sinks if the session list becomes empty.
                         if (userSessions.get(finalUserId) == null || userSessions.get(finalUserId).isEmpty()) {
                             userSinks.remove(finalUserId);
                             log.debug("Removed sink list for user {} as no sessions remain.", finalUserId);
                         } else {
                             // Simplified: We don't remove individual sinks here to avoid complexity.
                             // If a user reconnects, they get a new sink.
                             // Old sinks for closed sessions will eventually be garbage collected or cleaned up if needed.
                             log.debug("Session {} closed for user {}. Other sessions may still be active.", session.getId(), finalUserId);
                         }
                    }
                })
                .then();

        // Send messages from the sink to the client
        Mono<Void> output = session.send(sink.asFlux().map(session::textMessage));

        return Mono.zip(input, output).then();
    }

    // Method to send updates to all sessions of a specific user
    public void sendUpdateToUser(String userId, Object updatePayload) {
        List<Sinks.Many<String>> sinks = userSinks.get(userId);
        if (sinks != null && !sinks.isEmpty()) {
            try {
                String message = objectMapper.writeValueAsString(updatePayload);
                log.info("Sending update to user {}: {}", userId, message);
                // Emit to all sinks for this user
                // Use a copy to avoid ConcurrentModificationException if a session disconnects during iteration
                new CopyOnWriteArrayList<>(sinks).forEach(sink -> {
                    try {
                        Sinks.EmitResult result = sink.tryEmitNext(message);
                        if (result.isFailure()) {
                            log.warn("Failed to emit message to sink for user {}. Result: {}", userId, result);
                            // Optionally handle specific failures like Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER
                        }
                    } catch (Exception e) {
                        log.error("Exception while emitting message to sink for user {}: {}", userId, e.getMessage());
                    }
                });
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize update payload for user {}: {}", userId, updatePayload, e);
            }
        } else {
            log.debug("No active web sessions found for user {} to send update.", userId);
        }
    }
}

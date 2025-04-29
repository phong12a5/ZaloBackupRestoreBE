package io.bomtech.device.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bomtech.device.model.Device;
import io.bomtech.device.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders; // Import HttpHeaders
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeviceWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;
    private static final String USER_ID_HEADER = "X-User-Name"; // Use the correct header name

    // Store active sessions, mapping deviceId to session and sink
    // Using Sinks.Many for broadcasting messages to specific clients if needed later
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Sinks.Many<String>> sinks = new ConcurrentHashMap<>(); // For sending messages TO devices

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Device reqDevice = extractDeviceInfo(session);
        if (reqDevice == null) {
            log.warn("WebSocket connection attempt without deviceId or {} header. Closing session {}.", USER_ID_HEADER, session.getId()); // Log correct header name
            return session.close();
        }

        // --- Get userId from Handshake Header ---
        HttpHeaders headers = session.getHandshakeInfo().getHeaders();
        String userId = headers.getFirst(USER_ID_HEADER); // Read correct header
        reqDevice.setUserId(userId); // Set userId in device object


        log.info("Device connected: {} (User: {}) with session ID: {}", reqDevice.getId(), userId, session.getId());

        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        sinks.put(reqDevice.getId(), sink);
        sessions.put(reqDevice.getId(), session);

        // Call service to handle connection logic (update DB status)
        // We subscribe here to trigger the action but don't block the handler setup
        DeviceService deviceService = applicationContext.getBean(DeviceService.class);
        deviceService.handleDeviceConnection(reqDevice).subscribe(
            device -> log.debug("Device {} connection handled successfully.", reqDevice.getId()),
            error -> log.error("Error handling connection for device {}: {}", reqDevice.getId(), error.getMessage())
        );

        final String finalDeviceId = reqDevice.getId(); // Use the deviceId extracted from header
        final String finalUserId = userId; // Use the userId extracted from header

        Mono<Void> input = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(message -> processDeviceMessage(finalDeviceId, finalUserId, message))
                .doOnError(error -> log.error("Error receiving message from device {}: {}", finalDeviceId, error.getMessage()))
                .doFinally(signalType -> {
                    log.info("Device disconnected: {} (Session ID: {}), Signal: {}", finalDeviceId, session.getId(), signalType);
                    sessions.remove(finalDeviceId);
                    sinks.remove(finalDeviceId);
                    // Call service to handle disconnection (update DB status)
                    // Use subscribe() to trigger the async operation
                    deviceService.handleDeviceDisconnection(finalDeviceId).subscribe(
                        null, // No action needed on completion
                        error -> log.error("Error handling disconnection for device {}: {}", finalDeviceId, error.getMessage())
                    );
                })
                .then();

        Mono<Void> output = session.send(sink.asFlux().map(session::textMessage));

        return Mono.zip(input, output).then();
    }

    // Placeholder for deviceId extraction logic
    private Device extractDeviceInfo(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery();
        Device device = null;
        if (query != null) {
            device = new Device();
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    switch (pair[0]) {
                        case "deviceId":
                            device.setId(pair[1]);
                            break;
                        case "deviceName":
                            device.setDeviceName(pair[1]);
                            break;
                        case "os":
                            device.setOs(pair[1]);
                            break;
                        case "appVersion":
                            device.setAppVersion(pair[1]);
                    }
                }
            }
        }
        if (device != null && device.getId() != null) {
            return device;
        }
        log.warn("Could not extract deviceId from session {}", session.getId());
        return null; // Or throw an exception / close session immediately
    }

    // Placeholder for processing incoming messages
    private void processDeviceMessage(String deviceId, String userId, String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String messageType = root.path("type").asText(); // Expecting a "type" field in JSON

            log.info("Processing message type '{}' from device {}", messageType, deviceId);
            DeviceService deviceService = applicationContext.getBean(DeviceService.class);

            switch (messageType) {
                case "BACKUP_STATUS_UPDATE":
                    String status = root.path("payload").path("status").asText(); // e.g., "INPROGRESS", "CANCELED", "FAILED"
                    String accountId = root.path("payload").path("accountId").asText(); // Zalo account being backed up
                    String statusMessage = root.path("payload").path("message").asText(); // Optional: details
                    deviceService.updateBackupStatus(deviceId, accountId, status, statusMessage)
                        .subscribe(
                            null,
                            error -> log.error("Error processing BACKUP_STATUS_UPDATE for device {}: {}", deviceId, error.getMessage())
                        );
                    break;

                case "BACKUP_COMPLETE":
                    String completedAccountId = root.path("payload").path("accountId").asText();
                    String accountName = root.path("payload").path("accountName").asText();
                    String phoneNumber = root.path("payload").path("phoneNumber").asText(); // Optional

                    // Update final status first
                    deviceService.updateBackupStatus(deviceId, completedAccountId, "COMPLETED", "Backup successful")
                        .then(
                            deviceService.saveBackedUpAccount(deviceId, userId, completedAccountId, accountName, phoneNumber)
                        )
                        .subscribe(
                            saved -> log.info("Backup result saved for device {}, account {}", deviceId, completedAccountId),
                            error -> log.error("Error processing BACKUP_COMPLETE for device {}: {}", deviceId, error.getMessage())
                        );
                    break;

                // Add other message types as needed (e.g., DEVICE_INFO_UPDATE, HEARTBEAT)
                // case "HEARTBEAT":
                //     log.debug("Received heartbeat from device {}", deviceId);
                //     // Optionally update device.lastSeen here or just rely on WebSocket activity
                //     break;

                default:
                    log.warn("Received unknown message type '{}' from device {}: {}", messageType, deviceId, message);
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON message from device {}: {}", deviceId, message, e);
        } catch (Exception e) {
            log.error("Failed to process message from device {}: {}", deviceId, message, e);
        }
    }

    // Method to send a command (e.g., start backup) to a specific device
    public Mono<Void> sendCommandToDevice(String deviceId, String command) {
        Sinks.Many<String> sink = sinks.get(deviceId);
        if (sink != null) {
            log.info("Sending command to device {}: {}", deviceId, command);
            sink.emitNext(command, Sinks.EmitFailureHandler.FAIL_FAST);
            return Mono.empty();
        } else {
            log.warn("Cannot send command, device {} not connected or sink not found.", deviceId);
            // Optionally return an error or handle offline devices
            return Mono.error(new RuntimeException("Device " + deviceId + " not connected."));
        }
    }

     // Method to check if a device is connected
     public boolean isDeviceConnected(String deviceId) {
         return sessions.containsKey(deviceId);
     }
}

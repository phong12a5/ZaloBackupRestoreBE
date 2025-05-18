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

    private void processDeviceMessage(String deviceId, String userId, String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String messageType = root.path("type").asText();
            JsonNode payload = root.path("payload"); // Get payload node

            log.info("Processing message type '{}' from device {}", messageType, deviceId);
            DeviceService deviceService = applicationContext.getBean(DeviceService.class);

            switch (messageType) {
                case "DEVICE_STATUS_UPDATE": {
                    if (payload.has("phoneNumber")) {
                        String accountId = payload.path("accountId").asText();
                        String phoneNumber = payload.path("phoneNumber").asText();
                        log.info("Received DEVICE_STATUS_UPDATE with accountId for device {}: phoneNumber={}", deviceId, phoneNumber);
                        deviceService.updateDeviceAccountId(deviceId, accountId, phoneNumber)
                            .subscribe(
                                updatedDevice -> log.info("Successfully updated accountId for device {}", deviceId),
                                error -> log.error("Error processing accountId update for device {}: {}", deviceId, error.getMessage())
                            );
                    }
                }
                    break;

                case "BACKUP_STATUS_UPDATE": {
                    String status = payload.path("status").asText();
                    String statusMessage = payload.path("message").asText();
                    String accountId = payload.has("accountId") ? payload.path("accountId").asText() : "";
                    String accountName = payload.has("accountName") ? payload.path("accountName").asText() : "";
                    String phoneNumber = payload.has("phoneNumber") ? payload.path("phoneNumber").asText() : "";
                    String backupPath = payload.has("backupPath") ? payload.path("backupPath").asText() : "";


                    log.info("Received BACKUP_STATUS_UPDATE for device {}: Status={}, AccountId={}, Message='{}'",
                             deviceId, status, accountId, statusMessage);

                    deviceService.updateBackupStatus(deviceId, accountId, phoneNumber, status, statusMessage)
                        .flatMap(updatedDevice -> {
                            if ("COMPLETED".equals(status)) {
                                log.info("Backup COMPLETED for device {}, saving account details for AccountId={}", deviceId, accountId);
                                return deviceService.saveBackedUpAccount(deviceId, userId, accountId, accountName, phoneNumber, backupPath)
                                        .thenReturn(updatedDevice);
                            } else {
                                return Mono.just(updatedDevice);
                            }
                        })
                        .subscribe(
                            device -> log.info("Processed BACKUP_STATUS_UPDATE for device {}, status: {}", deviceId, status),
                            error -> log.error("Error processing BACKUP_STATUS_UPDATE for device {}: {}", deviceId, error.getMessage())
                        );
                }
                    break;
                case "FRIENDS_EXPORT_STATUS_UPDATE": {
                    String status = payload.path("status").asText();
                    String statusMessage = payload.path("message").asText();
                    String data = payload.path("data").asText();
                    String accountId = payload.has("accountId") ? payload.path("accountId").asText() : "";

                    log.debug("Received FRIENDS_EXPORT_STATUS_UPDATE for device {}, accountId {}, status: {}", deviceId, accountId, status);
                    deviceService.updateFriendsExportStatus(deviceId, accountId, status, data, statusMessage)
                        .subscribe(
                            null, // onSuccess for Mono<Void>
                            error -> log.error("Error processing FRIENDS_EXPORT_STATUS_UPDATE subscription for device {}: {}", deviceId, error.getMessage()),
                            () -> log.debug("FRIENDS_EXPORT_STATUS_UPDATE processing chain completed for device {}", deviceId)
                        );
                }
                    break;
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

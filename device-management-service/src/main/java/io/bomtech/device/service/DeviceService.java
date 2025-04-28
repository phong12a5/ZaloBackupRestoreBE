package io.bomtech.device.service;

import io.bomtech.device.model.BackedUpAccount;
import io.bomtech.device.model.Device;
import io.bomtech.device.repository.BackedUpAccountRepository;
import io.bomtech.device.repository.DeviceRepository;
import io.bomtech.device.websocket.DeviceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor // Lombok for constructor injection of final fields
@Slf4j
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final BackedUpAccountRepository backedUpAccountRepository;
    private final DeviceWebSocketHandler webSocketHandler; // Inject WebSocket handler

    // --- Device Management ---

    public Flux<Device> getDevicesByUserId(String userId) {
        log.debug("Fetching devices for user: {}", userId);
        return deviceRepository.findByUserId(userId)
                // Update online status based on WebSocket connection
                .map(device -> {
                    device.setOnline(webSocketHandler.isDeviceConnected(device.getId()));
                    return device;
                });
    }

    public Mono<Device> getDeviceById(String deviceId) {
        log.debug("Fetching device by ID: {}", deviceId);
        return deviceRepository.findById(deviceId)
                .map(device -> {
                    device.setOnline(webSocketHandler.isDeviceConnected(device.getId()));
                    return device;
                });
    }

    // Called when a device connects via WebSocket
    public Mono<Device> handleDeviceConnection(String deviceId, String userId /*, other device info */) {
        log.info("Handling connection for device: {}", deviceId);
        return deviceRepository.findById(deviceId)
                .flatMap(device -> {
                    // Device found, update status
                    device.setOnline(true);
                    device.setLastSeen(Instant.now());
                    // TODO: Update other info if provided (e.g., appVersion, deviceName from handshake/initial message)
                    log.debug("Updating existing device {} status to online", deviceId);
                    return deviceRepository.save(device);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Device not found, create and save a new one
                    log.info("Device {} not found. Creating new device record for user {}.", deviceId, userId);
                    // TODO: Get deviceName, os, appVersion from handshake or initial message if possible
                    Device newDevice = new Device(deviceId, userId, "Unknown Device"); // Use a default name initially
                    newDevice.setOnline(true);
                    newDevice.setLastSeen(Instant.now());
                    return deviceRepository.save(newDevice);
                }));
    }

    // Called when a device disconnects
    public Mono<Void> handleDeviceDisconnection(String deviceId) {
        log.info("Handling disconnection for device: {}", deviceId);
        return deviceRepository.findById(deviceId)
                .flatMap(device -> {
                    device.setOnline(false);
                    device.setLastSeen(Instant.now()); // Record last seen time on disconnect
                    log.debug("Updating device {} status to offline", deviceId);
                    return deviceRepository.save(device);
                })
                .then(); // Convert Mono<Device> to Mono<Void>
    }

    // --- Backup Orchestration ---

    public Mono<Void> initiateBackup(String userId, String deviceId /*, potentially specific accountId */) {
        log.info("Initiating backup for device: {} by user: {}", deviceId, userId);
        // 1. Check if device exists and belongs to the user (optional but recommended)
        // 2. Check if device is online via WebSocketHandler
        if (!webSocketHandler.isDeviceConnected(deviceId)) {
             log.warn("Cannot initiate backup: Device {} is offline.", deviceId);
             return Mono.error(new RuntimeException("Device " + deviceId + " is offline."));
        }

        // 3. Construct the backup command (e.g., JSON message)
        //    This command structure needs to be agreed upon with the mobile app team.
        String backupCommand = "{\"command\": \"start_backup\"}"; // Simple example

        // 4. Send the command via WebSocketHandler
        return webSocketHandler.sendCommandToDevice(deviceId, backupCommand)
                .doOnSuccess(v -> log.info("Backup command sent successfully to device {}", deviceId))
                .doOnError(e -> log.error("Failed to send backup command to device {}: {}", deviceId, e.getMessage()));
        // Note: We don't wait for completion here. Status updates come via WebSocket messages.
    }

    // Called from WebSocketHandler when a "backup complete" message is received
    public Mono<BackedUpAccount> saveBackedUpAccount(String deviceId, String userId, String zaloAccountId, String zaloName, String zaloPhone) {
         log.info("Saving backed up account info for device {}, accountId {}", deviceId, zaloAccountId);
         // Consider checking if an entry for this user/zaloAccountId already exists
         // and update it instead of creating duplicates, or handle based on requirements.
         BackedUpAccount account = new BackedUpAccount(userId, deviceId, zaloAccountId, zaloName, zaloPhone);
         return backedUpAccountRepository.save(account)
                 .doOnSuccess(saved -> log.info("Successfully saved backed up account: {}", saved.getId()))
                 .doOnError(e -> log.error("Failed to save backed up account for device {}: {}", deviceId, e.getMessage()));
    }

    // --- Backed Up Account Retrieval ---

    public Flux<BackedUpAccount> getBackedUpAccountsByUserId(String userId) {
        log.debug("Fetching backed up accounts for user: {}", userId);
        return backedUpAccountRepository.findByUserId(userId);
    }

    // Method to handle backup status updates from devices
    public Mono<Void> updateBackupStatus(String deviceId, String zaloAccountId, String status, String message) {
         log.info("Updating backup status for device {}: Account={}, Status={}, Message='{}'",
                 deviceId, zaloAccountId, status, message);
         return deviceRepository.findById(deviceId)
                 .flatMap(device -> {
                     device.setLastBackupAccountId(zaloAccountId);
                     device.setLastBackupStatus(status);
                     device.setLastBackupTimestamp(Instant.now());
                     // TODO: Potentially store the 'message' as well if needed
                     return deviceRepository.save(device);
                 })
                 .doOnError(e -> log.error("Failed to update backup status for device {}: {}", deviceId, e.getMessage()))
                 .then(); // Convert Mono<Device> to Mono<Void>
         // TODO: Consider pushing this status update to connected web clients (e.g., via another WebSocket/SSE)
    }
}

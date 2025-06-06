package io.bomtech.device.service;

import io.bomtech.device.model.BackedUpAccount;
import io.bomtech.device.model.Device;
import io.bomtech.device.repository.BackedUpAccountRepository;
import io.bomtech.device.repository.DeviceRepository;
import io.bomtech.device.websocket.DeviceWebSocketHandler;
import io.bomtech.device.websocket.WebUpdatesWebSocketHandler; // Import new handler
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map; // For creating update payload

@Service
@RequiredArgsConstructor // Lombok for constructor injection of final fields
@Slf4j
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final BackedUpAccountRepository backedUpAccountRepository;
    private final DeviceWebSocketHandler webSocketHandler;
    private final WebUpdatesWebSocketHandler webUpdatesWebSocketHandler; // Declare the handler

    // Inject storage path from application.yml
    @Value("${app.backup.storage-path:/app/backups}") // Default path if not set
    private String backupStoragePath;

    // Inject APK path from application.properties
    @Value("${mobile.apk.zalo}")
    private String zaloApkFilePath;

    @Value("${mobile.apk.patched_zalo}")
    private String patchedZaloApkFilePath;

    @Value("${mobile.apk.zalobr}")
    private String zalobrApkFilePath;

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
    public Mono<Device> handleDeviceConnection(Device device) {
        log.info("Handling connection for device: {}", device.getId());
        return deviceRepository.findById(device.getId())
                .flatMap(existingDevice -> {
                    // Device found, update status
                    existingDevice.setOnline(true);
                    existingDevice.setLastSeen(Instant.now());
                    log.debug("Updating existing device {} status to online", existingDevice.getId());
                    return deviceRepository.save(existingDevice);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Device not found, create and save a new one
                    log.info("Device {} not found. Creating new device record for user {}.", device.getId(), device.getUserId());
                    device.setOnline(true);
                    device.setLastSeen(Instant.now());
                    return deviceRepository.save(device);
                }))
                .doOnSuccess(savedDevice -> {
                    // Send update to web clients
                    Map<String, Object> update = Map.of(
                        "type", "DEVICE_STATUS_UPDATE",
                        "payload", Map.of(
                            "deviceId", savedDevice.getId(),
                            "online", true,
                            "lastSeen", savedDevice.getLastSeen().toString()
                        )
                    );
                    webUpdatesWebSocketHandler.sendUpdateToUser(savedDevice.getUserId(), update);
                });
    }

    // Called when a device disconnects
    public Mono<Void> handleDeviceDisconnection(String deviceId) {
        log.info("Handling disconnection for device: {}", deviceId);
        return deviceRepository.findById(deviceId)
                .flatMap(device -> {
                    device.setOnline(false);
                    device.setLastSeen(Instant.now()); // Record last seen time on disconnect
                    log.debug("Updating device {} status to offline", deviceId);
                    return deviceRepository.save(device)
                           .doOnSuccess(savedDevice -> {
                               // Send update to web clients
                               Map<String, Object> update = Map.of(
                                   "type", "DEVICE_STATUS_UPDATE",
                                   "payload", Map.of(
                                       "deviceId", savedDevice.getId(),
                                       "online", false,
                                       "lastSeen", savedDevice.getLastSeen().toString()
                                   )
                               );
                               webUpdatesWebSocketHandler.sendUpdateToUser(savedDevice.getUserId(), update);
                           });
                })
                .then();
    }

    public Mono<Void> initiateBackup(String userId, String deviceId /*, potentially specific accountId */) {
        log.info("Initiating backup for device: {} by user: {}", deviceId, userId);
        if (!webSocketHandler.isDeviceConnected(deviceId)) {
             log.warn("Cannot initiate backup: Device {} is offline.", deviceId);
             return Mono.error(new RuntimeException("Device " + deviceId + " is offline."));
        }

        String backupCommand = "{\"command\": \"start_backup\"}";

        return webSocketHandler.sendCommandToDevice(deviceId, backupCommand)
                .doOnSuccess(v -> log.info("Backup command sent successfully to device {}", deviceId))
                .doOnError(e -> log.error("Failed to send backup command to device {}: {}", deviceId, e.getMessage()));
    }

    public Mono<Void> initiateFriendsExport(String userId, String deviceId) {
        log.info("Initiating friends export for device {} by user {}", deviceId, userId);
        // Check if device is connected
        if (!webSocketHandler.isDeviceConnected(deviceId)) {
            log.warn("Device {} is not connected. Cannot initiate friends export.", deviceId);
            return Mono.error(new IllegalStateException("Device not connected"));
        }
        // Send command to device via WebSocket
        String backupCommand = "{\"command\": \"export_friends\"}";
        return webSocketHandler.sendCommandToDevice(deviceId, backupCommand)
                .doOnSuccess(v -> log.info("Friends export command sent to device {}", deviceId))
                .doOnError(error -> log.error("Failed to send friends export command to device {}: {}", deviceId, error.getMessage()));
    }

    public Mono<BackedUpAccount> saveBackedUpAccount(String deviceId, String userId, String zaloAccountId, String zaloName, String zaloPhone, String backupFilePath) {
         log.info("Saving backed up account info for device {}, userId {}, accountId {}", deviceId, userId, zaloAccountId);

         return backedUpAccountRepository.findByUserIdAndZaloAccountId(userId, zaloAccountId)
                .flatMap(existingAccount -> {
                    log.info("BackedUpAccount for userId {} and zaloAccountId {} already exists. Updating details.", userId, zaloAccountId);
                    existingAccount.setZaloAccountName(zaloName);
                    existingAccount.setZaloPhoneNumber(zaloPhone);
                    existingAccount.setDeviceId(deviceId); 
                    existingAccount.setBackupFilePath(backupFilePath);
                    existingAccount.setBackupTimestamp(Instant.now());
                    return backedUpAccountRepository.save(existingAccount)
                            .doOnSuccess(updated -> log.info("Successfully updated existing backed up account: {}", updated.getId()));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("No existing BackedUpAccount found for userId {} and zaloAccountId {}. Creating new entry.", userId, zaloAccountId);
                    BackedUpAccount newAccount = new BackedUpAccount(userId, deviceId, zaloAccountId, zaloName, zaloPhone, backupFilePath);
                    return backedUpAccountRepository.save(newAccount)
                            .doOnSuccess(saved -> log.info("Successfully saved new backed up account: {}", saved.getId()));
                }))
                .doOnError(e -> log.error("Failed to save or update backed up account for userId {}, zaloAccountId {}: {}", userId, zaloAccountId, e.getMessage()));
    }

    public Flux<BackedUpAccount> getBackedUpAccountsByUserId(String userId) {
        log.debug("Fetching backed up accounts for user: {}", userId);
        return backedUpAccountRepository.findByUserId(userId);
    }

    public Mono<BackedUpAccount> getBackedUpAccountById(String backedUpAccountId, String requestingUserId) {
        log.debug("Fetching backed up account with id: {} for user: {}", backedUpAccountId, requestingUserId);
        return backedUpAccountRepository.findById(backedUpAccountId)
                .flatMap(account -> {
                    if (!account.getUserId().equals(requestingUserId)) {
                        log.warn("User {} attempted to access unauthorized backed up account {}", requestingUserId, backedUpAccountId);
                        return Mono.error(new SecurityException("Access denied to this backed up account."));
                    }
                    return Mono.just(account);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("BackedUpAccount with id {} not found", backedUpAccountId);
                    return Mono.empty(); // Or Mono.error(new NotFoundException(...))
                }));
    }

    // Method to handle backup status updates from devices
    public Mono<Device> updateBackupStatus(String deviceId, String zaloAccountId, String zaloPhoneNumber, String status, String message) {
         log.info("Updating backup status for device {}: Account={}, Status={}, Message='{}'",
                 deviceId, zaloAccountId, status, message);
         return deviceRepository.findById(deviceId)
                 .flatMap(device -> {
                     device.setActiveAccountId(zaloAccountId);
                     device.setActiveAccountPhone(zaloPhoneNumber);
                     device.setLastBackupStatus(status);
                     device.setLastBackupTimestamp(Instant.now());
                     return deviceRepository.save(device)
                            .doOnSuccess(savedDevice -> {
                                // Send update to web clients
                                Map<String, Object> update = Map.of(
                                    "type", "BACKUP_STATUS_UPDATE",
                                    "payload", Map.of(
                                        "deviceId", savedDevice.getId(),
                                        "accountId", zaloAccountId,
                                        "status", status,
                                        "message", message, // Include the message from device
                                        "timestamp", savedDevice.getLastBackupTimestamp().toString()
                                    )
                                );
                                webUpdatesWebSocketHandler.sendUpdateToUser(savedDevice.getUserId(), update);
                            });
                 })
                 .doOnError(e -> log.error("Failed to update backup status for device {}: {}", deviceId, e.getMessage()));
    }


    public Mono<Void> updateFriendsExportStatus(String deviceId, String zaloAccountId, String phoneNumber, String status, String data, String message) {
        log.info("Updating friends export status for device {}: AccountId={}, Status={}, Message='{}'", deviceId, zaloAccountId, status, message);
        return deviceRepository.findById(deviceId)
            .flatMap(device -> {
                log.info("Device {} found. Updating friends export status to: {}. AccountId: {}", deviceId, status, zaloAccountId);
                Map<String, Object> update = Map.of(
                    "type", "FRIENDS_EXPORT_STATUS_UPDATE",
                    "payload", Map.of(
                        "deviceId", device.getId(),
                        "accountId", zaloAccountId,
                        "phoneNumber", phoneNumber,
                        "status", status,
                        "data", data,
                        "message", message,
                        "timestamp", Instant.now()
                    )
                );
                webUpdatesWebSocketHandler.sendUpdateToUser(device.getUserId(), update);
                return deviceRepository.save(device);
            })
            .then() // Convert Mono<Device> from save() to Mono<Void>
            .doOnError(e -> log.error("Error updating friends export status for device {}: {}", deviceId, e.getMessage()));
    }
    /**
     * Updates only the activeAccountId for a given device and notifies web clients.
     *
     * @param deviceId  The ID of the device to update.
     * @param accountId The new Zalo Account ID to set.
     * @return A Mono emitting the updated Device, or empty if not found.
     */
    public Mono<Device> updateDeviceAccountId(String deviceId, String accountId, String accountPhone) {
        return deviceRepository.findById(deviceId)
                .flatMap(device -> {
                    log.info("Updating accountId for device {}: Old AccountId = {}, New AccountId = {}",
                             deviceId, device.getActiveAccountId(), accountId);
                    device.setActiveAccountId(accountId);
                    device.setActiveAccountPhone(accountPhone); // Update phone number if provided
                    // Optionally update lastSeen or another timestamp if needed
                    // device.setLastSeen(Instant.now());
                    return deviceRepository.save(device)
                           .doOnSuccess(savedDevice -> {
                               // Send update to web clients after successful save
                               Map<String, Object> update = Map.of(
                                   "type", "DEVICE_STATUS_UPDATE", // Use the same type
                                   "payload", Map.of(
                                       "deviceId", savedDevice.getId(),
                                       "activeAccountId", savedDevice.getActiveAccountId(), // Send the updated account ID
                                       "activeAccountPhone", savedDevice.getActiveAccountPhone()
                                       // Include other fields like online/lastSeen if they should be sent too
                                   )
                               );
                               // Assuming userId is needed for routing, fetch it if not readily available
                               // For simplicity, assuming device object has userId populated
                               if (savedDevice.getUserId() != null) {
                                    webUpdatesWebSocketHandler.sendUpdateToUser(savedDevice.getUserId(), update);
                               } else {
                                    log.warn("Cannot send accountId update to web client for device {}: userId is null", deviceId);
                               }
                           });
                })
                .doOnError(error -> log.error("Error updating accountId for device {}: {}", deviceId, error.getMessage()));
    }

    // --- Method to Save Uploaded Backup File ---
    public Mono<String> saveBackupFile(String userId, String deviceId, FilePart filePart) {
        // Create a unique filename (e.g., using timestamp)
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String originalFilename = filePart.filename();
        // Sanitize filename (basic example, consider more robust sanitization)
        String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String filename = timestamp + "_" + sanitizedFilename;

        // Create user-specific and device-specific directory structure
        Path userDevicePath = Paths.get(backupStoragePath, userId, deviceId);
        Path destinationPath = userDevicePath.resolve(filename);

        log.info("Attempting to save backup file to: {}", destinationPath);

        // Ensure directories exist
        try {
            Files.createDirectories(userDevicePath); // Create parent directories if they don't exist
        } catch (IOException e) {
            log.error("Failed to create directories for backup storage at {}: {}", userDevicePath, e.getMessage());
            return Mono.error(new IOException("Could not create storage directory.", e)); // Propagate error
        } catch (SecurityException e) {
             log.error("Permission denied to create directories at {}: {}", userDevicePath, e.getMessage());
             return Mono.error(new SecurityException("Permission denied for storage directory.", e));
        }

        // Transfer the file reactively
        return filePart.transferTo(destinationPath)
                .then(Mono.fromRunnable(() -> log.info("Successfully saved backup file: {}", destinationPath)))
                .thenReturn(destinationPath.toString()) // Return the full path of the saved file
                .onErrorMap(IOException.class, e -> { // Map IOExceptions during transfer
                    log.error("IOException during file transfer to {}: {}", destinationPath, e.getMessage());
                    return new IOException("Failed to save file due to IO error during transfer.", e);
                })
                 .onErrorMap(IllegalStateException.class, e -> { // Handle cases like file already transferred
                    log.error("IllegalStateException during file transfer to {}: {}", destinationPath, e.getMessage());
                    return new IOException("Failed to save file, possibly already processed.", e);
                });
    }

    // --- Method to get the APK file as a Resource ---
    public Mono<Resource> getApkResource(String apkType) {
        log.info("Attempting to load APK resource of type: {}", apkType);
        String apkFilePath;
        if ("zalo".equalsIgnoreCase(apkType)) {
            apkFilePath = zaloApkFilePath;
        } else if ("patched_zalo".equalsIgnoreCase(apkType)) {
            apkFilePath = patchedZaloApkFilePath;
        } else if ("zalobr".equalsIgnoreCase(apkType)) {
            apkFilePath = zalobrApkFilePath;
        } else {
            log.error("Invalid APK type requested: {}", apkType);
            return Mono.error(new IllegalArgumentException("Invalid APK type: " + apkType));
        }
        log.debug("APK file path: {}", apkFilePath);
        Path path = Paths.get(apkFilePath);
        log.info("Attempting to load APK from path: {}", path);
        if (!Files.exists(path) || !Files.isReadable(path)) {
            log.error("APK file not found or not readable at path: {}", apkFilePath);
            return Mono.error(new IOException("APK file not found or not readable: " + apkFilePath));
        }
        return Mono.just(new FileSystemResource(path));
    }

    // --- Method to Download Backup File ---
    public Mono<Resource> downloadBackupFile(String backedUpAccountId, String requestingUserId) {
        log.info("Attempting to download backup file for accountId: {} by userId: {}", backedUpAccountId, requestingUserId);
        return backedUpAccountRepository.findById(backedUpAccountId)
                .flatMap(backedUpAccount -> {
                    // Verify if the requesting user is the owner of the backup
                    if (!backedUpAccount.getUserId().equals(requestingUserId)) {
                        log.warn("Access denied for userId {} attempting to download backup for accountId {}", requestingUserId, backedUpAccountId);
                        return Mono.error(new SecurityException("Access denied to this backup file."));
                    }

                    Path backupPath = Paths.get(backedUpAccount.getBackupFilePath());
                    log.debug("Backup file path for accountId {}: {}", backedUpAccountId, backupPath);

                    if (!Files.exists(backupPath) || !Files.isReadable(backupPath)) {
                        log.error("Backup file not found or not readable at path: {}", backupPath);
                        return Mono.error(new IOException("Backup file not found or not readable: " + backupPath));
                    }
                    Resource resource = new FileSystemResource(backupPath);
                    return Mono.just(resource);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("No BackedUpAccount found with id: {}", backedUpAccountId);
                    return Mono.error(new IOException("Backup record not found for id: " + backedUpAccountId)); // Or a custom NotFoundException
                }))
                .doOnError(e -> log.error("Error during backup file download for accountId {}: {}", backedUpAccountId, e.getMessage()));
    }

    // --- Method to Delete a BackedUpAccount ---
    public Mono<Void> deleteBackedUpAccount(String backedUpAccountId, String requestingUserId) {
        log.info("User {} attempting to delete backed up account with ID: {}", requestingUserId, backedUpAccountId);
        return backedUpAccountRepository.findById(backedUpAccountId)
            .flatMap(account -> {
                if (!account.getUserId().equals(requestingUserId)) {
                    log.warn("User {} attempted to delete account {} owned by {}. Denying request.",
                            requestingUserId, backedUpAccountId, account.getUserId());
                    return Mono.error(new SecurityException("User does not have permission to delete this account backup."));
                }

                // Attempt to delete the physical backup file first
                Path backupFilePath = Paths.get(account.getBackupFilePath());
                Mono<Void> deleteFileMono;
                if (Files.exists(backupFilePath)) {
                    try {
                        Files.delete(backupFilePath);
                        log.info("Successfully deleted backup file: {} for accountId: {}", backupFilePath, backedUpAccountId);
                        deleteFileMono = Mono.empty();
                    } catch (IOException e) {
                        log.error("Failed to delete backup file {} for accountId: {}. Error: {}", backupFilePath, backedUpAccountId, e.getMessage());
                        // Decide if this should be a critical error or just a warning. 
                        // For now, we'll log and continue to delete the DB record.
                        deleteFileMono = Mono.empty(); // Or Mono.error(e) if file deletion failure should stop the process
                    }
                } else {
                    log.warn("Backup file not found at path: {} for accountId: {}. Skipping file deletion.", backupFilePath, backedUpAccountId);
                    deleteFileMono = Mono.empty();
                }

                return deleteFileMono.then(backedUpAccountRepository.delete(account))
                    .doOnSuccess(v -> log.info("Successfully deleted backed up account record with ID: {}", backedUpAccountId))
                    .doOnError(e -> log.error("Failed to delete backed up account record with ID: {}. Error: {}", backedUpAccountId, e.getMessage()));
            })
            .switchIfEmpty(Mono.defer(() -> {
                log.warn("BackedUpAccount with ID: {} not found for deletion attempt by user: {}", backedUpAccountId, requestingUserId);
                // Return an error or complete normally if not finding it is acceptable.
                // For a delete operation, not finding it could be considered a success or a client error (e.g., 404).
                // Here, we'll treat it as if the resource is already gone, so complete normally.
                return Mono.empty(); 
            }));
    }

    public Flux<BackedUpAccount> transferBackedUpAccounts(List<String> backedUpAccountIds, String targetUserId, String requestingUserId) {
        log.info("User {} attempting to transfer {} accounts to user {}", requestingUserId, backedUpAccountIds.size(), targetUserId);
        if (requestingUserId.equals(targetUserId)) {
            log.warn("Requesting user {} and target user {} are the same. No transfer needed.", requestingUserId, targetUserId);
            return Flux.error(new IllegalArgumentException("Target user cannot be the same as the current owner."));
        }

        // It's important to ensure the targetUserId is a valid, existing user.
        // This might involve a call to the user-service. For this example, we'll assume it's valid.
        // if (!userService.isValidUser(targetUserId)) { // Pseudocode for user validation
        //    return Flux.error(new IllegalArgumentException("Target user ID is not valid."));
        // }

        return Flux.fromIterable(backedUpAccountIds)
            .flatMap(accountId -> backedUpAccountRepository.findById(accountId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Transfer failed: BackedUpAccount with id {} not found.", accountId);
                    // Optionally, collect these errors and return a partial success/failure response
                    return Mono.error(new RuntimeException("Account not found: " + accountId)); // Or a custom NotFoundException
                }))
                .flatMap(account -> {
                    if (!account.getUserId().equals(requestingUserId)) {
                        log.warn("User {} is not authorized to transfer account {}. Current owner: {}",
                                 requestingUserId, accountId, account.getUserId());
                        return Mono.error(new SecurityException("Unauthorized to transfer account: " + accountId));
                    }
                    log.debug("Transferring account {} from user {} to user {}", accountId, account.getUserId(), targetUserId);
                    account.setUserId(targetUserId);
                    // Potentially update other fields, e.g., clear deviceId if it's user-specific
                    // account.setDeviceId(null); 
                    return backedUpAccountRepository.save(account)
                        .doOnSuccess(savedAccount -> log.info("Successfully transferred account {} to user {}", savedAccount.getId(), targetUserId))
                        .doOnError(err -> log.error("Error saving transferred account {}: {}", accountId, err.getMessage()));
                })
            );
    }
}

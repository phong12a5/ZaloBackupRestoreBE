package io.bomtech.device.controller;

import io.bomtech.device.model.BackedUpAccount;
import io.bomtech.device.model.Device;
import io.bomtech.device.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Slf4j
public class DeviceController {

    private final DeviceService deviceService;
    private static final String USER_ID_HEADER = "X-User-Name"; // Use the correct header name

    // Helper to get userId from header or return error Mono
    private Mono<String> getUserIdFromHeader(String userIdHeader) {
        if (!StringUtils.hasText(userIdHeader)) {
            log.warn("Missing or empty {} header", USER_ID_HEADER); // Log correct header name
            // Return an error Mono that translates to 401 Unauthorized
            return Mono.error(new MissingUserIdHeaderException("Unauthorized: Missing user ID header"));
        }
        return Mono.just(userIdHeader);
    }

    // Custom exception for mapping to 401
    private static class MissingUserIdHeaderException extends RuntimeException {
        public MissingUserIdHeaderException(String message) {
            super(message);
        }
    }

    // Global exception handler for this controller
    @ExceptionHandler(MissingUserIdHeaderException.class)
    public ResponseEntity<String> handleMissingUserIdHeader(MissingUserIdHeaderException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }


    // Get all devices for the user specified in the header
    @GetMapping("/user/me")
    public Mono<ResponseEntity<Flux<Device>>> getMyDevices(@RequestHeader(USER_ID_HEADER) String userIdHeader) { // Read correct header
         return getUserIdFromHeader(userIdHeader).map(userId -> {
             log.info("API request: Get devices for user {}", userId);
             return ResponseEntity.ok(deviceService.getDevicesByUserId(userId));
         });
         // Error handled by @ExceptionHandler
    }

    // Get a specific device by its ID (verify ownership implicitly via service logic if needed)
    @GetMapping("/{deviceId}")
    public Mono<ResponseEntity<Device>> getDeviceById(@PathVariable String deviceId,
                                                      @RequestHeader(USER_ID_HEADER) String userIdHeader) { // Read correct header
         return getUserIdFromHeader(userIdHeader).flatMap(userId -> {
             log.info("API request: Get device by ID {} for user {}", deviceId, userId);
             // Ownership check should ideally happen in the service layer based on userId
             return deviceService.getDeviceById(deviceId)
                     // Optional: Filter here if service doesn't check ownership
                     // .filter(device -> userId.equals(device.getUserId()))
                     .map(ResponseEntity::ok)
                     .defaultIfEmpty(ResponseEntity.notFound().build());
         });
         // Error handled by @ExceptionHandler
    }

    // Endpoint to trigger the backup process on a device
    @PostMapping("/{deviceId}/backup")
    public Mono<ResponseEntity<Void>> requestBackup(@PathVariable String deviceId,
                                                    @RequestHeader(USER_ID_HEADER) String userIdHeader) { // Read correct header
        return getUserIdFromHeader(userIdHeader).flatMap(userId -> {
            log.info("API request: Initiate backup for device {} by user {}", deviceId, userId);
            // TODO: Add ownership check in DeviceService before initiating if needed
            return deviceService.initiateBackup(userId, deviceId)
                    .then(Mono.just(ResponseEntity.accepted().<Void>build()))
                    .onErrorResume(e -> {
                        log.error("API Error initiating backup for device {}: {}", deviceId, e.getMessage());
                        if (e.getMessage().contains("offline")) {
                            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
                        }
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                    });
        });
         // Error handled by @ExceptionHandler
    }

    // --- API Endpoint for Uploading Backup File ---
    @PostMapping("/{deviceId}/backup/upload")
    public Mono<ResponseEntity<String>> uploadBackupFile(
            @PathVariable String deviceId,
            @RequestHeader(USER_ID_HEADER) String userIdHeader,
            @RequestPart("file") Mono<FilePart> filePartMono) { // Receive file as "file" part

        return getUserIdFromHeader(userIdHeader).flatMap(userId ->
            filePartMono.flatMap(filePart -> {
                log.info("API request: Upload backup file for device {} by user {}", deviceId, userId);
                // Delegate file saving to the service
                return deviceService.saveBackupFile(userId, deviceId, filePart)
                    .map(savedPath -> ResponseEntity.ok("File uploaded successfully to: " + savedPath)) // Return path on success
                    .onErrorResume(e -> {
                        log.error("Failed to upload backup file for device {}: {}", deviceId, e.getMessage());
                        // Return appropriate error response based on exception type
                        if (e instanceof SecurityException) { // Example: Permission denied
                             return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body("Permission denied to save file."));
                        } else if (e instanceof java.io.IOException) { // Example: Disk full / IO error
                             return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save file due to IO error."));
                        }
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file."));
                    });
            })
        );
        // Error for missing header handled by @ExceptionHandler
    }

    // --- API Endpoint for Downloading APK ---
    @GetMapping("/apk")
    public Mono<ResponseEntity<Resource>> downloadApk() {
        log.info("API request: Download APK file");
        return deviceService.getApkResource()
                .map(resource -> {
                    HttpHeaders headers = new HttpHeaders();
                    // Suggest a filename for the download
                    headers.setContentDispositionFormData("attachment", "app-release.apk");
                    // Set the correct content type for APK files
                    headers.setContentType(MediaType.parseMediaType("application/vnd.android.package-archive"));

                    // Spring Boot should automatically handle Content-Length for FileSystemResource
                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(resource);
                })
                .onErrorResume(e -> {
                    log.error("API Error getting APK resource: {}", e.getMessage());
                    // Handle specific errors like file not found
                    if (e instanceof java.io.IOException && e.getMessage().contains("not found")) {
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
                    }
                    // Generic internal server error for other issues
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null));
                });
    }

    // Get all backed up accounts for the user specified in the header
    @GetMapping("/user/me/accounts")
    public Mono<ResponseEntity<Flux<BackedUpAccount>>> getMyBackedUpAccounts(@RequestHeader(USER_ID_HEADER) String userIdHeader) { // Read correct header
         return getUserIdFromHeader(userIdHeader).map(userId -> {
             log.info("API request: Get backed up accounts for user {}", userId);
             return ResponseEntity.ok(deviceService.getBackedUpAccountsByUserId(userId));
         });
         // Error handled by @ExceptionHandler
    }
}

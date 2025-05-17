package io.bomtech.device.controller;

import io.bomtech.device.dto.FileUploadResponse; // Import the new DTO
import io.bomtech.device.dto.TransferAccountsRequest;
import io.bomtech.device.model.BackedUpAccount;
import io.bomtech.device.model.Device;
import io.bomtech.device.service.DeviceService;
import io.bomtech.device.websocket.DeviceWebSocketHandler; // Import DeviceWebSocketHandler
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException; // Import for explicit error handling
// Remove Spring Security imports if no longer used directly for JWT principal
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Slf4j
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceWebSocketHandler deviceWebSocketHandler; // Add this field

    private static final String USER_ID_HEADER = "X-User-Name"; // Use the correct header name
    private static final String USER_ROLE_HEADER = "X-User-Role"; // Use the correct header name

    // Helper to get userId from header or return error Mono
    private Mono<String> getUserIdFromHeader(String userIdHeader) {
        if (!StringUtils.hasText(userIdHeader)) {
            log.warn("Missing or empty {} header", USER_ID_HEADER); // Log correct header name
            // Using ResponseStatusException for clearer error propagation
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Missing user ID header"));
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

    @PostMapping("/{deviceId}/backup")
    public Mono<ResponseEntity<Void>> requestBackup(@PathVariable String deviceId,
                                                    @RequestHeader(USER_ID_HEADER) String userIdHeader) {
        return getUserIdFromHeader(userIdHeader).flatMap(userId -> {
            log.info("API request: Initiate backup for device {} by user {}", deviceId, userId);
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
    }

    @PostMapping("/{deviceId}/backup/upload")
    public Mono<ResponseEntity<FileUploadResponse>> uploadBackupFile(
            @PathVariable String deviceId,
            @RequestHeader(USER_ID_HEADER) String userIdHeader,
            @RequestPart("file") Mono<FilePart> filePartMono) {

        return getUserIdFromHeader(userIdHeader).flatMap(userId ->
            filePartMono.flatMap(filePart -> {
                log.info("API request: Upload backup file for device {} by user {}", deviceId, userId);
                return deviceService.saveBackupFile(userId, deviceId, filePart)
                    .map(savedPath -> {
                        FileUploadResponse response = new FileUploadResponse("File uploaded successfully.", savedPath);
                        return ResponseEntity.ok(response);
                    })
                    .onErrorResume(e -> {
                        log.error("Failed to upload backup file for device {}: {}", deviceId, e.getMessage(), e); // Log exception too
                        String errorMessage = "Failed to upload file.";
                        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
                        String errorFilePath = null;

                        if (e instanceof SecurityException) {
                            errorMessage = "Permission denied to save file.";
                            status = HttpStatus.FORBIDDEN;
                        } else if (e instanceof java.io.IOException) {
                            errorMessage = "Failed to save file due to IO error.";
                        } else if (e instanceof ResponseStatusException) {
                            ResponseStatusException rse = (ResponseStatusException) e;
                            FileUploadResponse errorResponse = new FileUploadResponse(rse.getReason(), null);
                            return Mono.just(ResponseEntity.status(rse.getStatusCode()).body(errorResponse));
                        }
                        FileUploadResponse errorResponse = new FileUploadResponse(errorMessage, errorFilePath);
                        return Mono.just(ResponseEntity.status(status).body(errorResponse));
                    });
            })
        ).onErrorResume(ResponseStatusException.class, e -> {
            FileUploadResponse errorResponse = new FileUploadResponse(e.getReason(), null);
            return Mono.just(ResponseEntity.status(e.getStatusCode()).body(errorResponse));
        });
    }

    @GetMapping("/apk/{apkType}")
    public Mono<ResponseEntity<Resource>> downloadApk(
        @PathVariable String apkType
    ) {
        log.info("API request: Download APK file");
        return deviceService.getApkResource(apkType)
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

    // --- Endpoint to Get a Specific Backup Account by ID ---
    @GetMapping("/backups/{backedUpAccountId}")
    public Mono<ResponseEntity<BackedUpAccount>> getBackedUpAccountById(
            @PathVariable String backedUpAccountId,
            @RequestHeader(USER_ID_HEADER) String userIdHeader) {

        return getUserIdFromHeader(userIdHeader).flatMap(userId -> {
            log.info("API request: Get backed up account by ID {} for user {}", backedUpAccountId, userId);
            return deviceService.getBackedUpAccountById(backedUpAccountId, userId)
                    .map(ResponseEntity::ok)
                    .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
                    .onErrorResume(SecurityException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build())
                    );
        }).onErrorResume(ResponseStatusException.class, e ->
            Mono.just(ResponseEntity.status(e.getStatusCode()).build())
        );
    }

    // --- Endpoint to Download a Specific Backup File ---
    @GetMapping("/backups/download/{backedUpAccountId}")
    public Mono<ResponseEntity<Resource>> downloadBackupFile(
            @PathVariable String backedUpAccountId,
            @RequestHeader(USER_ID_HEADER) String userIdHeader) { // Get userId from header

        return getUserIdFromHeader(userIdHeader).flatMap(userId -> {
            log.info("Received request to download backup file with ID: {} for user: {}", backedUpAccountId, userId);

            return deviceService.downloadBackupFile(backedUpAccountId, userId)
                    .flatMap(resource -> {
                        try {
                            Path filePath = Paths.get(resource.getURI()); // Get path to extract filename
                            String filename = filePath.getFileName().toString();

                            HttpHeaders headers = new HttpHeaders();
                            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
                            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
                            headers.add(HttpHeaders.PRAGMA, "no-cache");
                            headers.add(HttpHeaders.EXPIRES, "0");

                            return Mono.just(ResponseEntity.ok()
                                    .headers(headers)
                                    .contentType(MediaType.APPLICATION_OCTET_STREAM) // General binary type
                                    .contentLength(resource.contentLength())
                                    .body(resource));
                        } catch (IOException e) {
                            log.error("Error accessing file resource for backup ID {}: {}", backedUpAccountId, e.getMessage());
                            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<Resource>body(null)); // Ensure type
                        }
                    })
                    .onErrorResume(SecurityException.class, e -> {
                        log.warn("SecurityException for backup download {}: {}", backedUpAccountId, e.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).<Resource>body(null)); // Ensure type
                    })
                    .onErrorResume(IOException.class, e -> {
                        log.error("IOException for backup download {}: {}", backedUpAccountId, e.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).<Resource>body(null)); // Ensure type
                    })
                    .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).<Resource>body(null))); // Ensure type
        }).onErrorResume(ResponseStatusException.class, e ->
            // Handle cases where getUserIdFromHeader fails (e.g., missing header)
            Mono.just(ResponseEntity.status(e.getStatusCode()).<Resource>body(null)) // Ensure type
        );
    }

    // --- Endpoint to Delete a Specific Backup Account by ID ---
    @DeleteMapping("/backups/{backedUpAccountId}")
    public Mono<ResponseEntity<Void>> deleteBackedUpAccount(
            @PathVariable String backedUpAccountId,
            @RequestHeader(USER_ID_HEADER) String userIdHeader) {
        log.info("API request: Delete backed up account with ID: {} for user: {}", backedUpAccountId, userIdHeader);
        return getUserIdFromHeader(userIdHeader)
                .flatMap(userId -> deviceService.deleteBackedUpAccount(backedUpAccountId, userId)
                        .then(Mono.just(ResponseEntity.ok().<Void>build()))
                )
                .onErrorResume(ResponseStatusException.class, e -> 
                    Mono.just(ResponseEntity.status(e.getStatusCode()).build()) // Use getStatusCode()
                )
                .onErrorResume(Exception.class, e -> {
                    log.error("Error deleting backed up account {}: {}", backedUpAccountId, e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PostMapping("/backups/transfer")
    public Mono<ResponseEntity<Flux<BackedUpAccount>>> transferAccounts(
            @RequestHeader(USER_ROLE_HEADER) String role,
            @RequestHeader(USER_ID_HEADER) String userIdHeader,
            @RequestBody TransferAccountsRequest transferRequest) {

        if (!"ADMIN".equalsIgnoreCase(role)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(Flux.error(new SecurityException("Unauthorized: Insufficient permissions."))));
        }

        return getUserIdFromHeader(userIdHeader).flatMap(requestingUserId -> {
            log.info("API request: User {} to transfer accounts to user {}", requestingUserId, transferRequest.getTargetUserId());
            if (transferRequest.getBackedUpAccountIds() == null || transferRequest.getBackedUpAccountIds().isEmpty()) {
                log.warn("Transfer request from user {} is missing account IDs.", requestingUserId);
                return Mono.just(ResponseEntity.badRequest().body(Flux.error(new IllegalArgumentException("BackedUpAccountIds list cannot be empty."))));
            }
            if (!StringUtils.hasText(transferRequest.getTargetUserId())) {
                log.warn("Transfer request from user {} is missing targetUserId.", requestingUserId);
                return Mono.just(ResponseEntity.badRequest().body(Flux.error(new IllegalArgumentException("TargetUserId cannot be empty."))));
            }

            Flux<BackedUpAccount> transferredAccounts = deviceService.transferBackedUpAccounts(
                transferRequest.getBackedUpAccountIds(),
                transferRequest.getTargetUserId(),
                requestingUserId
            ).doOnNext(backedUpAccount -> {
                String deviceId = backedUpAccount.getDeviceId(); // Assuming BackedUpAccount has getDeviceId()
                if (deviceId != null) {
                    // Define the command as a JSON string
                    String command = "{\"command\": \"refresh_account\"}";
                    log.info("Attempting to send refresh_account command to device {} for transferred account {}", deviceId, backedUpAccount.getId());
                    // Send command and subscribe to trigger it. Log errors if any.
                    deviceWebSocketHandler.sendCommandToDevice(deviceId, command)
                        .doOnSuccess(v -> log.info("Successfully sent REFRESH_ACCOUNTS command to device {} for account {}", deviceId, backedUpAccount.getId()))
                        .doOnError(e -> log.warn("Failed to send REFRESH_ACCOUNTS command to device {} for account {}: {}. Device might be offline or command failed.", deviceId, backedUpAccount.getId(), e.getMessage()))
                        .subscribe(); // Fire-and-forget, errors are logged by sendCommandToDevice and here
                } else {
                    log.warn("Cannot send REFRESH_ACCOUNTS command: deviceId is null for backedUpAccount {}", backedUpAccount.getId());
                }
            });

            // Handling potential errors from the service layer within the response
            return Mono.just(ResponseEntity.ok()
                .body(transferredAccounts
                    .onErrorResume(IllegalArgumentException.class, e -> {
                        log.warn("Transfer failed due to invalid argument for user {}: {}", requestingUserId, e.getMessage());
                        // It's tricky to return specific HTTP status codes for individual items in a Flux here.
                        // The overall operation is OK, but individual items might have failed.
                        // Consider a more complex response DTO if granular error reporting per account is needed.
                        return Flux.error(e); // Propagate error to be handled by global error handlers or client
                    })
                    .onErrorResume(SecurityException.class, e -> {
                        log.warn("Transfer failed due to security exception for user {}: {}", requestingUserId, e.getMessage());
                        return Flux.error(e); // Propagate error
                    })
                    .onErrorResume(RuntimeException.class, e -> {
                        // Catch other runtime exceptions like account not found
                        log.error("Transfer failed due to runtime exception for user {}: {}", requestingUserId, e.getMessage());
                        return Flux.error(e);
                    })
                )
            );
        });
    }
}

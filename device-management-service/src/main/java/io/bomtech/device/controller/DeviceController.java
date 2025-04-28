package io.bomtech.device.controller;

import io.bomtech.device.model.BackedUpAccount;
import io.bomtech.device.model.Device;
import io.bomtech.device.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

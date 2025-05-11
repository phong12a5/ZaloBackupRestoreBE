package io.bomtech.device.repository;

import io.bomtech.device.model.BackedUpAccount;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BackedUpAccountRepository extends ReactiveMongoRepository<BackedUpAccount, String> {

    // Find all backed up accounts for a specific user
    Flux<BackedUpAccount> findByUserId(String userId);

    // Find all backed up accounts from a specific device
    Flux<BackedUpAccount> findByDeviceId(String deviceId);

    // Find by user and zalo account ID (to potentially update existing records)
    Mono<BackedUpAccount> findByUserIdAndZaloAccountId(String userId, String zaloAccountId);
}

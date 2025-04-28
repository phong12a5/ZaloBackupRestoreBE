package io.bomtech.device.repository;

import io.bomtech.device.model.Device;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DeviceRepository extends ReactiveMongoRepository<Device, String> {

    // Find all devices associated with a specific user
    Flux<Device> findByUserId(String userId);

    // Find a specific device by its ID
    Mono<Device> findById(String id);
}

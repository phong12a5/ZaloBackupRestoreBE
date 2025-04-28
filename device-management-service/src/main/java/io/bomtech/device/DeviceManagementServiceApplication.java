package io.bomtech.device;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient; // Hoặc @EnableEurekaClient

@SpringBootApplication
@EnableDiscoveryClient // Cho phép đăng ký với Eureka
public class DeviceManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeviceManagementServiceApplication.class, args);
    }

}

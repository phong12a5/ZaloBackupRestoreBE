package io.bomtech.device.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data // Lombok annotation for getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok for no-args constructor
@Document(collection = "backed_up_accounts") // Maps this class to the MongoDB collection
public class BackedUpAccount {

    @Id
    private String id; // MongoDB document ID

    @Indexed // Index this field for faster queries by userId
    private String userId; // ID of the user owning this account backup

    @Indexed
    private String deviceId; // ID of the device that performed the backup

    private String zaloAccountId; // Unique identifier for the Zalo account
    private String zaloAccountName; // Display name
    private String zaloPhoneNumber; // Phone number (optional, consider privacy)
    private Instant backupTimestamp; // When the backup was completed
    // Add other relevant fields as needed
    // private String backupDataLocation;
    // private Map<String, Object> otherMetadata;

    public BackedUpAccount(String userId, String deviceId, String zaloAccountId, String zaloAccountName, String zaloPhoneNumber) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.zaloAccountId = zaloAccountId;
        this.zaloAccountName = zaloAccountName;
        this.zaloPhoneNumber = zaloPhoneNumber;
        this.backupTimestamp = Instant.now(); // Set timestamp on creation
    }
}

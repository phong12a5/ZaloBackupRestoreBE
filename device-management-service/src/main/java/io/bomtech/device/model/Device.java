package io.bomtech.device.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@Document(collection = "devices")
public class Device {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String deviceName;
    private String os;
    private String appVersion;
    private Instant lastSeen;
    private boolean online;

    /**
     * Possible values: INIT, BACKING_UP, UPLOADING, BACKUP_FAILED, UPLOAD_FAILED, CANCELED, COMPLETED
     */
    private String lastBackupStatus;
    /**
     * Zalo Account ID currently logged in on the device.
     */
    private String activeAccountId;
    private Instant lastBackupTimestamp;
}

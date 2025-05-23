// Based on device-management-service/src/main/java/io/bomtech/device/model/Device.java
export interface Device {
  id: string;
  userId: string;
  deviceName: string;
  os?: string; // Optional fields based on model
  appVersion?: string;
  lastSeen?: string; // ISO 8601 date string
  online: boolean;
  activeAccountId?: string;
  activeAccountPhone?: string;
  lastBackupStatus?: string;
  lastBackupTimestamp?: string; // ISO 8601 date string
}

// Based on device-management-service/src/main/java/io/bomtech/device/model/BackedUpAccount.java
export interface BackedUpAccount {
  id: string; // MongoDB ObjectId as string
  userId: string;
  deviceId: string;
  zaloAccountId: string;
  zaloAccountName: string;
  zaloPhoneNumber?: string; // Optional
  backupTimestamp: string; // ISO 8601 date string
}

// You might also want types for User, Auth responses etc.
// export interface User { ... }

// Based on user-service/src/main/java/io/bomtech/user/model/UserSafeDto.java
export interface UserSafeDto {
  id: string;
  username: string;
  fullname?: string; // Optional, adjust if your DTO has it
  role: string;
}
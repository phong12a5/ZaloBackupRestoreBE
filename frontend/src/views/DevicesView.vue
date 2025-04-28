<template>
  <div class="devices-view">
    <h1>My Devices</h1>

    <div v-if="isLoading" class="loading-message">Loading devices...</div>
    <div v-if="error" class="error-message">Error loading devices: {{ error }}</div>

    <div v-if="!isLoading && !error && devices.length === 0" class="no-devices">
      No devices found. Connect a device using the mobile app.
    </div>

    <table v-if="!isLoading && devices.length > 0" class="devices-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>Status</th>
          <th>OS</th>
          <th>App Version</th>
          <th>Last Seen</th>
          <th>Last Backup</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="device in devices" :key="device.id">
          <td>{{ device.deviceName }}</td>
          <td>
            <span :class="['status-badge', device.online ? 'status-online' : 'status-offline']">
              {{ device.online ? 'Online' : 'Offline' }}
            </span>
          </td>
          <td>{{ device.os || 'N/A' }}</td>
          <td>{{ device.appVersion || 'N/A' }}</td>
          <td>{{ formatTimestamp(device.lastSeen) }}</td>
          <td>
             <span v-if="device.lastBackupTimestamp">
               {{ device.lastBackupStatus || 'Unknown' }} ({{ formatTimestamp(device.lastBackupTimestamp) }})
             </span>
             <span v-else>Never</span>
          </td>
          <td>
            <button
              @click="triggerBackup(device.id)"
              :disabled="!device.online || backupInProgress[device.id]"
              class="action-button backup-button"
            >
              {{ backupInProgress[device.id] ? 'Backing up...' : 'Start Backup' }}
            </button>
            <!-- Add other actions like 'Remove Device' later -->
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import { getMyDevices, requestDeviceBackup } from '@/api/deviceApi';
import type { Device } from '@/types';

const devices = ref<Device[]>([]);
const isLoading = ref(true);
const error = ref<string | null>(null);
// Track backup state per device
const backupInProgress = reactive<Record<string, boolean>>({});

const fetchDevices = async () => {
  isLoading.value = true;
  error.value = null;
  try {
    devices.value = await getMyDevices();
  } catch (err: any) {
    console.error('Failed to fetch devices:', err);
    error.value = err.response?.data?.message || err.message || 'An unknown error occurred';
  } finally {
    isLoading.value = false;
  }
};

const triggerBackup = async (deviceId: string) => {
  if (backupInProgress[deviceId]) return; // Prevent multiple clicks

  backupInProgress[deviceId] = true;
  try {
    await requestDeviceBackup(deviceId);
    // Optionally show a success message (e.g., using a toast notification library)
    console.log(`Backup initiated for device ${deviceId}`);
    alert(`Backup initiated for device ${deviceId}. Status updates will appear here or via notifications.`); // Simple alert
    // Note: Actual progress/completion comes via WebSocket or polling
  } catch (err: any) {
    console.error(`Failed to initiate backup for device ${deviceId}:`, err);
    // Show error message to the user
    alert(`Error starting backup: ${err.response?.data?.message || err.message}`);
  } finally {
    // Reset button state after a short delay or based on WebSocket feedback
     setTimeout(() => { backupInProgress[deviceId] = false; }, 5000); // Simple reset after 5s
  }
};

const formatTimestamp = (timestamp?: string): string => {
  if (!timestamp) return 'N/A';
  try {
    // Assuming timestamp is ISO 8601 UTC, display in local time
    return new Date(timestamp).toLocaleString();
  } catch (e) {
    return 'Invalid Date';
  }
};

// Fetch devices when the component mounts
onMounted(fetchDevices);

// TODO: Implement WebSocket connection here or in a service
// to update device online status and backup progress in real-time.
</script>

<style scoped>
.devices-view {
  padding: 1.5rem 2rem;
}

h1 {
  margin-bottom: 1.5rem;
  color: #2c3e50;
  font-weight: 600;
}

.loading-message, .error-message, .no-devices {
  margin-top: 1rem;
  padding: 1rem;
  border-radius: 4px;
}
.loading-message { background-color: #e0e0e0; }
.error-message { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
.no-devices { background-color: #e2e3e5; color: #383d41; }

.devices-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 1rem;
  background-color: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  overflow: hidden; /* Ensures border-radius applies to table */
}

th, td {
  padding: 0.8rem 1rem;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
  font-size: 0.9rem;
  vertical-align: middle; /* Align content vertically */
}

th {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #495057;
}

tbody tr:last-child td {
  border-bottom: none;
}

tbody tr:hover {
  background-color: #f1f3f5;
}

.status-badge {
  padding: 0.2em 0.6em;
  border-radius: 10px;
  font-size: 0.8em;
  font-weight: bold;
  color: #fff;
  white-space: nowrap;
}

.status-online {
  background-color: #28a745; /* Green */
}

.status-offline {
  background-color: #6c757d; /* Gray */
}

.action-button {
  padding: 0.4rem 0.8rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85rem;
  transition: background-color 0.2s ease;
  white-space: nowrap;
}

.backup-button {
  background-color: #007bff;
  color: white;
}

.backup-button:hover:not(:disabled) {
  background-color: #0056b3;
}

.action-button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
  opacity: 0.6;
}
</style>
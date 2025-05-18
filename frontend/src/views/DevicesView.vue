<template>
  <div class="devices-view">
    <h2>My Devices</h2>

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
          <th>Account</th>
          <th>Backup Status</th>
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
          <td>{{ device.activeAccountPhone || 'N/A' }}</td> <!-- New Cell -->
          <td>
             <span v-if="device.lastBackupTimestamp">
               {{ device.lastBackupStatus || 'Unknown' }} ({{ formatTimestamp(device.lastBackupTimestamp) }})
             </span>
             <span v-else>Never</span>
          </td>
          <td>
            <button
              @click="triggerBackup(device.id)"
              :disabled="!device.online || backupInProgress[device.id] || !device.activeAccountPhone"
              class="action-button backup-button"
            >
              {{ backupInProgress[device.id] ? 'Backing up...' : 'Start Backup' }}
            </button>
            <button
              @click="triggerFriendsExport(device.id)"
              :disabled="!device.online || exportInProgress[device.id] || !device.activeAccountPhone"
              class="action-button export-friends-button"
              style="margin-left: 8px;"
            >
              {{ exportInProgress[device.id] ? 'Exporting...' : 'Export Friends' }}
            </button>
            <!-- Add other actions like 'Remove Device' later -->
          </td>
        </tr>
      </tbody>
    </table>
    <Notification
      :message="notificationMessage"
      :type="notificationType"
      :show="showNotification"
      @update:show="showNotification = $event"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, reactive } from 'vue';
import { getMyDevices, requestDeviceBackup, requestFriendsExport } from '@/api/deviceApi';
import type { Device } from '@/types';
import Notification from '@/components/Notification.vue'; // Import the Notification component

const devices = ref<Device[]>([]);
const isLoading = ref(true);
const error = ref<string | null>(null);
// Track backup state per device
const backupInProgress = reactive<Record<string, boolean>>({});
// Track export friends state per device
const exportInProgress = reactive<Record<string, boolean>>({});
const ws = ref<WebSocket | null>(null); // WebSocket instance

// Notification state
const showNotification = ref(false);
const notificationMessage = ref('');
const notificationType = ref<'success' | 'error' | 'info' | 'warning'>('info');

// Function to trigger notification
const triggerNotification = (message: string, type: 'success' | 'error' | 'info' | 'warning' = 'info') => {
  notificationMessage.value = message;
  notificationType.value = type;
  showNotification.value = true;
  // The Notification component itself will handle hiding after its duration
};

// Function to construct WebSocket URL
const getWebSocketURL = (): string | null => {
  const token = localStorage.getItem('authToken'); // Assuming token is stored here
  if (!token) {
    console.error('Access token not found for WebSocket connection.');
    error.value = 'Authentication token missing. Cannot connect for real-time updates.';
    return null;
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const host = window.location.hostname; // This will be zalo.ink
  // Construct URL without explicit port for wss/ws, as Nginx handles routing via standard ports (443/80)
  const url = `${protocol}//${host}/ws/web/updates?token=${encodeURIComponent(token)}`;
  console.log('WebSocket URL:', url); // Log the constructed URL for debugging
  return url;
};

// Function to handle incoming WebSocket messages
const handleWebSocketMessage = (event: MessageEvent) => {
  try {
    const update = JSON.parse(event.data);
    console.log('WebSocket message received:', update);

    const payload = update.payload;
    if (!payload || !payload.deviceId) {
      console.warn('Received message without deviceId:', update);
      return;
    }

    const deviceIndex = devices.value.findIndex(d => d.id === payload.deviceId);
    if (deviceIndex === -1) {
      console.warn(`Received update for unknown device ID: ${payload.deviceId}`);
      return;
    }

    const deviceToUpdate = devices.value[deviceIndex];

    if (update.type === 'DEVICE_STATUS_UPDATE') {
        // Handle online status
        if (payload.hasOwnProperty('online')) {
            deviceToUpdate.online = payload.online;
        }
        // Handle last seen time
        if (payload.hasOwnProperty('lastSeen')) {
            deviceToUpdate.lastSeen = payload.lastSeen;
        }
        // Handle account ID update (added)
        if (payload.hasOwnProperty('activeAccountPhone')) {
            deviceToUpdate.activeAccountPhone = payload.activeAccountPhone;
        }
    } else if (update.type === 'BACKUP_STATUS_UPDATE') {
        // ... existing BACKUP_STATUS_UPDATE logic ...
        if (payload.hasOwnProperty('status')) {
            const newStatus = payload.status;
            deviceToUpdate.lastBackupStatus = newStatus;
            // Update backupInProgress flag
            if (['BACKING_UP', 'UPLOADING'].includes(newStatus)) {
                 backupInProgress[deviceToUpdate.id] = true;
            } else if (['INIT', 'COMPLETED', 'BACKUP_FAILED', 'UPLOAD_FAILED', 'CANCELED'].includes(newStatus)) {
                 backupInProgress[deviceToUpdate.id] = false;
            } else {
                 backupInProgress[deviceToUpdate.id] = false;
                 console.warn(`Received unknown backup status: ${newStatus}`);
            }
        }
         if (payload.hasOwnProperty('timestamp')) {
             deviceToUpdate.lastBackupTimestamp = payload.timestamp;
         }
    } else if (update.type === 'FRIENDS_EXPORT_STATUS_UPDATE') { 
        if (payload.hasOwnProperty('status')) {
            const newStatus = payload.status;
            if (['EXPORTING_FRIENDS'].includes(newStatus)) {
                 exportInProgress[deviceToUpdate.id] = true;
            } else if (['COMPLETED_FRIENDS_EXPORT', 'FAILED_FRIENDS_EXPORT'].includes(newStatus)) {
                 exportInProgress[deviceToUpdate.id] = false;
                  if (newStatus === 'COMPLETED_FRIENDS_EXPORT' && payload.data) {
                      const exportedFriends = payload.data;
                      navigator.clipboard.writeText(exportedFriends).then(() => {
                          console.log('Exported friends copied to clipboard');
                          triggerNotification('Exported friends copied to clipboard', 'success');
                      }).catch(err => {
                          console.error('Failed to copy exported friends to clipboard:', err);
                          triggerNotification('Failed to copy exported friends to clipboard.', 'error');
                      });
                  } else if (newStatus === 'FAILED_FRIENDS_EXPORT') {
                    triggerNotification(payload.message || 'Failed to export friends.', 'error');
                  }
            } else {
                 exportInProgress[deviceToUpdate.id] = false;
                 console.warn(`Received unknown friends export status: ${newStatus}`);
            }
        }
        // if (payload.hasOwnProperty('timestamp')) {
        //     deviceToUpdate.lastExportTimestamp = payload.timestamp;
        // }
    } else {
        console.warn(`Received unhandled message type: ${update.type}`);
    }

    devices.value[deviceIndex] = { ...deviceToUpdate };

  } catch (e) {
    console.error('Failed to parse WebSocket message or update device:', e);
  }
};

// Function to setup WebSocket connection
const connectWebSocket = () => {
  const url = getWebSocketURL();
  if (!url) return; // Don't connect if URL couldn't be created (e.g., no token)

  if (ws.value && ws.value.readyState === WebSocket.OPEN) {
    console.log('WebSocket already connected.');
    return;
  }

  ws.value = new WebSocket(url);

  ws.value.onopen = () => {
    console.log('WebSocket connection established for real-time updates.');
    error.value = null; // Clear any previous errors on successful connection
  };

  ws.value.onmessage = handleWebSocketMessage;

  ws.value.onerror = (event) => {
    console.error('WebSocket error:', event);
    error.value = 'WebSocket connection error. Real-time updates may be unavailable.';
    // Optional: Implement retry logic here
  };

  ws.value.onclose = (event) => {
    console.log('WebSocket connection closed:', event.reason, `Code: ${event.code}`);
    ws.value = null; // Clear the ref
    // Optional: Attempt to reconnect after a delay, unless closed intentionally
    if (!event.wasClean) {
        error.value = `WebSocket disconnected unexpectedly (Code: ${event.code}). Attempting to reconnect...`;
        setTimeout(connectWebSocket, 5000); // Reconnect after 5 seconds
    } else {
         error.value = 'WebSocket connection closed.';
    }
  };
};

const fetchDevices = async () => {
  isLoading.value = true;
  error.value = null;
  try {
    devices.value = await getMyDevices();
    // Initialize backupInProgress and exportInProgress states for fetched devices
    devices.value.forEach(device => {
        if (backupInProgress[device.id] === undefined) {
             backupInProgress[device.id] = false;
        }
        if (exportInProgress[device.id] === undefined) {
             exportInProgress[device.id] = false;
        }
    });
  } catch (err: any) {
    console.error('Failed to fetch devices:', err);
    error.value = err.response?.data?.message || err.message || 'An unknown error occurred';
  } finally {
    isLoading.value = false;
  }
};

const triggerBackup = async (deviceId: string) => {
  if (backupInProgress[deviceId] || exportInProgress[deviceId]) return;

  backupInProgress[deviceId] = true; // Set immediately for UI feedback
  try {
    await requestDeviceBackup(deviceId);
    console.log(`Backup initiated for device ${deviceId}`);
    triggerNotification('Backup request sent.', 'info'); 
  } catch (err: any) {
    console.error(`Failed to initiate backup for device ${deviceId}:`, err);
    triggerNotification(err.response?.data?.message || err.message || 'Error starting backup', 'error');
    backupInProgress[deviceId] = false; // Reset on error
  }
};

const triggerFriendsExport = async (deviceId: string) => {
  if (exportInProgress[deviceId] || backupInProgress[deviceId]) return;

  exportInProgress[deviceId] = true; // Set immediately for UI feedback
  try {
    await requestFriendsExport(deviceId);
    console.log(`Friends export initiated for device ${deviceId}`);
  } catch (err: any) {
    console.error(`Failed to initiate friends export for device ${deviceId}:`, err);
    triggerNotification(err.response?.data?.message || err.message || 'Error starting friends export', 'error');
    exportInProgress[deviceId] = false; // Reset on error if no WebSocket update is expected for this failure
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

// Fetch devices and connect WebSocket when the component mounts
onMounted(() => {
  fetchDevices();
  connectWebSocket();
});

// Close WebSocket connection when the component unmounts
onUnmounted(() => {
  if (ws.value) {
    console.log('Closing WebSocket connection.');
    ws.value.close(1000, 'Component unmounted'); // 1000 is normal closure
    ws.value = null;
  }
});
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
  white-space: nowrap; /* Prevent text wrapping in cells */
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

.export-friends-button {
  background-color: #17a2b8; /* Example: Teal color */
  color: white;
}

.export-friends-button:hover:not(:disabled) {
  background-color: #117a8b;
}

.action-button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
  opacity: 0.6;
}
</style>
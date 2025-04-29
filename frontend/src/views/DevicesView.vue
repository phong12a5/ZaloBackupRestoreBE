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
          <td>{{ device.lastBackupAccountId || 'N/A' }}</td> <!-- New Cell -->
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
import { ref, onMounted, onUnmounted, reactive } from 'vue'; // Import onUnmounted
import { getMyDevices, requestDeviceBackup } from '@/api/deviceApi';
import type { Device } from '@/types';

const devices = ref<Device[]>([]);
const isLoading = ref(true);
const error = ref<string | null>(null);
// Track backup state per device
const backupInProgress = reactive<Record<string, boolean>>({});
const ws = ref<WebSocket | null>(null); // WebSocket instance

// Function to construct WebSocket URL
const getWebSocketURL = (): string | null => {
  const token = localStorage.getItem('authToken'); // Assuming token is stored here
  if (!token) {
    console.error('Access token not found for WebSocket connection.');
    error.value = 'Authentication token missing. Cannot connect for real-time updates.';
    return null;
  }
  // Construct URL based on current window location, targeting the API Gateway port (8080)
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  // Use API Gateway's host and port. Assuming gateway runs on the same host as frontend serves from, but port 8080.
  // Adjust hostname/port if gateway is elsewhere.
  const host = window.location.hostname;
  const gatewayPort = 8080; // As defined in api-gateway application.yml
  const url = `${protocol}//${host}:${gatewayPort}/ws/web/updates?token=${encodeURIComponent(token)}`;
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
        // ... existing DEVICE_STATUS_UPDATE logic ...
         if (update.payload.hasOwnProperty('online')) {
            deviceToUpdate.online = update.payload.online;
        }
        if (update.payload.hasOwnProperty('lastSeen')) {
            deviceToUpdate.lastSeen = update.payload.lastSeen;
        }
    } else if (update.type === 'BACKUP_STATUS_UPDATE') {
        if (update.payload.hasOwnProperty('status')) {
            const newStatus = update.payload.status;
            deviceToUpdate.lastBackupStatus = newStatus;

            // Update backupInProgress based on the new status
            // Consider BACKING_UP, UPLOADING as in-progress states
            if (['BACKING_UP', 'UPLOADING'].includes(newStatus)) {
                 backupInProgress[deviceToUpdate.id] = true;
            // Consider INIT, COMPLETED, FAILED (both types), CANCELED as final/inactive states
            } else if (['INIT', 'COMPLETED', 'BACKUP_FAILED', 'UPLOAD_FAILED', 'CANCELED'].includes(newStatus)) {
                 backupInProgress[deviceToUpdate.id] = false;
            }
            // Defaulting to false if status is not explicitly an in-progress one.
            else {
                 backupInProgress[deviceToUpdate.id] = false;
                 console.warn(`Received unknown backup status: ${newStatus}`);
            }
        }
         if (update.payload.hasOwnProperty('timestamp')) {
             deviceToUpdate.lastBackupTimestamp = update.payload.timestamp;
         }
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
    // Initialize backupInProgress state for fetched devices
    devices.value.forEach(device => {
        if (backupInProgress[device.id] === undefined) {
             backupInProgress[device.id] = false; // Default to not in progress
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
  if (backupInProgress[deviceId]) return;

  backupInProgress[deviceId] = true; // Set immediately for UI feedback
  try {
    await requestDeviceBackup(deviceId);
    console.log(`Backup initiated for device ${deviceId}`);
    // No alert needed, rely on WebSocket for status updates
    // Optionally show a temporary "Request sent" notification
  } catch (err: any) {
    console.error(`Failed to initiate backup for device ${deviceId}:`, err);
    alert(`Error starting backup: ${err.response?.data?.message || err.message}`);
    backupInProgress[deviceId] = false; // Reset on error
  }
  // No finally block needed to reset state, rely on WebSocket message
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

.action-button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
  opacity: 0.6;
}
</style>
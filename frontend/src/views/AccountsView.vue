<template>
  <div class="accounts-view">
    <h1>Backed Up Accounts</h1>

    <div v-if="isLoading" class="loading-message">Loading accounts...</div>
    <div v-if="error" class="error-message">Error loading accounts: {{ error }}</div>

    <div v-if="!isLoading && !error && accounts.length === 0" class="no-accounts">
      No backed up accounts found. Start a backup from the Devices page.
    </div>

    <table v-if="!isLoading && accounts.length > 0" class="accounts-table">
      <thead>
        <tr>
          <th>Phone Number</th>
          <th>Backed Up From Device</th>
          <th>Backup Date</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="account in accounts" :key="account.id">
          <td>{{ account.zaloPhoneNumber || 'N/A' }}</td>
          <td>{{ getDeviceName(account.deviceId) }}</td>
          <td>{{ formatTimestamp(account.backupTimestamp) }}</td>
          <td>
            <button class="action-button restore-button" @click="showRestoreInfo(account)" title="Restore function not implemented yet">Restore Info</button>
            <!-- Add 'Delete Backup' later -->
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { getMyBackedUpAccounts, getMyDevices } from '@/api/deviceApi'; // Also need devices for names
import type { BackedUpAccount, Device } from '@/types';

const accounts = ref<BackedUpAccount[]>([]);
const devices = ref<Device[]>([]); // Store devices to map IDs to names
const isLoading = ref(true);
const error = ref<string | null>(null);

const fetchAccountsAndDevices = async () => {
  isLoading.value = true;
  error.value = null;
  try {
    // Fetch both in parallel
    const [fetchedAccounts, fetchedDevices] = await Promise.all([
      getMyBackedUpAccounts(),
      getMyDevices() // Fetch devices to get their names
    ]);
    accounts.value = fetchedAccounts;
    devices.value = fetchedDevices;
  } catch (err: any) {
    console.error('Failed to fetch accounts or devices:', err);
    error.value = err.response?.data?.message || err.message || 'An unknown error occurred';
  } finally {
    isLoading.value = false;
  }
};

// Helper to get device name from ID
const deviceNameMap = computed(() => {
  return devices.value.reduce((map, device) => {
    map[device.id] = device.deviceName;
    return map;
  }, {} as Record<string, string>);
});

const getDeviceName = (deviceId: string): string => {
  return deviceNameMap.value[deviceId] || `Unknown (${deviceId.substring(0, 6)}...)`; // Return name or truncated ID
};

const formatTimestamp = (timestamp?: string): string => {
  if (!timestamp) return 'N/A';
  try {
    return new Date(timestamp).toLocaleString();
  } catch (e) {
    return 'Invalid Date';
  }
};

const showRestoreInfo = (account: BackedUpAccount) => {
    alert(`Restore function for ${account.zaloAccountName} (ID: ${account.zaloAccountId}) is not yet implemented.\n\nDetails:\nDevice: ${getDeviceName(account.deviceId)}\nBackup Time: ${formatTimestamp(account.backupTimestamp)}`);
};

// Fetch data when the component mounts
onMounted(fetchAccountsAndDevices);
</script>

<style scoped>
/* Reuse styles from DevicesView or create shared styles */
.accounts-view {
  padding: 1.5rem 2rem;
}

h1 {
  margin-bottom: 1.5rem;
  color: #2c3e50;
  font-weight: 600;
}

.loading-message, .error-message, .no-accounts {
  margin-top: 1rem;
  padding: 1rem;
  border-radius: 4px;
}
.loading-message { background-color: #e0e0e0; }
.error-message { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
.no-accounts { background-color: #e2e3e5; color: #383d41; }

.accounts-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 1rem;
  background-color: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  overflow: hidden;
}

th, td {
  padding: 0.8rem 1rem;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
  font-size: 0.9rem;
  vertical-align: middle;
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

.action-button {
  padding: 0.4rem 0.8rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85rem;
  transition: background-color 0.2s ease;
  white-space: nowrap;
}

.restore-button {
  background-color: #ffc107; /* Yellow/Orange for restore */
  color: #212529;
}

.restore-button:hover:not(:disabled) {
  background-color: #e0a800;
}

.action-button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
  opacity: 0.6;
}
</style>
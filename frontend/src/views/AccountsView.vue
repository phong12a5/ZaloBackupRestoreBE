<template>
  <div class="accounts-view">
    <h2>Backed Up Accounts</h2>

    <!-- Search and Actions Bar -->
    <div class="actions-bar">
      <textarea 
        v-model="searchQuery" 
        placeholder="Enter phone numbers (one per line, format: phonenumber OR phonenumber|password)" 
        class="search-textarea">
      </textarea>
      <button class="action-button main-action-button" @click="handleTransferAccount" :disabled="selectedAccountIds.length === 0">
        Transfer ({{ selectedAccountIds.length }})
      </button>
    </div>

    <div v-if="isLoading" class="loading-message">Loading accounts...</div>
    <div v-if="error" class="error-message">Error loading accounts: {{ error }}</div>

    <div v-if="!isLoading && !error && accounts.length === 0" class="no-accounts">
      No backed up accounts found. Start a backup from the Devices page.
    </div>

    <table v-if="!isLoading && filteredAccounts.length > 0" class="accounts-table">
      <thead>
        <tr>
          <th><input type="checkbox" @change="toggleSelectAll" :checked="allSelected" /></th>
          <th>Phone Number</th>
          <th>Account Name</th>
          <th>Backed Up From Device</th>
          <th>Backup Date</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="account in filteredAccounts" :key="account.id">
          <td><input type="checkbox" :value="account.id" v-model="selectedAccountIds"></td>
          <td>{{ account.zaloPhoneNumber || 'N/A' }}</td>
          <td>{{ account.zaloAccountName || 'N/A' }}</td>
          <td>{{ getDeviceName(account.deviceId) }}</td>
          <td>{{ formatTimestamp(account.backupTimestamp) }}</td>
          <td>
            <button class="action-button restore-button" @click="showRestoreInfo(account)" title="Restore Info">Restore Info</button>
            <!-- Add 'Delete Backup' later -->
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { getMyBackedUpAccounts, getMyDevices } from '@/api/deviceApi';
import type { BackedUpAccount, Device } from '@/types';

const accounts = ref<BackedUpAccount[]>([]);
const devices = ref<Device[]>([]);
const isLoading = ref(true);
const error = ref<string | null>(null);
const searchQuery = ref('');
const selectedAccountIds = ref<string[]>([]);

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

// Filtered accounts based on search query
const filteredAccounts = computed(() => {
  const query = searchQuery.value.trim();
  if (!query) {
    return accounts.value; // Return all accounts if search query is empty
  }

  const searchLines = query.split('\n')
    .map(line => line.trim())
    .filter(line => line.length > 0);

  if (searchLines.length === 0) {
    return []; // No valid search lines, so no results
  }

  const phoneNumbersToSearch = searchLines.map(line => {
    const parts = line.split('|');
    return parts[0].trim().toLowerCase(); // Get phone number part, normalize
  }).filter(phone => phone.length > 0); // Ensure we have actual phone numbers

  if (phoneNumbersToSearch.length === 0) {
    return []; // No valid phone numbers extracted, so no results
  }

  return accounts.value.filter(account => {
    const accountPhoneNumber = account.zaloPhoneNumber?.trim().toLowerCase();
    if (!accountPhoneNumber) {
      return false;
    }
    // Check if the account's phone number is in our list of numbers to search
    return phoneNumbersToSearch.includes(accountPhoneNumber);
  });
});

// Computed property for "Select All" checkbox state
const allSelected = computed(() => {
  return filteredAccounts.value.length > 0 && selectedAccountIds.value.length === filteredAccounts.value.length;
});

// Toggle select all accounts
const toggleSelectAll = (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (target.checked) {
    selectedAccountIds.value = filteredAccounts.value.map(acc => acc.id);
  } else {
    selectedAccountIds.value = [];
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
  return deviceNameMap.value[deviceId] || `Unknown (${deviceId.substring(0, 6)}...)`;
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
    alert(`Restore information for ${account.zaloPhoneNumber} (ID: ${account.zaloAccountId})\n\nName: ${account.zaloAccountName}\nDevice: ${getDeviceName(account.deviceId)}\nBackup Time: ${formatTimestamp(account.backupTimestamp)}`);
};

// Placeholder for action on selected accounts
const handleTransferAccount = () => {
  if (selectedAccountIds.value.length === 0) {
    alert('Please select at least one account.');
    return;
  }
  alert(`Action triggered for selected accounts: ${selectedAccountIds.value.join(', ')}`);
  // Implement actual logic here, e.g., call an API
};

// Fetch data when the component mounts
onMounted(fetchAccountsAndDevices);
</script>

<style scoped>
/* Reuse styles from DevicesView or create shared styles */
.accounts-view {
  padding: 1.5rem 2rem;
}

.actions-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.search-input {
  padding: 0.5rem 0.8rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 0.9rem;
  flex-grow: 1;
  margin-right: 1rem;
}

.search-textarea { /* Added style for textarea */
  padding: 0.5rem 0.8rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 0.9rem;
  flex-grow: 1;
  margin-right: 1rem;
  min-height: 20px; /* Adjust as needed */
  resize: vertical; /* Allow vertical resize */
  font-family: inherit; /* Ensure consistent font */
}

.main-action-button {
  background-color: #007bff; /* Blue for primary action */
  color: white;
}

.main-action-button:hover:not(:disabled) {
  background-color: #0056b3;
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

th:first-child, td:first-child { /* Style for checkbox column */
  width: 30px; /* Adjust as needed */
  text-align: center;
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
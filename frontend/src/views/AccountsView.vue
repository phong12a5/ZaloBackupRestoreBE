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
      <button 
        class="action-button transfer-button"
        @click="openTransferModal"
        :disabled="selectedAccountIds.length === 0">
        Transfer Selected ({{ selectedAccountIds.length }})
      </button>
    </div>

    <div v-if="isLoading && !showTransferModal" class="loading-message">Loading accounts...</div> <!-- Hide main loading when modal is active for clarity -->
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
          <th>Actions</th> <!-- Added Actions header -->
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
            <button @click="showRestoreInfo(account)" class="action-button restore-button">Restore Info</button>
            <button @click="deleteAccount(account.id)" class="action-button delete-button">Delete</button> <!-- Added Delete button -->
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Transfer Modal -->
    <div v-if="showTransferModal" class="modal-overlay">
      <div class="modal-content">
        <h3>Transfer Selected Accounts</h3>
        <p>You are about to transfer {{ selectedAccountIds.length }} account(s).</p>
        <div class="form-group">
          <label for="targetUserId">Target User ID:</label>
          <input type="text" id="targetUserId" v-model="targetUserIdInput" placeholder="Enter target User ID" class="modal-input">
          <ul v-if="showSuggestions" class="suggestions-list">
            <li v-for="user in userSuggestions" :key="user.id" @click="selectUserSuggestion(user)">
              <strong>{{ user.username }}</strong> - {{ user.fullname || 'No Name' }}
            </li>
          </ul>
        </div>
        <div class="modal-actions">
          <button @click="confirmTransfer" class="modal-button confirm-button" :disabled="!targetUserIdInput.trim() || isTransferring">
            <span v-if="isTransferring">Transferring...</span>
            <span v-else>Confirm Transfer</span>
          </button>
          <button @click="closeTransferModal" class="modal-button cancel-button" :disabled="isTransferring">Cancel</button>
        </div>
        <div v-if="transferError" class="error-message modal-error">{{ transferError }}</div>
        <div v-if="transferSuccessMessage" class="success-message modal-success">{{ transferSuccessMessage }}</div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'; // Added watch
import apiClient from '@/api/axios'; // Import apiClient
import { getMyBackedUpAccounts, getMyDevices, deleteBackedUpAccount } from '@/api/deviceApi';
import { getCurrentUser, getAllUsers } from '@/api/userApi'; // Added userApi imports
import type { BackedUpAccount, Device, UserSafeDto } from '@/types'; // Added UserSafeDto

const searchQuery = ref('');
const selectedAccountIds = ref<string[]>([]);

// New reactive variables for transfer modal
const showTransferModal = ref(false);
const targetUserIdInput = ref('');
const transferError = ref<string | null>(null);
const transferSuccessMessage = ref<string | null>(null);
const isTransferring = ref(false); // To manage loading state during transfer

const accounts = ref<BackedUpAccount[]>([]);
const devices = ref<Device[]>([]);
const error = ref<string | null>(null);
const isLoading = ref(true);

// User suggestion related reactive variables
const currentUser = ref<UserSafeDto | null>(null);
const allUsers = ref<UserSafeDto[]>([]);
const userSuggestions = ref<UserSafeDto[]>([]);
const showSuggestions = ref(false);

const fetchInitialData = async () => {
  try {
    isLoading.value = true;
    error.value = null;
    // Ensure accounts are refetched after a delete operation might have occurred elsewhere or to refresh state
    selectedAccountIds.value = []; // Clear selection on refresh

    const [fetchedAccounts, fetchedDevices, fetchedCurrentUser] = await Promise.all([
      getMyBackedUpAccounts(),
      getMyDevices(),
      getCurrentUser() // Fetch current user
    ]);
    accounts.value = fetchedAccounts;
    devices.value = fetchedDevices;
    currentUser.value = fetchedCurrentUser;

    // console.log('fetchedCurrentUser:', fetchedCurrentUser);
    // If current user is ADMIN, fetch all users for suggestions
    if (fetchedCurrentUser && fetchedCurrentUser.role === 'ADMIN') {
      const fetchedAllUsers = await getAllUsers();
      allUsers.value = fetchedAllUsers;
    }

  } catch (err: any) {
    console.error('Failed to fetch initial data:', err);
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
    alert(`Restore information for ${account.zaloPhoneNumber} (ID: ${account.zaloAccountId})

Name: ${account.zaloAccountName}
Device: ${getDeviceName(account.deviceId)}
Backup Time: ${formatTimestamp(account.backupTimestamp)}`);
};

// Placeholder for delete account logic
const deleteAccount = async (accountId: string) => {
  if (!confirm(`Are you sure you want to delete account backup ${accountId}? This action cannot be undone.`)) {
    return;
  }
  console.log('Attempting to delete account:', accountId);
  // TODO: Implement API call to delete the account
   try {
    await deleteBackedUpAccount(accountId); // Assuming this function will be created in deviceApi.ts
    accounts.value = accounts.value.filter(acc => acc.id !== accountId); // Optimistically update UI
    selectedAccountIds.value = selectedAccountIds.value.filter(id => id !== accountId); // Remove from selection
    alert('Account backup deleted successfully.');
  } catch (err: any) {
    console.error('Failed to delete account:', err);
    error.value = err.response?.data?.message || err.message || 'Failed to delete account backup.';
    alert(`Error: ${error.value}`);
  }
};

// --- Transfer Modal Logic ---
const openTransferModal = () => {
  if (selectedAccountIds.value.length === 0) {
    alert('Please select at least one account to transfer.');
    return;
  }
  transferError.value = null;
  transferSuccessMessage.value = null;
  targetUserIdInput.value = '';
  userSuggestions.value = []; // Clear suggestions
  showSuggestions.value = false; // Hide suggestions box
  isTransferring.value = false;
  showTransferModal.value = true;
};

const closeTransferModal = () => {
  if (isTransferring.value) return;
  showTransferModal.value = false;
  targetUserIdInput.value = '';
  transferError.value = null;
  transferSuccessMessage.value = null;
  userSuggestions.value = [];
  showSuggestions.value = false;
};

const confirmTransfer = async () => {
  if (!targetUserIdInput.value.trim()) {
    transferError.value = 'Target User ID cannot be empty.';
    return;
  }
  if (selectedAccountIds.value.length === 0) {
    transferError.value = 'No accounts selected for transfer.';
    return;
  }

  transferError.value = null;
  transferSuccessMessage.value = null;
  isTransferring.value = true;

  try {
    const payload = {
      backedUpAccountIds: selectedAccountIds.value,
      targetUserId: targetUserIdInput.value.trim(),
    };
    const response = await apiClient.post('/api/devices/backups/transfer', payload);

    // Assuming the API returns a 200 OK with a body that might contain details of successes/failures.
    // For now, we'll show a generic success message.
    // You might want to inspect response.data if it provides more granular feedback.
    transferSuccessMessage.value = `Successfully initiated transfer of ${selectedAccountIds.value.length} account(s) to ${targetUserIdInput.value.trim()}. Check server logs for details on individual accounts.`;
    
    // Clear selection and refresh data after a short delay to show message
    setTimeout(() => {
      selectedAccountIds.value = [];
      // Only close and refresh if still in modal and no new error occurred during the timeout
      if (showTransferModal.value && !transferError.value) {
        closeTransferModal();
        fetchInitialData(); // Changed to fetchInitialData
      }
    }, 3000); // Keep modal open for 3 seconds to show success

  } catch (err: any) {
    console.error('Error transferring accounts:', err);
    if (err.response && err.response.data) {
        let errorMessage = 'Failed to transfer accounts.';
        if (typeof err.response.data === 'string') {
            errorMessage = err.response.data;
        } else if (err.response.data.message) {
            errorMessage = err.response.data.message;
        } else if (err.response.data.error) { // Common Spring Boot error structure
            errorMessage = err.response.data.error + (err.response.data.message ? `: ${err.response.data.message}` : '');
        } else if (Array.isArray(err.response.data) && err.response.data.length > 0 && err.response.data[0].message) {
            errorMessage = err.response.data[0].message; 
        }
        transferError.value = errorMessage;
    } else if (err.message) {
        transferError.value = err.message;
    } else {
        transferError.value = 'An unexpected error occurred during transfer.';
    }
  } finally {
    isTransferring.value = false;
  }
};

// Watch for changes in targetUserIdInput to filter suggestions
watch(targetUserIdInput, (newQuery) => {
  if (currentUser.value?.role === 'ADMIN' && newQuery.trim().length > 0) {
    const queryLower = newQuery.toLowerCase();
    userSuggestions.value = allUsers.value.filter(user =>
      user.id.toLowerCase().includes(queryLower) ||
      user.username.toLowerCase().includes(queryLower) ||
      (user.fullname && user.fullname.toLowerCase().includes(queryLower))
    );
    showSuggestions.value = userSuggestions.value.length > 0;
  } else {
    userSuggestions.value = [];
    showSuggestions.value = false;
  }
});

const selectUserSuggestion = (user: UserSafeDto) => {
  targetUserIdInput.value = user.username;
  userSuggestions.value = [];
  showSuggestions.value = false;
};

// Fetch data when the component mounts
onMounted(fetchInitialData); // Changed to fetchInitialData
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
  min-height: 20px; /* Adjust as needed -> Reduced from a potentially larger or default value */
  height: 40px; /* Explicitly set height, adjust as needed */
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

.transfer-button {
  background-color: #ffc107; /* Yellow, distinct from other actions */
  color: #212529;
  margin-left: 0.5rem; /* Add some space if there are multiple main buttons */
}

.transfer-button:hover:not(:disabled) {
  background-color: #e0a800;
}

.delete-button {
  background-color: #dc3545; /* Red for delete */
  color: white;
  margin-left: 5px; /* Add some space next to the restore button */
}

.delete-button:hover:not(:disabled) {
  background-color: #c82333; /* Darker red on hover */
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000; /* Ensure it's on top */
}

.modal-content {
  background-color: #fff;
  padding: 25px;
  border-radius: 8px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
  width: 90%;
  max-width: 500px;
  z-index: 1001;
}

.modal-content h3 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #333;
}

.modal-content p {
  margin-bottom: 20px;
  color: #555;
}

.form-group {
  margin-bottom: 20px;
  position: relative; /* Added for suggestion box positioning */
}

.modal-input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 1rem;
  box-sizing: border-box;
}

.suggestions-list {
  position: absolute;
  background-color: white;
  border: 1px solid #ccc;
  border-top: none;
  max-height: 150px;
  overflow-y: auto;
  width: calc(100% - 2px); /* Adjust width to match input, considering border */
  z-index: 1002; /* Ensure suggestions are above other modal content but below modal overlay */
  list-style-type: none;
  padding: 0;
  margin: 0;
  border-radius: 0 0 4px 4px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.suggestions-list li {
  padding: 8px 12px;
  cursor: pointer;
}

.suggestions-list li:hover {
  background-color: #f0f0f0;
}

.suggestions-list li strong {
  font-weight: bold;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.modal-button {
  padding: 10px 20px;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 0.95rem;
  transition: background-color 0.2s ease;
}

.confirm-button {
  background-color: #28a745; /* Green for confirm */
  color: white;
}

.confirm-button:hover:not(:disabled) {
  background-color: #218838;
}

.confirm-button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.cancel-button {
  background-color: #6c757d; /* Gray for cancel */
  color: white;
}

.cancel-button:hover:not(:disabled) {
  background-color: #5a6268;
}

.modal-error, .modal-success {
  margin-top: 15px;
  padding: 10px;
  border-radius: 4px;
  text-align: center;
}

.modal-error {
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.modal-success {
  background-color: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

/* ... other styles ... */
</style>
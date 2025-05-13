<template>
  <nav class="top-navbar" @click="closeDropdown">
    <div class="navbar-brand">
      <span class="brand-text">ZaloBR</span>
    </div>
    <div class="navbar-menu">
      <div class="account-dropdown-wrapper" @click.stop>
        <div class="account-avatar" @click="toggleDropdown">
          <img src="/assets/avatar-placeholder.svg" alt="Avatar" class="avatar-image" />
        </div>
        <div v-if="dropdownVisible" class="account-dropdown">
          <div class="dropdown-header">
            <strong>{{ username }}</strong>
            <small>{{ email }}</small>
          </div>
          <hr />
          <div class="dropdown-item">Account Settings</div>
          <div class="dropdown-item">Profile</div>
          <hr />
          <div class="dropdown-item logout" @click="logout">Logout</div>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from '../api/axios';

const router = useRouter();
const dropdownVisible = ref(false);
const username = ref('');
const email = ref('');

const toggleDropdown = () => {
  dropdownVisible.value = !dropdownVisible.value;
};

const closeDropdown = () => {
  dropdownVisible.value = false;
};

const logout = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  router.push('/login');
};

const fetchUserInfo = async () => {
  try {
    const response = await axios.get('/users/me', {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('accessToken')}`,
      },
    });
    username.value = response.data.username;
    email.value = response.data.email;
  } catch (error: any) {
    console.error('Failed to fetch user info:', error);
    if (error.response && error.response.status === 401) {
      router.push('/login');
    }
  }
};

onMounted(() => {
  fetchUserInfo();
});
</script>

<style scoped>
.top-navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 1rem;
  background-color: #2a5b8d;
  color: #fff;
  height: 55px;
  border-bottom: 1px solid #34495e;
}

.navbar-brand {
  font-weight: bold;
  font-size: 1.1rem;
  color: #fff;
}

.navbar-menu {
  display: flex;
  align-items: center;
}

.account-dropdown-wrapper {
  position: relative;
}

.account-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.account-dropdown {
  position: absolute;
  top: 50px;
  right: 0;
  background-color: #fff;
  color: #2c3e50;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  width: 200px;
  z-index: 1000;
  padding: 10px 0;
}

.dropdown-header {
  padding: 10px 15px;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

.dropdown-header strong {
  display: block;
  font-size: 1rem;
  color: #333;
}

.dropdown-header small {
  font-size: 0.85rem;
  color: #666;
}

.dropdown-item {
  padding: 10px 15px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.dropdown-item:hover {
  background-color: #f4f4f4;
}

.logout {
  color: #e74c3c;
  font-weight: bold;
}
</style>

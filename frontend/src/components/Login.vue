<template>
  <div class="login-container">
    <h2>Login</h2>
    <form @submit.prevent="handleLogin">
      <div class="form-group">
        <label for="username">Username:</label>
        <input type="text" id="username" v-model="username" required />
      </div>
      <div class="form-group">
        <label for="password">Password:</label>
        <input type="password" id="password" v-model="password" required />
      </div>
      <button type="submit" :disabled="loading">
        {{ loading ? 'Logging in...' : 'Login' }}
      </button>
      <p v-if="error" class="error-message">{{ error }}</p>
    </form>
    <div class="switch-link">
      Don't have an account? <button @click="goToRegister" class="link-button">Sign Up</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router'; // Import useRouter
import apiClient from '../api/axios'; // Import the configured Axios instance

const username = ref('');
const password = ref('');
const error = ref<string | null>(null);
const loading = ref(false);
const router = useRouter(); // Get router instance

const handleLogin = async () => {
  loading.value = true;
  error.value = null;
  try {
    // Use apiClient instead of fetch
    const response = await apiClient.post('/auth/login', { // Use relative path
      username: username.value,
      password: password.value,
    });

    const data = response.data; // Axios wraps response in 'data'
    console.log('Login successful:', data);

    // Store both tokens
    if (data.accessToken && data.refreshToken) {
      localStorage.setItem('authToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken); // Store refresh token
      router.push('/home');
    } else {
      throw new Error('Login successful, but tokens are missing in response.');
    }

  } catch (err: any) {
    // Axios error handling might differ slightly
    error.value = err.response?.data?.message || err.message || 'An error occurred during login.';
    console.error('Login error:', err);
    localStorage.removeItem('authToken');
    localStorage.removeItem('refreshToken'); // Clear refresh token on failure too
  } finally {
    loading.value = false;
  }
};

const goToRegister = () => {
  router.push('/register'); // Navigate to register page
};
</script>

<style scoped>
.login-container {
  max-width: 400px;
  margin: 50px auto;
  padding: 30px;
  border: 1px solid #ccc;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  background-color: #f9f9f9;
  font-family: sans-serif;
}

h2 {
  text-align: center;
  margin-bottom: 25px;
  color: #333;
}

.form-group {
  margin-bottom: 20px;
}

label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: #555;
}

input[type="text"],
input[type="password"] {
  width: 100%;
  padding: 12px;
  border: 1px solid #ccc;
  border-radius: 4px;
  box-sizing: border-box; /* Makes sure padding doesn't affect width */
}

button {
  width: 100%;
  padding: 12px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s ease;
}

button:hover:not(:disabled) {
  background-color: #0056b3;
}

button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.error-message {
  color: #dc3545;
  margin-top: 15px;
  text-align: center;
  font-size: 0.9em;
}

.switch-link {
  text-align: center;
  margin-top: 20px;
  font-size: 0.9em;
  color: #555;
}

.link-button {
  background: none;
  border: none;
  color: #007bff;
  cursor: pointer;
  padding: 0;
  font-size: inherit;
  text-decoration: underline;
}
.link-button:hover {
  color: #0056b3;
}
</style>

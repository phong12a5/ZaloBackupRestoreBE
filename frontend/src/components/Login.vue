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

const username = ref('');
const password = ref('');
const error = ref<string | null>(null);
const loading = ref(false);
const router = useRouter(); // Get router instance

const handleLogin = async () => {
  loading.value = true;
  error.value = null;
  try {
    // Assume API Gateway is running on localhost:8080
    // and proxies /auth/login to the auth service (Removed /api)
    const response = await fetch('http://localhost:8080/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username: username.value,
        password: password.value,
      }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Login failed. Please check your credentials.' }));
      throw new Error(errorData.message || 'Login failed');
    }

    const data = await response.json();
    // Handle successful login (e.g., store token, redirect)
    console.log('Login successful:', data);
    // Store the token in localStorage
    if (data.token) {
      localStorage.setItem('authToken', data.token);
      // Redirect to the home page
      router.push('/home');
    } else {
      // Handle case where token is missing in response
      throw new Error('Login successful, but no token received.');
    }

  } catch (err: any) {
    error.value = err.message || 'An error occurred during login.';
    console.error('Login error:', err);
    // Clear any potentially stale token on login failure
    localStorage.removeItem('authToken');
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

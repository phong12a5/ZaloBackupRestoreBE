<template>
      <div class="register-container">
        <h2>Register</h2>
        <form @submit.prevent="handleRegister">
          <div class="form-group">
            <label for="username">Username:</label>
            <input type="text" id="username" v-model="username" required />
          </div>
          <!-- Add other fields like email if needed -->
          <!--
          <div class="form-group">
            <label for="email">Email:</label>
            <input type="email" id="email" v-model="email" required />
          </div>
          -->
          <div class="form-group">
            <label for="password">Password:</label>
            <input type="password" id="password" v-model="password" required />
          </div>
          <button type="submit" :disabled="loading">
            {{ loading ? 'Registering...' : 'Register' }}
          </button>
          <p v-if="error" class="error-message">{{ error }}</p>
        </form>
        <div class="switch-link">
          Already have an account? <button @click="goToLogin" class="link-button">Login</button>
        </div>
      </div>
    </template>

    <script setup lang="ts">
    import { ref } from 'vue';
    import { useRouter } from 'vue-router'; // Import useRouter
    import apiClient from '../api/axios'; // Import the configured Axios instance

    const username = ref('');
    const password = ref('');
    // Add other fields if needed, e.g., email
    // const email = ref('');
    const error = ref<string | null>(null);
    const loading = ref(false);
    const router = useRouter(); // Get router instance

    const handleRegister = async () => {
      loading.value = true;
      error.value = null;
      try {
        // Use apiClient instead of fetch
        const response = await apiClient.post('/auth/register', { // Use relative path
          username: username.value,
          password: password.value,
          // email: email.value, // Include other fields if added
        });

        // Axios automatically throws for non-2xx responses, so no need for !response.ok check
        console.log('Registration successful', response.data);
        alert('Registration successful! Please login.');
        router.push('/login');

      } catch (err: any) {
        // Extract the specific error message from the backend response
        const apiErrorMessage = err.response?.data?.error || err.response?.data?.message || err.message || 'An error occurred during registration.';

        // Set the inline error message
        error.value = apiErrorMessage;
        console.error('Registration error:', err);

        // Show the specific API error message in the alert popup
        alert(`Registration failed: ${apiErrorMessage}`);

      } finally {
        loading.value = false;
      }
    };

    const goToLogin = () => {
      router.push('/login'); // Navigate to login page
    };
    </script>

    <style scoped>
    /* Add styles similar to Login.vue, adjusted for Register */
    .register-container {
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
    input[type="email"], /* Add if email is used */
    input[type="password"] {
      width: 100%;
      padding: 12px;
      border: 1px solid #ccc;
      border-radius: 4px;
      box-sizing: border-box;
    }

    button[type="submit"] {
      width: 100%;
      padding: 12px;
      background-color: #28a745; /* Green for register */
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 16px;
      transition: background-color 0.3s ease;
    }

    button[type="submit"]:hover:not(:disabled) {
      background-color: #218838;
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

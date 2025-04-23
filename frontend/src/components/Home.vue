<template>
  <div class="home-container">
    <h1>Welcome!</h1>
    <p>You have successfully logged in.</p>
    <!-- Add more content for your home page here -->
    <button @click="logout">Logout</button> <!-- Optional logout button -->
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

const logout = () => {
  // Clear authentication and refresh tokens
  localStorage.removeItem('authToken');
  localStorage.removeItem('refreshToken');
  // Redirect to login page
  router.push('/login');
};
</script>

<style scoped>
.home-container {
  max-width: 600px;
  margin: 50px auto;
  padding: 30px;
  text-align: center;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background-color: #fff;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);
}

h1 {
  color: #333;
  margin-bottom: 15px;
}

p {
  color: #555;
  margin-bottom: 25px;
}

button {
  padding: 10px 20px;
  background-color: #dc3545; /* Red for logout */
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s ease;
}

button:hover {
  background-color: #c82333;
}
</style>

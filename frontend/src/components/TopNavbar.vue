<template>
  <nav class="top-navbar">
    <div class="navbar-brand">
      <!-- Placeholder for a burger menu icon on small screens -->
      <button class="menu-toggle" aria-label="Toggle menu">☰</button>
      <!-- Vultr uses an SVG logo, replace with yours -->
      <span class="brand-text">MyApp</span>
    </div>
    <div class="navbar-menu">
      <!-- Add Vultr-like icons/links here (e.g., using an icon library) -->
      <a href="#" class="navbar-item icon-link" aria-label="Notifications"><i class="icon-bell"></i></a>
      <a href="#" class="navbar-item icon-link" aria-label="Help"><i class="icon-question"></i></a>
      <div class="navbar-item account-info">
        <span class="account-name">user@example.com</span>
        <button class="account-dropdown" aria-label="Account options">▼</button>
        <button @click="logout" class="logout-button">Logout</button>
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

const logout = () => {
  // Clear authentication tokens (implement this based on your auth logic)
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  console.log('Logged out');
  router.push('/login');
};
</script>

<style scoped>
.top-navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 1rem; /* Adjust padding */
  background-color: #1e2a3a; /* Vultr dark blue */
  color: #fff;
  height: 55px; /* Vultr's approx height */
  border-bottom: 1px solid #34495e; /* Slightly lighter border */
}

.navbar-brand {
  display: flex;
  align-items: center;
}

.menu-toggle {
  display: none; /* Hidden by default, shown on small screens */
  background: none;
  border: none;
  color: #fff;
  font-size: 1.5rem;
  margin-right: 0.5rem;
  padding: 0.2rem 0.5rem;
}

.brand-text {
  font-weight: bold;
  font-size: 1.1rem;
  color: #fff;
}

.navbar-menu {
  display: flex;
  align-items: center;
}

.navbar-item {
  margin-left: 1rem;
  display: flex;
  align-items: center;
}

.icon-link {
  color: #bdc3c7; /* Light gray for icons */
  font-size: 1.2rem;
}
.icon-link:hover {
  color: #fff;
}

/* Placeholder for icons - replace with actual icons */
.icon-bell::before { content: '🔔'; }
.icon-question::before { content: '?'; }

.account-info {
  color: #ecf0f1; /* Lighter text for account */
}

.account-name {
  margin-right: 0.5rem;
  font-size: 0.9rem;
}

.account-dropdown {
  background: none;
  border: none;
  color: #bdc3c7;
  margin-right: 0.8rem;
}

.logout-button {
  background-color: #e74c3c; /* Red logout button */
  color: white;
  border: none;
  padding: 0.4rem 0.8rem;
  border-radius: 4px;
  font-size: 0.85rem;
}
.logout-button:hover {
  background-color: #c0392b;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .menu-toggle {
    display: block; /* Show burger menu */
  }
  .brand-text {
    /* Optionally hide brand text on very small screens */
    /* display: none; */
  }
  .account-name {
    display: none; /* Hide email on small screens */
  }
  .navbar-item:not(.account-info) {
     /* Hide other items like icons if needed */
     /* display: none; */
  }
}
</style>

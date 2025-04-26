<template>
  <aside :class="['left-sidebar', { 'is-hidden': !isSidebarVisible }]">
    <div class="sidebar-header">
      <!-- Optional header/logo -->
    </div>
    <ul>
      <!-- Add icons using an icon library -->
      <li><a href="#"><i class="icon-server"></i> Products</a></li>
      <li><a href="#"><i class="icon-billing"></i> Billing</a></li>
      <li><a href="#"><i class="icon-support"></i> Support</a></li>
      <li><a href="#"><i class="icon-account"></i> Account</a></li>
      <li><a href="#"><i class="icon-api"></i> API</a></li>
      <!-- Add more Vultr-like menu items -->
    </ul>
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue'; // Assuming you might control visibility via props/events later

// Placeholder state - in a real app, this would likely be managed globally (Vuex/Pinia)
// or passed via props/events triggered by the TopNavbar toggle.
const isSidebarVisible = ref(true);

// Placeholder for icons - replace with actual icons
</script>

<style scoped>
.left-sidebar {
  width: 220px; /* Slightly wider like Vultr */
  background-color: #2c3e50; /* Vultr sidebar dark blue/gray */
  color: #fff;
  padding: 1rem 0; /* More vertical padding, less horizontal */
  height: calc(100vh - 55px); /* Full height minus top navbar (updated height) */
  overflow-y: auto;
  transition: transform 0.3s ease, width 0.3s ease; /* Smooth transition for hiding */
  flex-shrink: 0; /* Prevent sidebar from shrinking */
}

.sidebar-header {
  padding: 0 1rem 1rem 1rem;
  /* Style for logo/header if needed */
}

ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

li a {
  color: #bdc3c7; /* Lighter gray text */
  text-decoration: none;
  display: flex; /* Use flex for icon alignment */
  align-items: center;
  padding: 0.75rem 1.5rem; /* Adjust padding */
  font-size: 0.95rem;
  white-space: nowrap; /* Prevent text wrapping */
}

li a:hover {
  color: #fff;
  background-color: #34495e; /* Slightly lighter background on hover */
}

li a i {
  margin-right: 0.8rem; /* Space between icon and text */
  width: 1.2em; /* Ensure icons align nicely */
  text-align: center;
}

/* Placeholder icons */
.icon-server::before { content: '💻'; }
.icon-billing::before { content: '💳'; }
.icon-support::before { content: '❓'; }
.icon-account::before { content: '👤'; }
.icon-api::before { content: '🔌'; }

/* Responsive: Hide sidebar */
@media (max-width: 768px) {
  .left-sidebar {
    position: absolute; /* Take out of flow */
    left: 0;
    top: 55px; /* Position below navbar */
    z-index: 1000; /* Ensure it's above content */
    transform: translateX(-100%); /* Hide off-screen */
    height: calc(100vh - 55px);
  }

  .left-sidebar.is-hidden {
     transform: translateX(-100%);
  }

  /* Add a class (e.g., .sidebar-visible) controlled by JS to show it */
  /* .left-sidebar.sidebar-visible { */
  /*   transform: translateX(0); */
  /* } */
}
</style>

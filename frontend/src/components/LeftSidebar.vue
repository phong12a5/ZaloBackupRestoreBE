<template>
  <aside :class="['left-sidebar', { 'is-hidden': !isSidebarVisible }]">
    <div class="sidebar-header">
      <!-- Optional header/logo -->
    </div>
    <ul>
      <!-- Use router-link for navigation -->
      <li><router-link to="/devices"><i class="icon-server"></i> Devices</router-link></li>
      <li><router-link to="/accounts"><i class="icon-billing"></i> Accounts</router-link></li>
      <li><router-link to="/download"><i class="icon-download"></i> Download</router-link></li>
      <!-- Removed API Docs link -->
    </ul>
    <div class="sidebar-footer">
      <p>&copy; {{ new Date().getFullYear() }} ZaloBackupRestore. All rights reserved.</p>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { RouterLink } from 'vue-router'; // Import RouterLink

// Placeholder state - in a real app, this would likely be managed globally (Vuex/Pinia)
// or passed via props/events triggered by the TopNavbar toggle.
const isSidebarVisible = ref(true);

// Placeholder for icons - replace with actual icons
</script>

<style scoped>
.left-sidebar {
  width: 180px; /* Slightly wider like Vultr */
  background-color: #2a5b8d; /* Vultr sidebar dark blue/gray */
  color: #fff;
  /* padding: 1rem 0; */ /* Adjusted to allow footer to stick to bottom */
  height: calc(100vh - 55px); /* Full height minus top navbar (updated height) */
  overflow-y: auto;
  transition: transform 0.3s ease, width 0.3s ease; /* Smooth transition for hiding */
  flex-shrink: 0; /* Prevent sidebar from shrinking */
  display: flex; /* Added for footer positioning */
  flex-direction: column; /* Added for footer positioning */
  padding-top: 1rem; /* Add padding back to top only */
}

.sidebar-header {
  padding: 0 1rem 1rem 1rem;
  /* Style for logo/header if needed */
}

ul {
  list-style: none;
  padding: 0;
  margin: 0;
  flex-grow: 1; /* Allow ul to take available space, pushing footer down */
}

li a, /* Keep styling for 'a' if any exist elsewhere, or remove if only router-links are used */
li .router-link { /* Apply base styles to router-link */
  color: #bdc3c7; /* Lighter gray text */
  text-decoration: none;
  display: flex; /* Use flex for icon alignment */
  align-items: center;
  padding: 0.75rem 1.5rem; /* Adjust padding */
  font-size: 0.95rem;
  white-space: nowrap; /* Prevent text wrapping */
  transition: background-color 0.2s ease, color 0.2s ease; /* Smooth transition */
}

li a:hover,
li .router-link:hover {
  color: #fff;
  background-color: #34495e; /* Slightly lighter background on hover */
}

/* Style for the active router link */
li .router-link-exact-active {
  color: #fff; /* White text for active link */
  background-color: #1abc9c; /* A distinct background color for active state */
  /* Optional: Add a left border or other indicators */
  /* border-left: 3px solid #f1c40f; */
  /* font-weight: bold; */
}

/* Ensure hover on active link doesn't change background if it's already distinct */
li .router-link-exact-active:hover {
  background-color: #1abc9c; /* Keep the active background color on hover */
}

li a i,
li .router-link i {
  margin-right: 0.8rem; /* Space between icon and text */
  width: 1.2em; /* Ensure icons align nicely */
  text-align: center;
}

/* Placeholder icons */
.icon-server::before { content: '💻'; }
.icon-billing::before { content: '💳'; }
.icon-download::before { content: '📥'; } /* Added download icon */
.icon-support::before { content: '❓'; }
.icon-account::before { content: '👤'; }
.icon-api::before { content: '🔌'; }

.sidebar-footer {
  padding: 1rem 1.5rem;
  text-align: center;
  font-size: 0.75rem;
  color: #bdc3c7; /* Lighter gray text, same as menu items */
  margin-top: auto; /* Pushes footer to the bottom if sidebar content is short */
  border-top: 1px solid #34495e; /* Optional separator line */
}

.sidebar-footer p {
  margin: 0;
}

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

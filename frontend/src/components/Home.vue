<template>
  <div class="home-layout">
    <!-- TopNavbar needs to emit an event to toggle sidebar visibility -->
    <TopNavbar />
    <div class="main-container">
      <!-- LeftSidebar needs to react to the toggle event/state -->
      <LeftSidebar />
      <main class="content-area">
        <!-- Add a container for better content structure/padding -->
        <div class="content-wrapper">
          <h2>Welcome to the Home Page!</h2>
          <p>This is the main content area where your application features will be displayed.</p>
          <!-- Your main application content goes here -->
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import TopNavbar from './TopNavbar.vue';
import LeftSidebar from './LeftSidebar.vue';

// Need state management (e.g., Pinia/Vuex) or provide/inject
// to handle sidebar visibility toggle between TopNavbar and LeftSidebar.
</script>

<style scoped>
.home-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.main-container {
  display: flex;
  flex-grow: 1;
  overflow: hidden; /* Changed from hidden to auto if sidebar might overlay */
  position: relative; /* Needed for absolute positioning of sidebar on mobile */
}

.content-area {
  flex-grow: 1;
  overflow-y: auto;
  background-color: #f4f7fa; /* Match body background or use #fff */
  transition: margin-left 0.3s ease; /* Animate margin when sidebar hides (desktop) */
}

.content-wrapper {
  padding: 1.5rem 2rem; /* Add padding to content */
  max-width: 1200px; /* Optional: Max width for content */
  margin: 0 auto; /* Center content */
}

/* Responsive adjustments for content area when sidebar hides */
@media (max-width: 768px) {
  .content-area {
    margin-left: 0; /* No margin needed when sidebar is absolute/hidden */
  }
}

/* Example: If sidebar is NOT absolutely positioned on desktop but hides */
/* You would need a class on main-container when sidebar is hidden */
/*
.main-container.sidebar-hidden .content-area {
  margin-left: 0;
}
.main-container.sidebar-hidden .left-sidebar {
  width: 0;
  overflow: hidden;
}
*/
</style>

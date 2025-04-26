<template>
  <div id="app-container">
    <router-view /> <!-- Router views (Login, Register, Home) will be rendered here -->
  </div>
</template>

<script setup lang="ts">
// No script logic needed for this basic layout yet
</script>

<style scoped>
#app-container {
  min-height: 100vh;
  width: 100%; /* Ensure it takes full width */
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}
</style>

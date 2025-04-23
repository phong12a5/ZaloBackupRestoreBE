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
  min-height: 100vh; /* Ensure container takes full viewport height */
  display: flex;
  flex-direction: column;
  align-items: center; /* Center content horizontally */
  justify-content: center; /* Center content vertically for login/register */
  padding: 20px;
  box-sizing: border-box;
}
</style>

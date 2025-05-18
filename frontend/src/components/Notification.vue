<template>
  <transition name="slide-fade">
    <div v-if="visible" :class="['notification', `notification-${type}`]">
      {{ message }}
    </div>
  </transition>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue';

interface Props {
  message: string;
  type?: 'success' | 'error' | 'info' | 'warning';
  duration?: number; // in milliseconds
  show: boolean; // Prop to control visibility from parent
}

const props = withDefaults(defineProps<Props>(), {
  type: 'info',
  duration: 3000,
});

const emit = defineEmits(['update:show']);

const visible = ref(props.show);
let timer: number | undefined;

watch(() => props.show, (newValue) => {
  visible.value = newValue;
  if (newValue) {
    if (timer) clearTimeout(timer);
    if (props.duration > 0) {
      timer = window.setTimeout(() => {
        visible.value = false;
        emit('update:show', false); // Notify parent to update its state
      }, props.duration);
    }
  } else {
    if (timer) clearTimeout(timer);
  }
});

onUnmounted(() => {
  if (timer) clearTimeout(timer);
});
</script>

<style scoped>
.notification {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 15px 20px;
  border-radius: 8px;
  color: white;
  font-size: 0.9rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  min-width: 250px;
  max-width: 400px; /* Added max-width */
  text-align: center;
  pointer-events: none; /* Allow clicks to pass through if needed, or manage clicks on notification itself */
}

.notification-success {
  background-color: #4CAF50; /* Green */
}

.notification-error {
  background-color: #f44336; /* Red */
}

.notification-info {
  background-color: #2196F3; /* Blue */
}

.notification-warning {
  background-color: #ff9800; /* Orange */
}

/* Animation: Slide from right */
.slide-fade-enter-active, .slide-fade-leave-active {
  transition: all 0.4s ease-out;
}
.slide-fade-enter-from, .slide-fade-leave-to {
  transform: translateX(120%);
  opacity: 0;
}

/* Alternative: Slide from top
.notification {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%); // For centering
  padding: 15px 20px;
  // ... other styles ...
}
.slide-fade-enter-from, .slide-fade-leave-to {
  transform: translate(-50%, -120%); // Adjust for centering and top slide
  opacity: 0;
}
*/
</style>

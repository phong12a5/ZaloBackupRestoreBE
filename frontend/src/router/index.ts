import { createRouter, createWebHistory } from 'vue-router';
import Login from '../components/Login.vue';
import Register from '../components/Register.vue';
import Home from '../components/Home.vue'; // Import the Home component

const routes = [
  {
    path: '/',
    redirect: '/login' // Redirect root path to login
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
  },
  {
    path: '/register',
    name: 'Register',
    component: Register,
  },
  {
    path: '/home',
    name: 'Home',
    component: Home,
    meta: { requiresAuth: true } // Optional: Add meta field for route guarding
  },
  // Add other routes here
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// Optional: Add navigation guard to check authentication
router.beforeEach((to, _from, next) => { // Changed 'from' to '_from'
  const isAuthenticated = !!localStorage.getItem('authToken'); // Basic check

  if (to.matched.some(record => record.meta.requiresAuth) && !isAuthenticated) {
    // Redirect to login if trying to access a protected route without being authenticated
    next({ name: 'Login' });
  } else {
    next(); // Proceed as normal
  }
});

export default router;

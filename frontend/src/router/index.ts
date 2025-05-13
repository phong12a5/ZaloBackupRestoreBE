import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import Login from '../components/Login.vue';
import Register from '../components/Register.vue';
import Home from '../components/Home.vue'; // Import the Home component
import DevicesView from '../views/DevicesView.vue';
import AccountsView from '../views/AccountsView.vue'; // Import AccountsView
import DownloadView from '../views/DownloadView.vue'; // Import AccountsView

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/login' },
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
    meta: { requiresAuth: true },
    redirect: '/devices',
    children: [
      {
        path: '/devices',
        name: 'Devices',
        component: DevicesView,
        meta: { requiresAuth: true }
      },
      { // Add route for AccountsView
        path: '/accounts',
        name: 'Accounts',
        component: AccountsView, // Use direct import or lazy load
        // component: () => import('../views/AccountsView.vue'), // Lazy load example
        meta: { requiresAuth: true }
      },
      { // Add route for DownloadView
        path: '/download',
        name: 'Download',
        component: DownloadView,
        meta: { requiresAuth: true }
      }
    ]
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

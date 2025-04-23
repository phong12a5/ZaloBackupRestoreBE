import axios from 'axios';
import router from '../router'; // Import router to redirect on refresh failure

const apiClient = axios.create({
  baseURL: 'http://localhost:8080', // Your API Gateway base URL
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add the access token to headers
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle token refresh
let isRefreshing = false;
let failedQueue: { resolve: (value: unknown) => void; reject: (reason?: any) => void }[] = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const originalRequest = error.config;

    // Check if it's a 401 error and not a retry request
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // If already refreshing, queue the original request
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          originalRequest.headers['Authorization'] = 'Bearer ' + token;
          return apiClient(originalRequest); // Retry with new token
        }).catch(err => {
          return Promise.reject(err); // Propagate refresh error
        });
      }

      originalRequest._retry = true; // Mark as retry
      isRefreshing = true;

      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) {
        console.error('No refresh token available. Logging out.');
        localStorage.removeItem('authToken');
        localStorage.removeItem('refreshToken');
        router.push('/login');
        isRefreshing = false;
        processQueue(new Error('No refresh token'), null);
        return Promise.reject(new Error('No refresh token available.'));
      }

      try {
        // Assume /auth/refresh endpoint exists on the backend
        const refreshResponse = await axios.post('http://localhost:8080/auth/refresh', { refreshToken });
        const newAccessToken = refreshResponse.data.accessToken;
        // const newRefreshToken = refreshResponse.data.refreshToken; // If backend sends a new one

        localStorage.setItem('authToken', newAccessToken);
        // if (newRefreshToken) localStorage.setItem('refreshToken', newRefreshToken);

        apiClient.defaults.headers.common['Authorization'] = `Bearer ${newAccessToken}`;
        originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`;

        processQueue(null, newAccessToken); // Process queued requests with new token
        return apiClient(originalRequest); // Retry the original request

      } catch (refreshError: any) {
        console.error('Unable to refresh token:', refreshError);
        localStorage.removeItem('authToken');
        localStorage.removeItem('refreshToken');
        router.push('/login'); // Redirect to login on refresh failure
        processQueue(refreshError, null); // Reject queued requests
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    // For errors other than 401 or if it's already a retry
    return Promise.reject(error);
  }
);

export default apiClient;

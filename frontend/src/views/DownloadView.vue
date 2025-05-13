<template>
  <div class="download-view">
    <h2>Software Downloads</h2>

    <section class="download-section">
      <h3>Android Application (APK)</h3>
      <button @click="downloadApk" class="download-button">Download Android APK</button>
    </section>

    <hr class="section-divider">
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import apiClient from '@/api/axios'; // Import your apiClient

export default defineComponent({
  name: 'DownloadView',
  methods: {
    async downloadApk() {
      try {
        const response = await apiClient.get('/api/devices/apk/zalobr', {
          responseType: 'blob', // Important for file downloads
        });

        // Create a new Blob object from the response data with the correct MIME type
        const blob = new Blob([response.data], { type: response.headers['content-type'] || 'application/vnd.android.package-archive' });

        // Create a link element
        const link = document.createElement('a');

        // Create an object URL for the blob
        const url = window.URL.createObjectURL(blob);
        link.href = url;

        // Try to get filename from Content-Disposition header
        let filename = 'app-release.apk'; // Default filename
        const contentDisposition = response.headers['content-disposition'];
        if (contentDisposition) {
          const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"])(?:\\.|[^\'"])*\2|[^;\n]*)/i);
          if (filenameMatch && filenameMatch[1]) {
            filename = filenameMatch[1].replace(/['"]/g, '');
          }
        }
        link.setAttribute('download', filename);

        // Append to the DOM, click, and remove
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        // Revoke the object URL to free up resources
        window.URL.revokeObjectURL(url);

      } catch (error) {
        console.error('Error downloading APK:', error);
        // Handle error appropriately in the UI, e.g., show a notification
        alert('Failed to download APK. Please try again later.');
      }
    }
  }
});
</script>

<style scoped>
.download-view {
    padding: 1.5rem 2rem;
}

.download-section {
  margin-bottom: 30px;
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  /* Added for horizontal centering */
  margin-left: auto;
  margin-right: auto;
  max-width: 700px; /* Adjust as needed, ensures the section is not full-width if parent is wider */
}

.download-section h2 {
  margin-top: 0;
  color: #333;
}

.download-button {
  background-color: #007bff;
  color: white;
  border: none;
  padding: 10px 20px;
  text-align: center;
  text-decoration: none;
  display: inline-block;
  font-size: 16px;
  border-radius: 5px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.download-button:hover {
  background-color: #0056b3;
}

.section-divider {
  border: 0;
  height: 1px;
  background-color: #eee;
  margin: 40px 0;
}
</style>

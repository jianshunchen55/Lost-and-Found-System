<template>
  <NavBar v-if="!$route.meta.hideNav" />
  <div class="main-content">
    <router-view />
  </div>
  <Footer v-if="!$route.meta.hideNav" />
</template>

<script setup lang="ts">
import NavBar from './components/NavBar.vue'
import Footer from './components/Footer.vue'
</script>

<style>
html, body, #app {
  height: 100%;
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  background-attachment: fixed; /* Ensure gradient covers scroll */
}
.main-content {
  min-height: calc(100vh - 160px); /* Adjust based on header/footer height */
}
</style>
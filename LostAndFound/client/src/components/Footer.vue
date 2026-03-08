<template>
  <footer class="footer">
    <div class="container">
      <div class="links">
        <router-link to="/">首页</router-link>
        <router-link to="/publish/lost">发布失物</router-link>
        <router-link to="/publish/found">发布拾物</router-link>
        <a href="javascript:void(0)" @click="contactVisible = true">联系我们</a>
      </div>
      <div class="copyright">
        © 2025 广理大学失物招领系统 | 基于 Spring Boot + Vue 3 开发
      </div>
    </div>
    
    <el-dialog v-model="contactVisible" title="联系我们" width="500px" center>
      <div style="text-align: center; padding: 20px; font-size: 16px; line-height: 2;">
        <p><strong>地点：</strong>人工智能学院楼110</p>
        <p><strong>电话：</strong>13670881532</p>
        <p style="margin-top: 15px; color: #409EFF; font-weight: bold;">欢迎您的到来给予我们反馈！</p>
      </div>
    </el-dialog>
  </footer>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const contactVisible = ref(false)
</script>

<style scoped>
.footer {
  background: #f0f2f5;
  padding: 30px 0;
  margin-top: 40px;
  border-top: 1px solid #e8e8e8;
}
.container {
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
}
.links {
  margin-bottom: 15px;
}
.links a {
  margin: 0 15px;
  color: #666;
  text-decoration: none;
}
.links a:hover {
  color: #1890ff;
}
.copyright {
  color: #999;
  font-size: 14px;
}
</style>
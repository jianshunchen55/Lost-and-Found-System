<template>
  <div class="sidebar">
    <div class="user-card">
      <div class="avatar-wrapper">
        <img :src="user.avatar ? 'http://localhost:8080'+user.avatar : '/default-avatar.png'" alt="Avatar" class="avatar"/>
      </div>
      <h3 class="username">{{ user.nickname || user.username }}</h3>
      <p class="role-badge">{{ roleText }}</p>
    </div>

    <div class="menu">
      <div class="menu-item" :class="{active: active === 'profile'}" @click="nav('/profile?tab=info')">个人信息</div>
      <div class="menu-item" :class="{active: active === 'mypost'}" @click="nav('/profile?tab=mypost')">我的发布</div>
      <div class="menu-item" :class="{active: active === 'publish-lost'}" @click="nav('/publish/lost')">发布失物</div>
      <div class="menu-item" :class="{active: active === 'publish-found'}" @click="nav('/publish/found')">发布拾物</div>
      <div class="menu-item" :class="{active: active === 'myclaims'}" @click="nav('/profile?tab=claims')">我的认领</div>
      <div class="menu-item" :class="{active: active === 'messages'}" @click="nav('/profile?tab=messages')">消息中心</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'

const props = defineProps<{
  active?: string
}>()

const store = useStore()
const router = useRouter()
const user = computed(() => store.state.auth.user || {})
const roleText = computed(() => {
  const map: any = { 'ROLE_STUDENT': '学生', 'ROLE_ADMIN': '管理员', 'ROLE_MANAGER': '负责人' }
  if (user.value.roles && user.value.roles.length > 0) {
    return user.value.roles.map((r: any) => map[r.code] || r.code).join(', ')
  }
  return map[user.value.role] || user.value.role || '未知角色'
})

function nav(path: string) {
  router.push(path)
}
</script>

<style scoped>
.sidebar {
  width: 250px;
  background: white;
  border-radius: 8px;
  padding: 20px;
  height: fit-content;
  flex-shrink: 0;
}
.user-card {
  text-align: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}
.avatar-wrapper {
  width: 80px;
  height: 80px;
  margin: 0 auto 10px;
  border-radius: 50%;
  overflow: hidden;
}
.avatar { width: 100%; height: 100%; object-fit: cover; }
.username { margin: 10px 0 5px; font-size: 18px; }
.role-badge { color: #666; font-size: 12px; background: #f5f5f5; display: inline-block; padding: 2px 8px; border-radius: 10px; }

.menu-item {
  padding: 12px 15px;
  cursor: pointer;
  border-radius: 4px;
  margin-bottom: 5px;
  transition: all 0.3s;
}
.menu-item:hover { background: #f5f5f5; }
.menu-item.active { background: #e6f7ff; color: #1890ff; font-weight: 500; }
</style>

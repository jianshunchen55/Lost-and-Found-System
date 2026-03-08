<template>
  <nav class="navbar">
    <div class="container">
      <div class="logo" @click="$router.push('/')">
        <span class="logo-text">广理失物招领</span>
      </div>
      
      <div class="nav-links">
        <router-link to="/publish/lost">发布失物</router-link>
        <router-link to="/publish/found">发布拾物</router-link>
        <router-link to="/query">物品查询</router-link>
        <router-link to="/lost-items">用户失物</router-link>
        <div class="msg-link" @click="$router.push('/messages')">
          消息中心
          <span class="badge" v-if="unreadCount > 0">{{ unreadCount }}</span>
        </div>
      </div>

      <div class="search-bar">
        <input type="text" placeholder="搜索物品..." v-model="keyword" @keyup.enter="doSearch"/>
        <button @click="doSearch">🔍</button>
      </div>

      <div class="user-area">
        <div v-if="!isLoggedIn" class="auth-btns">
          <button @click="$router.push('/login')">登录</button>
          <button @click="$router.push('/register')">注册</button>
        </div>
        <div v-else class="user-profile" @mouseenter="handleMouseEnter" @mouseleave="handleMouseLeave">
          <img :src="avatarUrl" class="nav-avatar" alt="Avatar"/>
          <span class="username">{{ user.nickname || user.username }}</span>
          <div class="dropdown" v-if="showDropdown">
            <div @click="$router.push('/profile')">个人中心</div>
            <div @click="$router.push('/my-claims')">认领进度</div>
            <div v-if="isAdmin" @click="$router.push('/admin/stats')">后台管理</div>
            <div @click="logout" class="logout">退出登录</div>
          </div>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import http from '../api/http'

const store = useStore()
const router = useRouter()
const route = useRoute()

const keyword = ref('')
const showDropdown = ref(false)

const isLoggedIn = computed(() => !!store.state.auth.token)
const user = computed(() => store.state.auth.user || {})
const isAdmin = computed(() => {
  const roles = user.value.roles || []
  if (Array.isArray(roles)) {
    return roles.some((r: any) => ['ROLE_ADMIN', 'ROLE_MANAGER'].includes(r.code))
  }
  return ['ROLE_ADMIN', 'ROLE_MANAGER'].includes(user.value.role)
})
const unreadCount = ref(0)
const displayCount = computed(() => {
  const c = unreadCount.value
  return c > 99 ? '99+' : c
})
let timer: any = null
let dropdownTimer: any = null

const handleMouseEnter = () => {
  if (dropdownTimer) clearTimeout(dropdownTimer)
  showDropdown.value = true
}

const handleMouseLeave = () => {
  dropdownTimer = setTimeout(() => {
    showDropdown.value = false
  }, 200)
}

const fetchUnreadCount = async () => {
  if (!isLoggedIn.value) return
  try {
    const res = await http.get('/message/unread-count')
    unreadCount.value = res.data.count
  } catch (e) {
    console.error('Failed to fetch unread count', e)
  }
}

watch(isLoggedIn, (newVal) => {
  if (newVal) {
    fetchUnreadCount()
  } else {
    unreadCount.value = 0
  }
})

// Refresh count when route changes (e.g. after reading messages)
watch(() => route.path, () => {
  if (isLoggedIn.value) {
    fetchUnreadCount()
  }
})

onMounted(() => {
  if (isLoggedIn.value) {
    fetchUnreadCount()
  }
  timer = setInterval(() => {
    if (isLoggedIn.value) fetchUnreadCount()
  }, 10000) // Poll every 10s
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (dropdownTimer) clearTimeout(dropdownTimer)
})

const avatarUrl = computed(() => {
  if (user.value.avatar) return user.value.avatar
  return 'data:image/svg+xml;charset=UTF-8,%3Csvg%20width%3D%2264%22%20height%3D%2264%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2232%22%20r%3D%2232%22%20fill%3D%22%23cccccc%22%2F%3E%3Ctext%20x%3D%2250%25%22%20y%3D%2250%25%22%20font-family%3D%22Arial%22%20font-size%3D%2224%22%20fill%3D%22%23ffffff%22%20text-anchor%3D%22middle%22%20dy%3D%22.3em%22%3E%F0%9F%91%A4%3C%2Ftext%3E%3C%2Fsvg%3E'
})

const doSearch = () => {
  if (keyword.value.trim()) {
    router.push({ path: '/query', query: { keyword: keyword.value } })
  }
}

const logout = () => {
  store.commit('auth/logout')
  router.push('/login')
}
</script>

<style scoped>
.navbar {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  height: 70px;
  position: sticky;
  top: 0;
  z-index: 1000;
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  transition: all 0.3s ease;
}

.container {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 24px;
}

.logo {
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: transform 0.3s ease;
}

.logo:hover {
  transform: scale(1.02);
}

.logo-text {
  font-size: 24px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 0.5px;
}

.nav-links {
  display: flex;
  gap: 12px;
  align-items: center;
}

.nav-links a, .msg-link {
  color: #505050;
  text-decoration: none;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  position: relative;
  padding: 8px 16px;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.nav-links a:hover, .msg-link:hover {
  color: #764ba2;
  background: rgba(118, 75, 162, 0.08);
}

.nav-links .router-link-active {
  color: #764ba2;
  font-weight: 600;
  background: rgba(118, 75, 162, 0.1);
}

.msg-link {
  display: flex;
  align-items: center;
}

.badge {
  background: linear-gradient(135deg, #ff6b6b, #ff4757);
  color: white;
  border-radius: 10px;
  padding: 0 6px;
  font-size: 11px;
  font-weight: bold;
  position: absolute;
  top: 2px;
  right: 2px;
  box-shadow: 0 2px 5px rgba(255, 71, 87, 0.3);
  border: 2px solid #fff;
}

.search-bar {
  display: flex;
  align-items: center;
  background: rgba(245, 247, 250, 0.8);
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: 24px;
  padding: 6px 16px;
  transition: all 0.3s ease;
  width: 240px;
}

.search-bar:focus-within {
  background: #fff;
  border-color: #764ba2;
  box-shadow: 0 0 0 3px rgba(118, 75, 162, 0.1);
  width: 280px;
}

.search-bar input {
  border: none;
  outline: none;
  background: transparent;
  width: 100%;
  font-size: 14px;
  color: #333;
}

.search-bar button {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  opacity: 0.6;
  transition: opacity 0.3s;
  padding: 0;
  margin-left: 8px;
}

.search-bar button:hover {
  opacity: 1;
}

.user-area {
  min-width: 120px;
  display: flex;
  justify-content: flex-end;
}

.auth-btns button {
  margin-left: 12px;
  padding: 8px 20px;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.auth-btns button:first-child {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(118, 75, 162, 0.3);
}

.auth-btns button:first-child:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(118, 75, 162, 0.4);
}

.auth-btns button:nth-child(2) {
  background: transparent;
  color: #666;
  border: 1px solid rgba(0,0,0,0.1);
}

.auth-btns button:nth-child(2):hover {
  border-color: #764ba2;
  color: #764ba2;
}

.user-profile {
  position: relative;
  cursor: pointer;
  padding: 6px;
  display: flex;
  align-items: center;
  border-radius: 24px;
  transition: background 0.3s;
}

.user-profile:hover {
  background: rgba(0,0,0,0.03);
}

.nav-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  margin-right: 10px;
}

.username {
  font-weight: 500;
  color: #333;
  font-size: 14px;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowraps;
}

.dropdown {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  box-shadow: 0 10px 40px rgba(0,0,0,0.1);
  border: 1px solid rgba(255,255,255,0.5);
  border-radius: 16px;
  width: 140px;
  overflow: hidden;
  padding: 6px;
  animation: slideIn 0.2s ease-out;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.dropdown div {
  padding: 10px 16px;
  color: #555;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 8px;
  text-align: left;
}

.dropdown div:hover {
  background: rgba(118, 75, 162, 0.08);
  color: #764ba2;
  transform: translateX(4px);
}

.logout {
  color: #ff4757 !important;
  border-top: 1px solid rgba(0,0,0,0.05);
  margin-top: 4px;
}

.logout:hover {
  background: rgba(255, 71, 87, 0.1) !important;
  color: #ff4757 !important;
}

@media (max-width: 768px) {
  .nav-links {
    display: none;
  }
  .search-bar {
    display: none;
  }
}
</style>
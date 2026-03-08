<template>
  <div class="home-page">

    <!-- Hero Section -->
    <div class="hero-section">
      <div class="container hero-container">
        <!-- Carousel -->
      <div class="carousel-wrapper">
        <el-carousel trigger="click" height="300px">
          <!-- Slide 1: Static Welcome -->
          <el-carousel-item key="welcome">
            <div class="carousel-content welcome-slide">
               <div class="welcome-text">
                 <h3>欢迎使用失物招领系统</h3>
                 <p>请大家积极发布失物与拾物信息，共同维护校园环境。</p>
               </div>
            </div>
          </el-carousel-item>
          <!-- Slide 2+: Dynamic Image Notices -->
          <el-carousel-item v-for="item in imageNotices" :key="item.id">
            <div class="carousel-content">
              <el-image :src="item.imageUrl" fit="cover" class="carousel-img" />
              <div class="carousel-text">
                <h3>{{ item.title }}</h3>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>
      
      <!-- Core Stats -->
        <div class="hero-stats">
          <div class="stat-box">
            <h4>本周失物</h4>
            <div class="stat-num">{{ stats.lostWeekly || 0 }}</div>
          </div>
          <div class="stat-box">
            <h4>认领成功</h4>
            <div class="stat-num success">{{ stats.claimedWeekly || 0 }}</div>
          </div>
          <button class="guide-btn" @click="$router.push('/guide')">新手教程</button>
        </div>
      </div>
    </div>

    <!-- Quick Access -->
    <div class="container section">
      <div class="quick-access">
        <div class="card" @click="$router.push('/publish/lost')">
          <div class="icon">📢</div>
          <div class="text">失物发布</div>
        </div>
        <div class="card" @click="$router.push('/publish/found')">
          <div class="icon">🎁</div>
          <div class="text">拾物发布</div>
        </div>
        <div class="card" @click="$router.push('/query')">
          <div class="icon">🔍</div>
          <div class="text">高级查询</div>
        </div>
        <div class="card" @click="$router.push('/messages')">
          <div class="icon">💬</div>
          <div class="text">消息中心</div>
        </div>
        <div class="card" @click="$router.push('/my-claims')">
          <div class="icon">📋</div>
          <div class="text">认领进度</div>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="container content-layout">
      <!-- Left: Item Lists -->
      <div class="main-list">
        <div class="tabs">
          <div class="active">最新失物</div>
        </div>
        
        <div class="item-grid">
          <div v-for="item in lostItems" :key="item.id" class="item-card" @click="viewDetail(item)">
            <div class="item-img">
              <img 
                :src="item.imageUrl ? 'http://localhost:8080'+item.imageUrl : '/placeholder.svg'" 
                @error="handleImageError"
                alt="Item" 
              />
              <span class="status-tag" :class="item.status">{{ statusText(item) }}</span>
            </div>
            <div class="item-info">
              <h4>{{ item.title }}</h4>
              <p class="meta">📍 {{ item.location }}</p>
              <p class="meta">🕒 {{ formatDate(item.createdAt) }}</p>
            </div>
          </div>
        </div>
        <div class="load-more">
          <button @click="$router.push('/query')">查看更多</button>
        </div>
      </div>

      <!-- Right: Sidebar -->
      <div class="sidebar">
        <!-- Text Notices -->
        <div class="sidebar-box">
          <h3>📢 最新公告</h3>
          <ul class="text-notice-list" v-if="textNotices.length > 0">
            <li v-for="item in textNotices" :key="item.id" class="text-notice-item">
              <div class="notice-title">{{ item.title }}</div>
              <div class="notice-date">{{ formatDate(item.updatedAt || item.createdAt || new Date()) }}</div>
            </li>
          </ul>
          <div v-else style="color:#999;font-size:13px;text-align:center;padding:10px">暂无公告</div>
        </div>

        <!-- Smart Match (Logged in only) -->
        <!--
        <div class="sidebar-box match-box" v-if="isLoggedIn">
          <h3>✨ 为你匹配</h3>
          <div v-if="matches.length === 0" class="empty-match">暂无匹配物品</div>
          <div v-else class="match-list">
            <div v-for="m in matches" :key="m.id" class="match-item" @click="$router.push(`/detail/found/${m.id}`)" style="cursor:pointer">
              <span>{{ m.title }}</span>
              <span class="match-score">{{ Math.round((m.matchScore || 0) * 100) }}%</span>
            </div>
          </div>
        </div>
        -->

        <!-- Hot Spots -->
        <div class="sidebar-box">
          <h3>🔥 高发地点</h3>
          <ul class="hot-list">
            <li v-for="item in hotLocations" :key="item.name" @click="searchByLocation(item.name)" class="clickable">
              <span class="loc-name">{{ item.name }}</span>
              <span class="loc-count">{{ item.value }}件</span>
            </li>
          </ul>
        </div>

        <!-- FAQ -->
        <div class="sidebar-box faq-box">
          <h3>❓ 常见问题</h3>
          <div class="faq-item" v-for="(faq, i) in faqs" :key="i">
            <div class="q" @click="faq.open = !faq.open">{{ faq.q }}</div>
            <div class="a" v-if="faq.open">{{ faq.a }}</div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import http from '@/api/http'

const store = useStore()
const router = useRouter()

// State
const notices = ref<any[]>([])
const currentNoticeIdx = ref(0)
const stats = ref({ lostWeekly: 32, claimedWeekly: 15 }) // Mock for now, replace with API if available
const lostItems = ref<any[]>([])
const matches = ref<any[]>([]) // Mock
const hotLocations = ref<any[]>([])
const faqs = ref([
  { q: '发布信息审核需要多久？', a: '通常在提交后1小时内完成审核。', open: false },
  { q: '认领物品需要带什么？', a: '请携带学生证或身份证进行线下核验。', open: false },
  { q: '如何撤销已发布的物品？', a: '在个人中心找到对应记录点击删除即可。', open: false },
  { q: '认领失误如何退认？', a: '点击认领进度点击误认的物品点击退认按钮即可。', open: false }
])

// Computed
const isLoggedIn = computed(() => !!store.state.auth.token)
const user = computed(() => store.state.auth.user || {})
const isAdmin = computed(() => ['ROLE_ADMIN', 'ROLE_MANAGER'].includes(user.value.role))
const imageNotices = computed(() => notices.value.filter(n => n.imageUrl))
const textNotices = computed(() => notices.value.filter(n => !n.imageUrl))
const currentNotice = computed(() => notices.value[currentNoticeIdx.value])

// Methods
const fetchNotices = async () => {
  try {
    const res = await http.get('/notice/list')
    notices.value = res.data || []
    if (notices.value.length === 0) {
      notices.value = [{ id: 1, title: '欢迎使用失物招领系统', content: '请大家积极发布失物与拾物信息，共同维护校园环境。' }]
    }
  } catch (e) {
    console.error(e)
    notices.value = [{ id: 1, title: '欢迎使用失物招领系统', content: '请大家积极发布失物与拾物信息，共同维护校园环境。' }]
  }
}

const fetchStats = async () => {
  try {
    const res = await http.get('/stat/home-summary')
    if (res.data) {
      stats.value = res.data
    }
    const resLoc = await http.get('/stat/hotLocation')
    if (resLoc.data) {
      hotLocations.value = resLoc.data
    }
  } catch (e) {
    console.error('Failed to fetch stats', e)
  }
}

const fetchMatches = async () => {
  if (!isLoggedIn.value) return
  try {
    const res = await http.get('/smart-match')
    matches.value = res.data || []
  } catch (e) {
    console.error('Failed to fetch matches', e)
  }
}

const fetchItems = async () => {
  try {
    const resLost = await http.get('/lost/query?page=0&size=6&t=' + new Date().getTime())
    lostItems.value = resLost.data.records
  } catch (e) {
    console.error(e)
  }
}

const statusText = (item: any) => {
  if (item.status === 'PENDING') return '待审核'
  if (item.status === 'APPROVED') return '寻找中'
  if (item.status === 'CLAIMED') return '已找回'
  return item.status
}

const formatDate = (ts: string) => {
  return new Date(ts).toLocaleDateString()
}

const viewDetail = (item: any) => {
  router.push(`/detail/lost/${item.id}`)
}

const searchByLocation = (loc: string) => {
  router.push({ path: '/query', query: { location: loc, keyword: loc } })
}

const handleImageError = (e: Event) => {
  const target = e.target as HTMLImageElement
  target.src = '/placeholder.svg'
}

// Lifecycle
onMounted(() => {
  fetchNotices()
  fetchItems()
  fetchStats()
  if (isLoggedIn.value) {
    fetchMatches()
  }
})
</script>

<style scoped>
/* Global Page Style */
.home-page {
  /* background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%); Moved to App.vue */
  min-height: 100vh;
  padding-bottom: 40px;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* Hero Section */
.hero-section {
  background: transparent;
  padding: 30px 0;
  margin-bottom: 20px;
}

.hero-container {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.carousel-wrapper {
  flex: 2;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
  background: transparent;
}

.carousel-wrapper :deep(.el-carousel),
.carousel-wrapper :deep(.el-carousel__container),
.carousel-wrapper :deep(.el-carousel__item) {
  background: transparent !important;
}

.carousel-img :deep(.el-image__inner) {
  vertical-align: bottom;
  display: block;
}

.welcome-slide {
  background: linear-gradient(120deg, #a1c4fd 0%, #c2e9fb 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  height: 100%;
  color: #fff;
}

.welcome-text h3 {
  color: #fff;
  font-size: 28px;
  margin-bottom: 12px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.welcome-text p {
  font-size: 16px;
  opacity: 0.9;
}

.carousel-content {
  position: relative;
  width: 100%;
  height: 100%;
}

.carousel-img {
  width: 100%;
  height: 100%;
  display: block;
  vertical-align: bottom;
  transition: transform 0.5s ease;
}

.carousel-text {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  background: linear-gradient(to top, rgba(0,0,0,0.7), transparent);
  color: #fff;
  padding: 20px;
  box-sizing: border-box;
}

.carousel-text h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0,0,0,0.3);
}

/* Hero Stats */
.hero-stats {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-box {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  padding: 20px;
  border-radius: 16px;
  text-align: center;
  box-shadow: 0 8px 16px rgba(0,0,0,0.05);
  transition: transform 0.3s ease;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.stat-box:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(0,0,0,0.1);
}

.stat-box h4 {
  margin: 0 0 10px 0;
  color: #666;
  font-size: 15px;
  font-weight: 500;
}

.stat-num {
  font-size: 32px;
  font-weight: 700;
  color: #ff6b6b;
  background: -webkit-linear-gradient(45deg, #ff6b6b, #ff8e8e);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.stat-num.success {
  color: #51cf66;
  background: -webkit-linear-gradient(45deg, #51cf66, #69db7c);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.guide-btn {
  background: linear-gradient(45deg, #4facfe 0%, #00f2fe 100%);
  color: white;
  border: none;
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 18px;
  font-weight: 600;
  box-shadow: 0 4px 15px rgba(79, 172, 254, 0.4);
  transition: all 0.3s ease;
}

.guide-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(79, 172, 254, 0.6);
}

/* Quick Access */
.quick-access {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 40px;
}

.card {
  flex: 1;
  background: rgba(255, 255, 255, 0.9);
  padding: 24px;
  border-radius: 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  border: 1px solid rgba(255,255,255,0.5);
}

.card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 24px rgba(0,0,0,0.1);
}

.card .icon {
  font-size: 36px;
  margin-bottom: 12px;
  transition: transform 0.3s ease;
}

.card:hover .icon {
  transform: scale(1.1);
}

.card .text {
  font-weight: 600;
  color: #444;
  font-size: 16px;
}

/* Main Content Layout */
.content-layout {
  display: flex;
  gap: 30px;
}

.main-list {
  flex: 3;
}

.sidebar {
  flex: 1;
}

/* Tabs */
.tabs {
  display: flex;
  gap: 30px;
  margin-bottom: 24px;
  border-bottom: 1px solid rgba(0,0,0,0.1);
  padding-bottom: 5px;
}

.tabs div {
  padding-bottom: 12px;
  cursor: pointer;
  font-size: 20px;
  font-weight: 600;
  color: #888;
  position: relative;
  transition: color 0.3s;
}

.tabs div.active {
  color: #333;
}

.tabs div.active::after {
  content: '';
  position: absolute;
  bottom: -6px; /* Adjust to match border-bottom */
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, #4facfe, #00f2fe);
  border-radius: 3px;
}

/* Item Grid */
.item-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.item-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(0,0,0,0.02);
}

.item-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(0,0,0,0.1);
}

.item-img {
  height: 180px;
  background: #f0f0f0;
  position: relative;
  overflow: hidden;
}

.item-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.item-card:hover .item-img img {
  transform: scale(1.05);
}

.status-tag {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(0,0,0,0.6);
  backdrop-filter: blur(4px);
  color: white;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.status-tag.APPROVED {
  background: rgba(82, 196, 26, 0.9);
}

.item-info {
  padding: 16px;
}

.item-info h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta {
  font-size: 13px;
  color: #888;
  margin: 6px 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.load-more {
  text-align: center;
  margin-top: 40px;
}

.load-more button {
  padding: 12px 40px;
  border: none;
  background: white;
  color: #555;
  border-radius: 30px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  font-size: 15px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.load-more button:hover {
  background: #f8f9fa;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0,0,0,0.12);
  color: #333;
}

/* Sidebar */
.sidebar-box {
  background: white;
  padding: 24px;
  border-radius: 16px;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.sidebar-box h3 {
  margin-top: 0;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 16px;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.text-notice-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.text-notice-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px dashed #eee;
  cursor: pointer;
  transition: all 0.2s;
}

.text-notice-item:last-child {
  border-bottom: none;
}

.text-notice-item:hover {
  padding-left: 5px;
}

.text-notice-item:hover .notice-title {
  color: #4facfe;
}

.notice-title {
  font-size: 14px;
  color: #555;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: 10px;
}

.notice-date {
  font-size: 12px;
  color: #aaa;
}

.hot-list {
  list-style: none;
  padding: 0;
}

.hot-list li {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f9f9f9;
  cursor: pointer;
  transition: background-color 0.2s;
  border-radius: 4px;
}

.hot-list li:hover {
  background-color: #f8f9fa;
  padding-left: 8px;
  padding-right: 8px;
}

.loc-name {
  color: #555;
}

.loc-count {
  background: #f0f2f5;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  color: #888;
}

.faq-item {
  border-bottom: 1px solid #f0f0f0;
}

.faq-item:last-child {
  border-bottom: none;
}

.faq-item .q {
  padding: 14px 0;
  cursor: pointer;
  font-weight: 600;
  color: #444;
  font-size: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.faq-item .q:hover {
  color: #4facfe;
}

.faq-item .a {
  padding-bottom: 14px;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  background: #f9f9f9;
  padding: 10px;
  border-radius: 6px;
  margin-bottom: 10px;
}
</style>
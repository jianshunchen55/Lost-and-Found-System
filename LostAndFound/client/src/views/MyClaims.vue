<template>
  <div class="container">
    <h2>我的认领记录</h2>
    
    <div v-if="loading" class="loading">加载中...</div>
    
    <div v-else>
      <div v-if="claims.length === 0" class="empty-state">
        <p>暂无认领记录</p>
        <button @click="$router.push('/')">去首页看看</button>
      </div>
      
      <div v-else class="items-list">
        <div v-for="item in claims" :key="item.id" class="item-card" @click="$router.push(`/detail/found/${item.id}`)">
          <div class="thumb">
            <img :src="item.thumbnailUrl ? 'http://localhost:8080'+item.thumbnailUrl : '/placeholder.svg'" />
          </div>
          <div class="info">
            <h3>{{ item.title }}</h3>
            <div class="meta">
              <span>{{ formatDate(item.claimTime) }} 申请认领</span>
              <span class="status-tag" :class="statusClass(item.claimStatus)">{{ statusText(item.claimStatus) }}</span>
            </div>
            <p>{{ item.description }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import http from '../api/http'

const loading = ref(false)
const claims = ref<any[]>([])

const fetchClaims = async () => {
  loading.value = true
  try {
    const res = await http.get('/my/claims')
    claims.value = res.data.lost || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const formatDate = (ts: string) => {
  if (!ts) return ''
  return new Date(ts).toLocaleString()
}

const statusText = (status: number) => {
  if (status === 1) return '审核中' // Assuming 1 is claimed/pending approval if logic differs, adjust
  if (status === 2) return '已通过'
  if (status === -1) return '已拒绝'
  return '处理中'
}

const statusClass = (status: number) => {
  if (status === 2) return 'success'
  if (status === -1) return 'danger'
  return 'pending'
}

onMounted(() => {
  fetchClaims()
})
</script>

<style scoped>
.container {
  max-width: 1000px;
  margin: 40px auto;
  padding: 20px;
  background: white;
  min-height: 400px;
  border-radius: 8px;
}
.empty-state {
  text-align: center;
  padding-top: 60px;
  color: #999;
}
.empty-state button {
  margin-top: 20px;
  padding: 8px 20px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.item-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  border: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.2s;
}
.item-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
.thumb img {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 4px;
}
.info {
  flex: 1;
}
.info h3 {
  margin: 0 0 8px 0;
}
.meta {
  display: flex;
  gap: 12px;
  color: #666;
  font-size: 14px;
  margin-bottom: 8px;
  align-items: center;
}
.status-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}
.status-tag.pending { background: #e6f7ff; color: #1890ff; }
.status-tag.success { background: #f6ffed; color: #52c41a; }
.status-tag.danger { background: #fff1f0; color: #f5222d; }
</style>

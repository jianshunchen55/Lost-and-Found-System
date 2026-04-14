<template>
  <div class="query-page">
    <div class="filter-card">
      <el-form :inline="true" :model="q" class="search-form">
        <!-- Removed Type Selection -->
        <el-form-item label="物品类别">
          <el-select v-model="q.category" clearable placeholder="请选择">
            <el-option label="全部" value="" />
            <el-option label="证件" value="card" />
            <el-option label="电子设备" value="electronic" />
            <el-option label="文具" value="book" />
            <el-option label="生活用品" value="daily" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="q.keyword" placeholder="输入关键词" :prefix-icon="Search" />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="q.location" placeholder="输入地点" :prefix-icon="Location" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="onlyApproved">仅显示待认领</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetFilters">清空</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="results-card">
      <el-table :data="list" style="width:100%">
        <el-table-column label="发布者" width="170">
          <template #default="{row}">
            <el-popover placement="top" :width="220" trigger="click">
              <template #reference>
                <div style="display:flex;align-items:center;cursor:pointer">
                   <el-avatar :src="row.publisherAvatar ? (row.publisherAvatar) : ''" :size="30" style="margin-right:8px">{{ row.publisherNickname?.[0]?.toUpperCase() }}</el-avatar>
                   <span style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ row.publisherNickname || '未知用户' }}</span>
                </div>
              </template>
              <div style="text-align:center">
                 <p style="font-weight:bold;margin-bottom:12px">{{ row.publisherNickname }}</p>
                 <div class="popover-actions">
                   <el-button size="small" type="primary" @click="openChat(row)">发送私信</el-button>
                   <el-button size="small" class="add-friend-btn" @click="addFriend(row)">添加好友</el-button>
                 </div>
              </div>
            </el-popover>
          </template>
        </el-table-column>
        <el-table-column label="图片" width="90">
          <template #default="{row}">
            <img :src="row.thumbnailUrl ? (row.thumbnailUrl) : '/placeholder.svg'" style="width:60px;height:60px;object-fit:cover;border-radius:8px" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="物品名" width="160" />
        <el-table-column prop="location" label="地点" min-width="200" />
        <el-table-column label="类别" width="80">
          <template #default="{row}">{{ categoryText(row.category) }}</template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="{row}">{{ formatDate(row.foundTime) || formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="智能匹配度" width="100">
          <template #default="{row}">
            {{ row.matchScore ? (row.matchScore * 100).toFixed(0) + '%' : '0%' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{row}">{{ statusText(row) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="100">
          <template #default="{row}">
            <el-button size="small" type="primary" link @click="viewDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination background :page-size="page.size" :current-page="page.page" :total="page.total" layout="prev, pager, next" @current-change="(p:number)=>{page.page=p;search()}"/>
      </div>
    </div>
    
    <el-dialog v-model="visible" title="匹配结果">
      <div v-if="matchResult">
        <p>匹配度：{{matchResult.score}}%</p>
        <p>候选：{{matchResult.candidate?.title || '无候选'}}</p>
      </div>
    </el-dialog>
    
    <el-dialog v-model="chatVisible" title="发送私信" width="400px">
      <el-input v-model="chatContent" type="textarea" :rows="3" placeholder="请输入内容" />
      <template #footer>
        <el-button @click="chatVisible = false">取消</el-button>
        <el-button type="primary" @click="sendChat">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import http from '../api/http'
import { reactive, ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Location } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const q = reactive({ category:'', keyword:'', location:'' })
const page = reactive({ page:1, size:10, total:0 })
const list = ref<any[]>([])
const visible = ref(false)
const matchResult = ref<{score:number, candidate:any} | null>(null)
const onlyApproved = ref(false)

const chatVisible = ref(false)
const chatContent = ref('')
const currentTarget = ref<any>(null)

function openChat(row: any) {
  currentTarget.value = row
  chatVisible.value = true
}

async function sendChat() {
  if(!chatContent.value) return
  try {
    await http.post('/chat/send', { receiverId: currentTarget.value.userId, content: chatContent.value })
    ElMessage.success('发送成功')
    chatVisible.value = false
    chatContent.value = ''
  } catch(e) { 
    console.error(e)
    ElMessage.error('发送失败') 
  }
}

async function addFriend(row: any) {
  try {
     await http.post('/friend/add', { targetId: row.userId })
     ElMessage.success('好友请求已发送')
  } catch(e: any) { 
    console.error(e)
    if (e.response && e.response.status === 409) {
        ElMessage.info('对方已经是您的好友！')
    } else {
        ElMessage.error('发送失败') 
    }
  }
}

async function search() {
  try {
    const url = '/found/query'
    const { data } = await http.get(url, { params: { 
      page: page.page-1, 
      size: page.size, 
      category: q.category, 
      keyword: q.keyword, 
      location: q.location,
      status: onlyApproved.value ? 'APPROVED' : undefined
    }})
    list.value = data.records
    page.total = data.total
  } catch (e) { console.error(e) }
}

async function match(row: any) {
  try {
    const { data } = await http.post('/message/push', { itemId: row.id })
    matchResult.value = data
    visible.value = true
  } catch (e) { 
    console.error(e) 
    alert('匹配失败')
  }
}

function handleTypeChange() {
  page.page = 1
  search()
}

onMounted(() => {
  if (route.query.keyword) q.keyword = route.query.keyword as string
  if (route.query.category) q.category = route.query.category as string
  if (route.query.location) q.location = route.query.location as string
  search()
})

watch(() => route.query, (newQuery) => {
  q.keyword = (newQuery.keyword as string) || ''
  q.category = (newQuery.category as string) || ''
  q.location = (newQuery.location as string) || ''
  search()
})

function statusText(row: any) { 
  if (row.status === 'PENDING') return '待审核'
  if (row.status === 'APPROVED') {
    if (row.claimStatus === 1) return '认领审核中'
    return '待认领'
  }
  if (row.status === 'CLAIMED') return '已认领'
  return row.status 
}
function categoryText(c:string) {
  const map: Record<string, string> = {
    card: '证件',
    electronic: '电子设备',
    book: '文具',
    daily: '生活用品',
    other: '其他'
  }
  return map[c] || c
}
function formatDate(ts:string) { try { return new Date(ts).toLocaleString() } catch { return ts } }

watch(() => q.category, () => { page.page = 1; search() })
watch(() => q.keyword, () => { page.page = 1; })
watch(() => q.location, () => { page.page = 1; })
watch(onlyApproved, () => { page.page = 1; search() })

function resetFilters() {
  q.category = ''
  q.keyword = ''
  q.location = ''
  onlyApproved.value = false
  page.page = 1
  search()
}

function viewDetail(row: any) {
  router.push(`/detail/found/${row.id}`)
}
</script>

<style scoped>
.query-page {
  max-width: 1280px;
  margin: 30px auto;
  padding: 0 24px;
}

.filter-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  padding: 24px;
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.6);
  margin-bottom: 24px;
}

.results-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  padding: 24px;
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.6);
  min-height: 500px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

/* Custom Element Plus overrides */
:deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 16px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: #2c3e50;
}

:deep(.el-input__wrapper), :deep(.el-select__wrapper) {
  box-shadow: none !important;
  background: rgba(245, 247, 250, 0.5);
  border: 1px solid rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  border-radius: 12px;
  padding: 4px 12px;
}

:deep(.el-input__wrapper:hover), :deep(.el-select__wrapper:hover) {
  background: rgba(255, 255, 255, 0.8);
  border-color: #4facfe;
}

:deep(.el-input__wrapper.is-focus), :deep(.el-select__wrapper.is-focused) {
  background: #fff;
  border-color: #4facfe;
  box-shadow: 0 0 0 3px rgba(79, 172, 254, 0.1) !important;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border: none;
  border-radius: 12px;
  padding: 8px 24px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(79, 172, 254, 0.3);
  transition: all 0.3s ease;
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(79, 172, 254, 0.4);
}

:deep(.el-button:not(.el-button--primary)) {
  border-radius: 12px;
  padding: 8px 20px;
  border: 1px solid rgba(0,0,0,0.1);
  background: transparent;
}

:deep(.el-button:not(.el-button--primary):hover) {
  color: #4facfe;
  border-color: #4facfe;
  background: rgba(79, 172, 254, 0.05);
}

:deep(.el-table) {
  background: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: transparent;
}

:deep(.el-table__row) {
  transition: all 0.3s ease;
}

:deep(.el-table__row:hover) {
  background-color: rgba(79, 172, 254, 0.05) !important;
  transform: scale(1.002);
}

:deep(.el-checkbox__inner) {
  border-radius: 4px;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #4facfe;
  border-color: #4facfe;
}

:deep(.el-checkbox__label) {
  color: #555;
}

:deep(.el-checkbox.is-bordered.is-checked) {
  border-color: #4facfe;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #4facfe;
}

@media (max-width: 768px) {
  .search-form {
    flex-direction: column;
    align-items: stretch;
  }
  
  :deep(.el-form-item) {
    margin-right: 0;
    margin-bottom: 12px;
  }
  
  :deep(.el-form-item__content) {
    justify-content: flex-end;
  }
}

.popover-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.add-friend-btn {
  color: #67c23a !important;
  border-color: #67c23a !important;
  background: #f0f9eb !important;
}

.add-friend-btn:hover {
  color: #fff !important;
  background: #67c23a !important;
  border-color: #67c23a !important;
}
</style>

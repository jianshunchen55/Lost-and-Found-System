<template>
  <div class="detail-page container" v-if="item">
    <div class="header">
      <div class="header-top">
        <h2>{{ item.title }}</h2>
        <div class="publisher-section" v-if="item.publisherNickname">
            <img 
              :src="item.publisherAvatar ? 'http://localhost:8080' + item.publisherAvatar : '/default-avatar.png'" 
              class="publisher-avatar"
            />
            <span class="publisher-name">{{ item.publisherNickname }}</span>
        </div>
        <div class="header-actions" v-if="isOwner">
          <button class="edit-btn" v-if="['PENDING', 'APPROVED'].includes(item.status)" @click="editItem">编辑</button>
          <button class="del-btn" v-if="['PENDING', 'APPROVED'].includes(item.status)" @click="deleteItem">
            {{ item.status === 'APPROVED' ? (type === 'lost' ? '已找回/删除' : '删除') : '撤销发布' }}
          </button>
        </div>
        <div class="header-actions" v-else>
        <button 
          class="del-btn"
          v-if="type === 'found' && item.claimStatus === 1 && isClaimer"
          @click="unclaimItem"
        >
          退认
        </button>
        <button 
          class="claim-btn" 
          v-else-if="type === 'found' && item.claimStatus !== 2 && item.auditStatus === 1" 
          :disabled="isOwner || item.claimStatus === 1"
          :style="(isOwner || item.claimStatus === 1) ? { borderColor:'#ccc', color:'#999', cursor:'not-allowed' } : {}"
          @click="applyClaim"
        >
          {{ isOwner ? '无法认领自己发布的物品' : (item.claimStatus === 1 ? '认领审核中' : '申请认领') }}
        </button>
      </div>
      </div>
      <span class="badge" :class="item.status">{{ statusText(item.status) }}</span>
      <div class="meta">
        <span>类别：{{ categoryText(item.category) }}</span>
        <span v-if="type==='lost'">丢失时间：{{ item.lostTime ? formatDate(item.lostTime) : '未填写' }}</span>
        <span v-else>拾到时间：{{ item.foundTime ? formatDate(item.foundTime) : '未填写' }}</span>
      </div>
      <div class="meta">
        <span>地点：{{ item.location || '未填写' }}</span>
        <span>联系方式：{{ item.contact || '未填写' }}</span>
        <span v-if="item.status === 'CLAIMED' && item.claimTime">认领时间：{{ formatDate(item.claimTime) }}</span>
      </div>
    </div>
    
    <div class="content">
      <div class="left-col">
        <div class="images" v-if="images.length">
          <img v-for="(img,i) in images" :key="i" :src="toUrl(img)" />
        </div>
        <div class="map-section" v-if="item.latitude && item.longitude">
           <h4>位置信息</h4>
           <MapSelector 
             :mapSrc="mapImageSrc" 
             :initialPoint="{x: item.longitude, y: item.latitude}" 
             :readonly="true"
             style="width: 100%;"
           />
        </div>
      </div>
      <div class="desc">
        <h4>描述</h4>
        <pre class="text">{{ item.description || '暂无描述' }}</pre>
      </div>
    </div>

    <div class="actions">
      <button class="primary-btn" @click="$router.back()">返回</button>
      <button class="text-btn" @click="$router.push(`/profile?tab=mypost&type=${type}`)">我的发布</button>
    </div>

    <!-- Claim Dialog -->
    <el-dialog v-model="showClaimDialog" title="申请认领信息填写" width="400px">
      <el-form :model="claimForm" label-width="80px">
        <el-form-item label="姓名" required>
          <el-input v-model="claimForm.name" placeholder="请输入您的真实姓名"></el-input>
        </el-form-item>
        <el-form-item label="联系电话" required>
          <el-input v-model="claimForm.phone" placeholder="请输入您的联系电话"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showClaimDialog = false">取消</el-button>
          <el-button type="primary" @click="submitClaim">提交申请</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import MapSelector from '../components/MapSelector.vue'

const route = useRoute()
const router = useRouter()
const store = useStore()
const type = (route.params.type as string) || 'lost'
const id = route.params.id as string
const item = ref<any>(null)
const images = ref<string[]>([])
const showClaimDialog = ref(false)
const claimForm = ref({ name: '', phone: '' })

const user = computed(() => store.state.auth.user)
const isOwner = computed(() => item.value && user.value && String(item.value.userId) === String(user.value.id))
const isClaimer = computed(() => item.value && user.value && String(item.value.claimUserId) === String(user.value.id))

const mapImageSrc = ref('data:image/svg+xml;utf8,' + encodeURIComponent(`
  <svg xmlns="http://www.w3.org/2000/svg" width="800" height="500">
    <defs>
      <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
        <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#eee" stroke-width="1"/>
      </pattern>
    </defs>
    <rect width="100%" height="100%" fill="#fafafa"/>
    <rect width="100%" height="100%" fill="url(#grid)"/>
    <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="#999" font-size="20">
      校园地图占位图（未提供图片）
    </text>
  </svg>
`))

async function loadMapConfig() {
  try {
    const res = await http.get('/config/map_url')
    if (res.data && res.data.value) {
      mapImageSrc.value = res.data.value.startsWith('http') ? res.data.value : ('http://localhost:8080' + res.data.value)
    }
  } catch (e) {
    // Keep default
  }
}

function toUrl(u: string) {
  if (!u) return ''
  if (u.startsWith('http://') || u.startsWith('https://')) return u
  return 'http://localhost:8080' + u
}
function formatDate(ts: string) { return new Date(ts).toLocaleString() }
function statusText(s: string) { 
  if (item.value && item.value.status === 'APPROVED' && item.value.claimStatus === 1) return '认领审核中'
  const map: any = {'PENDING':'待审核','APPROVED':'待认领','CLAIMED':'已认领','REJECTED':'已驳回','发布':'待认领','退回':'已退回'}
  if (type === 'lost') {
    map['APPROVED'] = '待找回'
    map['CLAIMED'] = '已找回'
    map['发布'] = '待找回'
  }
  return map[s] || s 
}
function categoryText(c: string) {
  const map: any = {
    'card': '证件',
    'electronic': '电子设备',
    'book': '书籍',
    'daily': '生活用品',
    'other': '其他'
  }
  return map[c] || c || '未填写'
}

function editItem() {
  if (type === 'lost') {
    router.push(`/publish/lost/${id}`)
  } else {
    router.push({ path: '/publish/found', query: { id } })
  }
}

async function deleteItem() {
  if (!confirm('确定要撤销这条发布吗？')) return
  try {
    const url = type === 'lost' ? `/my/lost/${id}` : `/my/found/${id}`
    await http.delete(url)
    ElMessage.success('已撤销并删除')
    router.push('/profile?tab=mypost')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function applyClaim() {
  claimForm.value.name = user.value?.nickname || ''
  claimForm.value.phone = user.value?.phone || ''
  showClaimDialog.value = true
}

async function submitClaim() {
  if (!claimForm.value.name || !claimForm.value.phone) {
    ElMessage.warning('请填写姓名和电话')
    return
  }
  try {
    const url = type === 'lost' ? `/lost/claim` : `/found/claim`
    await http.post(url, { 
      id: item.value.id,
      name: claimForm.value.name,
      phone: claimForm.value.phone
    })
    ElMessage.success('已提交认领申请')
    showClaimDialog.value = false
    const res = await http.get(type === 'lost' ? `/lost/${id}` : `/found/${id}`)
    item.value = res.data
  } catch (e) {
    ElMessage.error((e as any).response?.data || '操作失败')
  }
}

async function unclaimItem() {
  if (!confirm('确定要取消认领吗？')) return
  try {
    await http.post(`/my/claims/${id}/unclaim`)
    ElMessage.success('已取消认领')
    const res = await http.get(`/found/${id}`)
    item.value = res.data
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

onMounted(async () => {
  loadMapConfig()
  try {
    if (type === 'lost') {
      const res = await http.get(`/lost/${id}`)
      item.value = res.data
    } else {
      const res = await http.get(`/found/${id}`)
      item.value = res.data
    }
    if (item.value.images) {
      try { images.value = JSON.parse(item.value.images) }
      catch { images.value = (item.value.imageUrl || '').split(',').filter((x:string)=>x) }
    } else {
      images.value = (item.value.imageUrl || '').split(',').filter((x:string)=>x)
    }
  } catch (e) {
    item.value = null
  }
})
</script>
<style scoped>
.detail-page { max-width: 960px; margin: 24px auto; }
.header { background: #fff; padding: 16px 24px; border-radius: 8px; }
.header-top { display: flex; justify-content: space-between; align-items: center; }
.header-actions button { margin-left: 10px; padding: 4px 12px; border: 1px solid #ddd; background: #fff; border-radius: 4px; cursor: pointer; }
.header-actions .edit-btn { color: #1890ff; border-color: #1890ff; }
.header-actions .del-btn { color: #ff4d4f; border-color: #ff4d4f; }
.header-actions .claim-btn { color: #52c41a; border-color: #52c41a; }

.badge { margin-left: 0; display: inline-block; margin-top: 8px; padding: 2px 8px; border-radius: 4px; background: #eee; font-size: 12px; }
.badge.PENDING { background: #ffe58f; }
.badge.APPROVED { background: #bae7ff; }
.badge.CLAIMED { background: #d9f7be; }
.meta { color: #666; margin-top: 8px; display: flex; gap: 24px; }
.content { display: grid; grid-template-columns: 1.3fr 1fr; gap: 16px; margin-top: 16px; }
.left-col { display: flex; flex-direction: column; gap: 16px; }
.images { background: #fff; padding: 12px; border-radius: 8px; display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.images img { width: 100%; height: 160px; object-fit: cover; border-radius: 6px; }
.map-section { background: #fff; padding: 12px; border-radius: 8px; }
.map-section h4 { margin-bottom: 8px; }
.desc { background: #fff; padding: 12px; border-radius: 8px; height: fit-content; }
.text { white-space: pre-wrap; font-family: inherit; }
.actions { margin-top: 16px; display: flex; gap: 8px; }
.primary-btn { background: #1677ff; color: #fff; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; }
.text-btn { background: transparent; color: #1677ff; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; }
.publisher-section { display: flex; align-items: center; margin: 0 15px; }
.publisher-avatar { width: 40px; height: 40px; border-radius: 50%; border: 2px solid transparent; }
.publisher-name { margin-left: 8px; font-weight: bold; color: #333; }
</style>

<template>
  <div class="profile-container">
    <div class="sidebar-card">
      <div class="user-profile">
        <div class="avatar-wrapper">
          <img :src="user.avatar ? 'http://localhost:8080'+user.avatar : '/default-avatar.png'" alt="Avatar" class="avatar"/>
          <div class="avatar-mask" @click="triggerUpload">
            <el-icon><Camera /></el-icon>
            <span>更换头像</span>
          </div>
          <input type="file" ref="fileInput" @change="handleUpload" accept="image/*" hidden />
        </div>
        <h3 class="username">{{ user.nickname || user.username }}</h3>
        <span class="role-badge">{{ roleText }}</span>
      </div>

      <div class="nav-menu">
        <div class="menu-item" :class="{active: currentTab==='info'}" @click="currentTab='info'">
          <el-icon><UserIcon /></el-icon>
          <span>个人信息</span>
        </div>
        <div class="menu-item" :class="{active: currentTab==='records'}" @click="currentTab='records'">
          <el-icon><List /></el-icon>
          <span>我的发布</span>
        </div>
        <div class="menu-item" :class="{active: currentTab==='claims'}" @click="currentTab='claims'">
          <el-icon><Goods /></el-icon>
          <span>我的认领</span>
        </div>
        <div class="menu-item" :class="{active: currentTab==='friends'}" @click="currentTab='friends'">
          <el-icon><Connection /></el-icon>
          <span>我的好友</span>
        </div>
        <div class="menu-item" :class="{active: currentTab==='security'}" @click="currentTab='security'">
          <el-icon><Lock /></el-icon>
          <span>账号安全</span>
        </div>
      </div>
    </div>

    <div class="content-card">
      <!-- Info Tab -->
      <div v-if="currentTab==='info'" class="tab-pane fade-in">
        <div class="section-header">
          <h2>个人信息</h2>
          <p class="subtitle">管理您的个人资料和联系方式</p>
        </div>
        <form @submit.prevent="updateProfile" class="glass-form">
          <div class="form-row">
            <div class="form-group">
              <label>账号</label>
              <input :value="user.username" disabled class="glass-input disabled"/>
            </div>
            <div class="form-group">
              <label>角色</label>
              <input :value="roleText" disabled class="glass-input disabled"/>
            </div>
          </div>
          <div class="form-group">
            <label>昵称</label>
            <input v-model="form.nickname" placeholder="设置昵称" class="glass-input"/>
          </div>
          <div class="form-group">
            <label>院系/部门</label>
            <div class="select-wrapper">
              <select v-model="form.department" class="glass-input">
                <option value="" disabled>请选择院系</option>
                <option v-for="dept in departments" :key="dept" :value="dept">{{ dept }}</option>
              </select>
            </div>
          </div>
          <div class="form-group">
            <label>手机号码</label>
            <input v-model="form.phone" placeholder="联系电话" class="glass-input"/>
          </div>
          <div class="form-group">
            <label>电子邮箱</label>
            <input v-model="form.email" type="email" placeholder="Email" class="glass-input"/>
          </div>
          <div class="form-group">
            <label>个人简介</label>
            <textarea v-model="form.bio" rows="3" class="glass-input textarea"></textarea>
          </div>
          <div class="form-actions">
            <button type="submit" class="gradient-btn">保存修改</button>
          </div>
        </form>
      </div>

      <!-- Records Tab -->
      <div v-if="currentTab==='records'" class="tab-pane fade-in">
        <div class="tab-header">
          <div class="sub-tabs">
            <span :class="{active: recordType==='lost'}" @click="recordType='lost'">我的失物</span>
            <span :class="{active: recordType==='found'}" @click="recordType='found'">我的拾物</span>
          </div>
          <button class="gradient-btn sm" @click="$router.push(recordType==='lost' ? '/publish/lost' : '/publish/found')">
            + 发布{{ recordType==='lost' ? '失物' : '拾物' }}
          </button>
        </div>

        <div class="records-grid">
          <div v-for="item in records" :key="item.id" class="record-card">
            <div class="card-thumb">
              <img :src="item.thumbnailUrl ? 'http://localhost:8080'+item.thumbnailUrl : '/placeholder.svg'"/>
              <span class="status-badge" :class="item.status">{{ statusText(item.status) }}</span>
            </div>
            <div class="card-content">
              <h4>{{ item.title }}</h4>
              <p class="time">{{ formatDate(item.createdAt) }}</p>
              <div class="card-actions">
                <button v-if="item.status==='PENDING'" @click="editRecord(item)" class="action-btn edit">编辑</button>
                <button v-if="item.status==='PENDING'" @click="deleteRecord(item)" class="action-btn delete">撤销</button>
                <button v-if="item.status==='CLAIMED' || item.status==='RETURNED'" @click="deleteRecord(item)" class="action-btn delete">删除</button>
                <button @click="$router.push(`/detail/${recordType}/${item.id}`)" class="action-btn view">详情</button>
              </div>
            </div>
          </div>
          <div v-if="records.length===0" class="empty-state">
            <el-icon :size="48"><List /></el-icon>
            <p>暂无记录</p>
          </div>
        </div>
      </div>


      <!-- Claims Tab -->
      <div v-if="currentTab==='claims'" class="tab-pane fade-in">
         <div class="section-header">
            <h2>我的认领</h2>
         </div>
         
         <div class="records-grid">
            <div v-for="item in myLostClaims" :key="item.id" class="record-card clickable" @click="$router.push(`/detail/found/${item.id}`)">
               <div class="card-thumb">
                  <img :src="item.thumbnailUrl ? 'http://localhost:8080'+item.thumbnailUrl : '/placeholder.svg'"/>
                  <span class="status-badge" :class="claimStatusClass(item.claimStatus)">{{ claimStatusText(item.claimStatus) }}</span>
               </div>
               <div class="card-content">
                  <h4>{{ item.title }}</h4>
                  <p class="desc">{{ item.description }}</p>
                  <p class="time">申请时间：{{ formatDate(item.claimTime) }}</p>
                  <div class="card-actions" v-if="item.claimStatus === 1">
                    <button @click.stop="unclaimItem(item.id)" class="action-btn delete">取消认领</button>
                  </div>
               </div>
            </div>
            <div v-if="myLostClaims.length===0" class="empty-state">
              <el-icon :size="48"><Goods /></el-icon>
              <p>暂无认领记录</p>
            </div>
         </div>
      </div>

      <!-- Security Tab -->
      <div v-if="currentTab==='security'" class="tab-pane fade-in">
        <div class="section-header">
          <h2>账号安全</h2>
        </div>
        <div class="security-card">
          <div class="sec-icon">
            <el-icon><Lock /></el-icon>
          </div>
          <div class="sec-info">
            <h4>登录密码</h4>
            <p>建议定期更换密码以保障账号安全</p>
          </div>
          <button @click="showPwdModal=true" class="gradient-btn outline">修改密码</button>
        </div>
      </div>

      <!-- Friends Tab -->
      <div v-if="currentTab==='friends'" class="tab-pane fade-in">
        <div class="section-header">
          <h2>我的好友</h2>
        </div>
        <div class="friends-grid">
          <div v-for="friend in friends" :key="friend.id" class="friend-card" @click="openChat(friend)">
            <div class="friend-avatar">
              <img :src="friend.avatar ? 'http://localhost:8080'+friend.avatar : '/default-avatar.png'" />
              <div v-if="friend.unreadCount && friend.unreadCount > 0" class="unread-dot">{{ friend.unreadCount }}</div>
            </div>
            <div class="friend-info">
              <h4>
                {{ friend.remark || friend.nickname || friend.username }}
                <el-icon class="edit-icon" @click="openRemarkDialog(friend, $event)"><Edit /></el-icon>
              </h4>
              <p>{{ friend.department || '暂无院系信息' }}</p>
            </div>
            <div class="friend-actions-right">
              <button class="icon-btn chat" title="聊天">
                <el-icon><ChatDotRound /></el-icon>
              </button>
              <button class="icon-btn delete-friend" @click.stop="deleteFriend(friend, $event)" title="删除好友">
                <el-icon><Delete /></el-icon>
              </button>
            </div>
          </div>
          <div v-if="friends.length===0" class="empty-state">
            <el-icon :size="48"><Connection /></el-icon>
            <p>暂无好友</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Remark Dialog -->
    <el-dialog v-model="remarkDialogVisible" title="修改备注" width="400px" class="glass-dialog">
      <div class="glass-input-wrapper">
        <el-input v-model="newRemark" placeholder="请输入备注名" @keyup.enter="saveRemark" class="glass-input-el"></el-input>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="remarkDialogVisible = false" class="glass-btn">取消</el-button>
          <el-button type="primary" @click="saveRemark" class="gradient-btn">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- Chat Dialog -->
    <el-dialog v-model="replyVisible" :title="replyTarget?.senderNickname || '聊天'" width="600px" class="glass-dialog chat-dialog-window">
      <div class="chat-container" ref="chatBox">
        <div v-for="(msg, index) in displayList" :key="msg.id">
          <div v-if="msg.showTime" class="chat-time">{{ formatTime(msg.createdAt) }}</div>
          <div class="chat-msg" :class="{ mine: isMine(msg) }">
            <el-avatar class="chat-avatar" :src="getAvatar(msg)" shape="square" :size="36">{{ getNickname(msg)[0]?.toUpperCase() }}</el-avatar>
            <div class="chat-content">
              <div class="chat-bubble">{{ msg.content }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="chat-input-area">
        <el-input v-model="replyContent" type="textarea" :rows="3" placeholder="按 Enter 发送" @keydown.enter.prevent="sendReply" resize="none" class="chat-textarea"/>
        <div class="chat-actions">
          <el-button @click="replyVisible = false" class="glass-btn">关闭</el-button>
          <el-button type="primary" @click="sendReply" class="gradient-btn">发送</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- Password Modal -->
    <div v-if="showPwdModal" class="modal-mask">
      <div class="modal-box glass-modal">
        <h3>修改密码</h3>
        <input type="password" v-model="pwdForm.oldPassword" placeholder="原密码" class="glass-input"/>
        <input type="password" v-model="pwdForm.newPassword" placeholder="新密码 (8-20位)" class="glass-input"/>
        <input type="password" v-model="pwdForm.confirmPassword" placeholder="确认新密码" class="glass-input"/>
        <div class="modal-actions">
          <button @click="showPwdModal=false" class="glass-btn">取消</button>
          <button @click="changePassword" class="gradient-btn">确认修改</button>
        </div>
      </div>
    </div>

    <ImageCropper v-if="showCropper && tempFile" :file="tempFile" @confirm="uploadCropped" @cancel="cancelCrop" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, nextTick, onUnmounted } from 'vue'
import { useStore } from 'vuex'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Edit, User as UserIcon, List, Goods, Lock, Connection, Camera, ChatDotRound, Delete } from '@element-plus/icons-vue'
import http from '../api/http'
import ImageCropper from '../components/ImageCropper.vue'

const store = useStore()
const route = useRoute()
const router = useRouter()
const currentTab = ref('info')
const recordType = ref('lost')
// const claimTab = ref('lost') // Removed
const showPwdModal = ref(false)
const fileInput = ref<HTMLInputElement|null>(null)

const showCropper = ref(false)
const tempFile = ref<File|null>(null)

const user = computed(() => store.state.auth.user || {})
const roleText = computed(() => {
  const map: any = { 'ROLE_STUDENT': '学生', 'ROLE_ADMIN': '管理员', 'ROLE_MANAGER': '负责人' }
  if (user.value.roles && user.value.roles.length > 0) {
    return user.value.roles.map((r: any) => map[r.code] || r.code).join(', ')
  }
  return map[user.value.role] || user.value.role || '未知角色'
})

const form = ref({
  nickname: '',
  department: '',
  phone: '',
  email: '',
  bio: ''
})

const departments = [
  '计算机科学与技术学院',
  '人工智能学院',
  '电子信息工程学院',
  '自动化学院',
  '机械工程学院',
  '材料科学与工程学院',
  '化学化工学院',
  '外国语学院',
  '理学院',
  '经济管理学院',
  '艺术设计学院',
  '马克思主义学院',
  '体育学院',
  '继续教育学院'
]

const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const records = ref<any[]>([])
const myLostClaims = ref<any[]>([])
const friends = ref<any[]>([])

// Chat related
const replyVisible = ref(false)
const replyContent = ref('')
const replyTarget = ref<any>(null)
const chatHistory = ref<any[]>([])
const chatBox = ref<HTMLElement | null>(null)
let pollingTimer: any = null

// Remark related
const remarkDialogVisible = ref(false)
const newRemark = ref('')
const currentFriend = ref<any>(null)

// const myFoundClaims = ref<any[]>([])
// const currentClaims = computed(() => claimTab.value === 'lost' ? myLostClaims.value : myFoundClaims.value)

// Methods
const fetchProfile = async () => {
  try {
    const res = await http.get('/profile')
    const data = res.data
    form.value = { ...data }
    store.commit('auth/setUser', data)
  } catch (e) { console.error(e) }
}

const updateProfile = async () => {
  try {
    // Validate phone
    if (!/^\d{11}$/.test(form.value.phone)) {
      alert('手机号码必须为11位数字')
      return
    }

    // Default bio
    const bioToSave = form.value.bio && form.value.bio.trim() !== '' ? form.value.bio : '无'

    const payload = {
      nickname: form.value.nickname,
      department: form.value.department,
      phone: form.value.phone,
      email: form.value.email,
      bio: bioToSave
    }
    await http.put('/profile', payload)
    alert('保存成功')
    fetchProfile()
  } catch (e: any) { 
    console.error(e)
    alert('保存失败: ' + (e.response?.data?.message || e.message))
  }
}

const triggerUpload = () => fileInput.value?.click()
const handleUpload = (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  tempFile.value = file
  showCropper.value = true
  // Reset input
  if (fileInput.value) fileInput.value.value = ''
}

const cancelCrop = () => {
  showCropper.value = false
  tempFile.value = null
}

const uploadCropped = async (blob: Blob) => {
  const fd = new FormData()
  fd.append('file', blob, 'avatar.jpg')
  try {
    await http.post('/profile/avatar', fd)
    fetchProfile()
    alert('头像上传成功')
    showCropper.value = false
    tempFile.value = null
  } catch (e: any) { 
    console.error(e)
    alert(e.response?.data?.message || '上传失败') 
  }
}

const fetchRecords = async () => {
  records.value = []
  try {
    const url = recordType.value === 'lost' ? '/my/lost' : '/my/found'
    const res = await http.get(url)
    records.value = res.data.records
  } catch (e) { console.error(e) }
}

const fetchMyClaims = async () => {
  try {
    const res = await http.get('/my/claims')
    myLostClaims.value = res.data.lost
    // myFoundClaims.value = res.data.found
  } catch (e) { console.error(e) }
}

const deleteRecord = async (item: any) => {
  const isClaimed = item.status === 'CLAIMED' || item.status === 'RETURNED'
  const msg = isClaimed ? '确定要删除这条记录吗？' : '确定要撤销这条发布吗？'
  if (!confirm(msg)) return
  try {
    const url = recordType.value === 'lost' ? `/my/lost/${item.id}` : `/my/found/${item.id}`
    await http.delete(url)
    fetchRecords()
    alert(isClaimed ? '删除成功' : '已撤销并删除相关图片')
  } catch (e) { alert('操作失败') }
}

const editRecord = (item: any) => {
  if (recordType.value === 'lost') {
    router.push({ path: `/publish/lost/${item.id}` })
  } else {
    router.push({ path: '/publish/found', query: { id: item.id } })
  }
}

const changePassword = async () => {
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) return alert('两次密码不一致')
  try {
    await http.post('/profile/password', pwdForm.value)
    alert('密码修改成功，请重新登录')
    showPwdModal.value = false
    // logout logic
    store.commit('auth/logout')
    router.push('/login')
  } catch (e: any) { alert(e.response?.data?.message || '修改失败') }
}

const unclaimItem = async (id: number) => {
  if (!confirm('确定要取消认领吗？')) return
  try {
    await http.post(`/my/claims/${id}/unclaim`)
    alert('已取消认领')
    fetchMyClaims()
  } catch (e) {
    console.error(e)
    alert('操作失败')
  }
}

const openRemarkDialog = (friend: any, event: Event) => {
  event.stopPropagation()
  currentFriend.value = friend
  newRemark.value = friend.remark || ''
  remarkDialogVisible.value = true
}

const saveRemark = async () => {
  if (!currentFriend.value) return
  try {
    await http.post('/friend/alias', { targetId: currentFriend.value.id, alias: newRemark.value })
    ElMessage.success('备注修改成功')
    remarkDialogVisible.value = false
    
    // Update local state
    const friend = friends.value.find(f => f.id === currentFriend.value.id)
    if (friend) {
      friend.remark = newRemark.value
    }
    
    // Also update chat title if this friend is currently being chatted with
    if (replyTarget.value && replyTarget.value.id === currentFriend.value.id) {
       replyTarget.value.remark = newRemark.value
       replyTarget.value.senderNickname = newRemark.value
    }
  } catch (e: any) {
    console.error(e)
    ElMessage.error(e.response?.data || '修改失败')
  }
}

const deleteFriend = async (friend: any, event: Event) => {
  if (!confirm(`确定要删除好友 "${friend.remark || friend.nickname || friend.username}" 吗？`)) return
  try {
    await http.delete(`/friend/${friend.id}`)
    ElMessage.success('已删除好友')
    friends.value = friends.value.filter(f => f.id !== friend.id)
    
    // If currently chatting with this friend, close chat
    if (replyTarget.value && replyTarget.value.id === friend.id) {
      replyVisible.value = false
      replyTarget.value = null
    }
  } catch (e: any) {
    console.error(e)
    ElMessage.error(e.response?.data || '删除失败')
  }
}

// Chat & Friends logic
const displayList = computed(() => {
    let lastTime = 0
    return chatHistory.value.map((msg, index) => {
        const t = new Date(msg.createdAt).getTime()
        let showTime = false
        if (index === 0) {
            showTime = true
            lastTime = t
        } else {
            if (t - lastTime > 5 * 60 * 1000) {
                showTime = true
                lastTime = t
            }
        }
        return { ...msg, showTime }
    })
})

onUnmounted(() => {
    if (pollingTimer) clearInterval(pollingTimer)
})

watch(replyVisible, (val) => {
    if (val) {
        loadHistory()
        // Polling is handled by global timer
    }
  })

const isMine = (msg: any) => msg.senderId === user.value.id

const getAvatar = (msg: any) => {
    if (isMine(msg)) {
       return user.value.avatar ? 'http://localhost:8080' + user.value.avatar : ''
    }
    return replyTarget.value?.avatar ? 'http://localhost:8080' + replyTarget.value.avatar : ''
}

const getNickname = (msg: any) => {
    if (isMine(msg)) return user.value.nickname || user.value.username
    // Use remark if available
    if (replyTarget.value) {
        return replyTarget.value.remark || replyTarget.value.nickname || replyTarget.value.username || '?'
    }
    return '?'
}

const formatTime = (t: string) => new Date(t).toLocaleString()

const openChat = (friend: any) => {
    replyTarget.value = friend
    // Ensure nickname is available for title
    replyTarget.value.senderNickname = friend.remark || friend.nickname || friend.username 
    replyVisible.value = true
    replyContent.value = ''
}

const loadHistory = async (isPolling = false) => {
    if (!replyTarget.value) return
    try {
        const targetId = replyTarget.value.id
        
        // Mark messages as read since chat is open
        await http.put(`/chat/read-from/${targetId}`)
        // Update local friend unread count immediately
        const friend = friends.value.find(f => f.id === targetId)
        if (friend) {
            friend.unreadCount = 0
        }

        const { data } = await http.get('/chat/history', { params: { targetId } })
        
        const oldLen = chatHistory.value.length
        const newLen = (data || []).length
        chatHistory.value = data || []
        
        if (!isPolling || newLen > oldLen) {
            scrollToBottom()
        }
    } catch(e) { console.error(e) }
}

const scrollToBottom = () => {
    nextTick(() => {
        if (chatBox.value) {
            chatBox.value.scrollTop = chatBox.value.scrollHeight
        }
    })
}

const sendReply = async () => {
    if (!replyContent.value.trim()) return
    try {
        const content = replyContent.value
        await http.post('/chat/send', { 
            receiverId: replyTarget.value.id, 
            content: content 
        })
        chatHistory.value.push({
            id: Date.now(),
            senderId: user.value.id,
            receiverId: replyTarget.value.id,
            content: content,
            createdAt: new Date().toISOString(),
            isRead: false
        })
        replyContent.value = ''
        scrollToBottom()
    } catch (e: any) {
        console.error(e)
        alert('发送失败')
    }
}

const fetchFriends = async () => {
  try {
    const res = await http.get('/friend/list')
    friends.value = res.data
  } catch (e) { console.error(e) }
}

const statusText = (s: string) => {
  const map: any = { 'PENDING':'待审核','APPROVED':'待认领','CLAIMED':'已认领','发布':'待认领','RETURNED':'已退回' }
  if (recordType.value === 'lost') {
    map['APPROVED'] = '待找回'
    map['CLAIMED'] = '已找回'
    map['发布'] = '待找回'
  }
  return map[s] || s
}
const claimStatusText = (s: number) => {
  if (s === 1) return '审核中'
  if (s === 2) return '已认领'
  if (s === -1) return '已驳回'
  return '未知'
}
const claimStatusClass = (s: number) => {
  if (s === 1) return 'PENDING' 
  if (s === 2) return 'CLAIMED'
  if (s === -1) return 'REJECTED'
  return ''
}
const formatDate = (ts: string) => new Date(ts).toLocaleString()
const typeText = (t: string) => ({AUDIT:'系统', MATCH:'匹配', CLAIM:'认领', NOTICE:'公告'} as any)[t] || '其他'

// Watchers
watch(currentTab, (val) => {
  if (val === 'records') fetchRecords()
  if (val === 'claims') fetchMyClaims()
  if (val === 'friends') fetchFriends()
})
watch(recordType, () => fetchRecords())

watch(() => route.query.tab, (val) => {
  if (val && typeof val === 'string') {
    currentTab.value = val === 'mypost' ? 'records' : val
    
    // Handle recordType for records tab
    if (val === 'mypost' || val === 'records') {
      if (route.query.type && (route.query.type === 'found' || route.query.type === 'lost')) {
        recordType.value = route.query.type as string
      }
    }
  }
}, { immediate: true })

// Sync state to URL to support back button navigation
watch([currentTab, recordType], ([newTab, newType]) => {
  const query: any = { ...route.query, tab: newTab }
  if (newTab === 'records') {
    query.type = newType
  } else {
    delete query.type
  }
  
  // Only replace if query is different to avoid redundant navigation
  if (route.query.tab !== query.tab || route.query.type !== query.type) {
    router.replace({ query })
  }
})

onMounted(() => {
    fetchProfile()

    // Start polling for messages
    pollingTimer = setInterval(async () => {
      if (replyVisible.value && replyTarget.value) {
        // If chat is open, refresh chat messages
        await loadHistory(true)
      }

      // Also refresh friend list to update unread counts
      // We only do this if "My Friends" tab is active
      if (currentTab.value === 'friends') {
        fetchFriends()
      }
    }, 2000)
  })
</script>

<style scoped>
.profile-container {
  display: flex;
  gap: 24px;
  max-width: 1200px;
  margin: 40px auto;
  padding: 0 20px;
  min-height: 600px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.profile-container * {
  box-sizing: border-box;
}

/* Sidebar */
.sidebar-card {
  width: 280px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 32px 20px;
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.18);
  height: fit-content;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.user-profile {
  text-align: center;
  margin-bottom: 40px;
  width: 100%;
}

.avatar-wrapper {
  width: 100px;
  height: 100px;
  margin: 0 auto 16px;
  position: relative;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
  border: 3px solid rgba(255, 255, 255, 0.8);
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.avatar-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  opacity: 0;
  transition: opacity 0.3s;
  cursor: pointer;
  font-size: 12px;
  gap: 4px;
}

.avatar-wrapper:hover .avatar-mask {
  opacity: 1;
}

.avatar-wrapper:hover .avatar {
  transform: scale(1.1);
}

.username {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 700;
  color: #2c3e50;
  background: linear-gradient(135deg, #2c3e50 0%, #3498db 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.role-badge {
  display: inline-block;
  padding: 4px 12px;
  background: linear-gradient(135deg, #e0c3fc 0%, #8ec5fc 100%);
  color: #5e35b1;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

/* Navigation Menu */
.nav-menu {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-radius: 16px;
  cursor: pointer;
  color: #64748b;
  font-weight: 500;
  transition: all 0.3s ease;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.5);
  transform: translateX(5px);
  color: #667eea;
}

.menu-item.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(118, 75, 162, 0.3);
}

/* Content Area */
.content-card {
  flex: 1;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.18);
  min-width: 0; /* Prevent flex overflow */
}

.section-header {
  margin-bottom: 32px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  padding-bottom: 16px;
}

.section-header h2 {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 8px;
  color: #1a202c;
}

.subtitle {
  color: #718096;
  font-size: 14px;
  margin: 0;
}

/* Forms */
.glass-form {
  max-width: 600px;
}

.form-row {
  display: flex;
  gap: 20px;
}

.form-group {
  margin-bottom: 24px;
  flex: 1;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #4a5568;
  font-weight: 500;
  font-size: 14px;
}

.glass-input {
  width: 100%;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(203, 213, 224, 0.6);
  border-radius: 12px;
  font-size: 14px;
  transition: all 0.3s;
  color: #2d3748;
}

.glass-input:focus {
  outline: none;
  background: white;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.glass-input.disabled {
  background: rgba(247, 250, 252, 0.5);
  color: #a0aec0;
  cursor: not-allowed;
}

.glass-input.textarea {
  resize: vertical;
  min-height: 100px;
}

.select-wrapper {
  position: relative;
}

.form-actions {
  margin-top: 32px;
}

.gradient-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 12px 32px;
  border-radius: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(118, 75, 162, 0.3);
}

.gradient-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(118, 75, 162, 0.4);
}

.gradient-btn.outline {
  background: transparent;
  border: 2px solid #667eea;
  color: #667eea;
  box-shadow: none;
}

.gradient-btn.outline:hover {
  background: rgba(102, 126, 234, 0.1);
  transform: translateY(-2px);
}

.gradient-btn.sm {
  padding: 8px 20px;
  font-size: 13px;
}

/* Tabs & Records */
.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.sub-tabs {
  display: flex;
  background: rgba(241, 245, 249, 0.5);
  padding: 4px;
  border-radius: 12px;
}

.sub-tabs span {
  padding: 8px 24px;
  border-radius: 8px;
  cursor: pointer;
  color: #64748b;
  font-weight: 500;
  transition: all 0.3s;
}

.sub-tabs span.active {
  background: white;
  color: #667eea;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.records-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.record-card {
  background: rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.4);
  transition: all 0.3s;
}

.record-card.clickable {
  cursor: pointer;
}

.record-card:hover {
  transform: translateY(-4px);
  background: white;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.05);
}

.card-thumb {
  height: 160px;
  position: relative;
}

.card-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
}

.status-badge.PENDING { color: #f59e0b; }
.status-badge.APPROVED { color: #10b981; }
.status-badge.CLAIMED { color: #3b82f6; }
.status-badge.REJECTED { color: #ef4444; }
.status-badge.RETURNED { color: #6366f1; }

.card-content {
  padding: 16px;
}

.card-content h4 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #1a202c;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-content .time {
  font-size: 12px;
  color: #a0aec0;
  margin: 0 0 16px;
}
.card-content .desc {
  font-size: 13px;
  color: #718096;
  margin: 0 0 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.action-btn {
  border: none;
  background: transparent;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn.edit { color: #3b82f6; background: #eff6ff; }
.action-btn.delete { color: #ef4444; background: #fef2f2; }
.action-btn.view { color: #8b5cf6; background: #f5f3ff; }

.action-btn:hover { opacity: 0.8; }

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px;
  color: #cbd5e0;
}

.empty-state p {
  margin-top: 16px;
  font-size: 16px;
  color: #a0aec0;
}

/* Security */
.security-card {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 32px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.02);
}

.sec-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #edf2f7;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4a5568;
  font-size: 24px;
}

.sec-info { flex: 1; }
.sec-info h4 { margin: 0 0 4px; font-size: 16px; }
.sec-info p { margin: 0; color: #718096; font-size: 14px; }

/* Friends */
.friends-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}

.friend-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  cursor: pointer;
  transition: all 0.3s;
}

.friend-card:hover {
  background: white;
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.05);
}

.friend-avatar {
  position: relative;
  width: 48px;
  height: 48px;
}

.friend-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.unread-dot {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ef4444;
  color: white;
  font-size: 10px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid white;
}

.friend-info { flex: 1; min-width: 0; }
.friend-info h4 {
  margin: 0 0 4px;
  font-size: 15px;
  color: #2d3748;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: flex;
  align-items: center;
}

.edit-icon {
  margin-left: 6px;
  color: #cbd5e0;
  font-size: 12px;
  transition: color 0.2s;
}
.friend-card:hover .edit-icon { color: #667eea; }

.friend-info p {
  margin: 0;
  font-size: 12px;
  color: #a0aec0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.icon-btn.chat {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: #ebf4ff;
  color: #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.icon-btn.chat:hover {
  background: #3b82f6;
  color: white;
}

.icon-btn.delete-friend {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: #fef2f2;
  color: #ef4444;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.icon-btn.delete-friend:hover {
  background: #ef4444;
  color: white;
}

.friend-actions-right {
  display: flex;
  gap: 6px;
  align-items: center;
}

/* Animations */
.fade-in {
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Modal Styling */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.glass-modal {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  padding: 32px;
  border-radius: 24px;
  width: 400px;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.glass-modal h3 {
  margin-top: 0;
  margin-bottom: 24px;
  color: #2d3748;
  text-align: center;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
}

.glass-btn {
  background: transparent;
  border: 1px solid #cbd5e0;
  color: #4a5568;
  padding: 12px 24px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.glass-btn:hover {
  background: #f7fafc;
  border-color: #a0aec0;
}

/* Chat Styles */
.chat-container {
  height: 400px;
  overflow-y: auto;
  padding: 20px;
  background: rgba(247, 250, 252, 0.8);
  border-radius: 16px;
  margin-bottom: 20px;
}

.chat-msg {
  display: flex;
  margin-bottom: 20px;
  align-items: flex-end;
}

.chat-msg.mine {
  flex-direction: row-reverse;
}

.chat-avatar {
  margin: 0 12px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  border: 2px solid white;
}

.chat-content {
  max-width: 70%;
}

.chat-bubble {
  background: white;
  padding: 12px 16px;
  border-radius: 16px 16px 16px 4px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  line-height: 1.5;
  color: #2d3748;
}

.chat-msg.mine .chat-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 16px 16px 4px 16px;
}

.chat-time {
  text-align: center;
  color: #a0aec0;
  font-size: 12px;
  margin: 16px 0;
}

.chat-input-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-textarea :deep(.el-textarea__inner) {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 12px;
  resize: none;
  background: rgba(255, 255, 255, 0.8);
}

.chat-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>

<style>
/* Global overrides for this page */
.glass-dialog {
  background: rgba(255, 255, 255, 0.95) !important;
  backdrop-filter: blur(20px) !important;
  border-radius: 24px !important;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.1) !important;
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
  overflow: hidden;
}

.glass-dialog .el-dialog__header {
  margin-right: 0;
  border-bottom: 1px solid rgba(0,0,0,0.05);
  padding: 20px 24px;
}

.glass-dialog .el-dialog__body {
  padding: 24px;
}

.glass-dialog .el-dialog__footer {
  padding: 20px 24px;
  border-top: 1px solid rgba(0,0,0,0.05);
}

.glass-input-el .el-input__wrapper {
  box-shadow: none !important;
  background: rgba(247, 250, 252, 0.8) !important;
  border: 1px solid rgba(203, 213, 224, 0.6) !important;
  border-radius: 12px !important;
  padding: 8px 12px !important;
}

.glass-input-el .el-input__wrapper.is-focus {
  border-color: #667eea !important;
  background: white !important;
}
</style>

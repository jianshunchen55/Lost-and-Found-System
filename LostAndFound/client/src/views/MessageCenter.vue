<template>
  <div class="message-page">
    <div class="header-section">
      <div class="page-title">
        <el-icon class="title-icon"><Bell /></el-icon>
        <h2>消息中心</h2>
      </div>
      
      <div class="actions-bar">
        <el-select v-model="type" placeholder="筛选类型" class="type-select" popper-class="glass-dropdown" clearable :disabled="tab==='chat'">
          <el-option label="全部类型" value="" />
          <el-option label="系统通知" value="AUDIT" />
          <el-option label="物品匹配" value="MATCH" />
          <el-option label="认领消息" value="CLAIM" />
          <el-option label="公告通知" value="NOTICE" />
        </el-select>
        <el-button @click="markAllRead" type="primary" class="mark-read-btn">
          <el-icon style="margin-right: 6px"><Check /></el-icon> 全部已读
        </el-button>
      </div>
    </div>

    <div class="content-card">
      <el-tabs v-model="tab" class="custom-tabs">
        <el-tab-pane label="未读消息" name="unread">
          <template #label>
            <span class="tab-label">
              <el-icon><Message /></el-icon> 未读消息
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="已读历史" name="read">
          <template #label>
            <span class="tab-label">
              <el-icon><Reading /></el-icon> 已读历史
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="私信聊天" name="chat">
          <template #label>
            <span class="tab-label">
              <el-icon><ChatDotRound /></el-icon> 私信聊天
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <el-table :data="list" style="width:100%" class="custom-table">
        <el-table-column label="类型" width="140">
          <template #default="{row}">
            <div class="type-tag" :class="row.type === 'FRIEND_REQUEST' ? 'friend' : (tab==='chat' ? 'chat' : tagType(row.type))">
              <span v-if="tab==='chat'">私信</span>
              <span v-else>{{ typeText(row.type) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="标题/发送者">
           <template #default="{row}">
              <div class="message-title">
                <span v-if="tab==='chat'" class="sender-name">{{ row.senderNickname }}</span>
                <span v-else class="title-text">{{ row.title }}</span>
              </div>
           </template>
        </el-table-column>
        <el-table-column prop="content" label="内容摘要">
          <template #default="{row}">
             <div class="message-content" :class="{ 'unread': !row.isRead }">
               {{ row.content }}
               <span class="unread-dot" v-if="!row.isRead"></span>
             </div>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{row}">
            <span class="time-text">{{ new Date(row.createdAt).toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{row}">
            <div class="action-buttons">
              <template v-if="row.type === 'FRIEND_REQUEST'">
                 <el-button size="small" type="success" plain @click="handleFriend(row, true)">通过</el-button>
                 <el-button size="small" type="danger" plain @click="handleFriend(row, false)">拒绝</el-button>
              </template>
              <template v-else>
                <template v-if="tab==='chat'">
                    <el-button size="small" type="primary" link @click="openReply(row)">回复</el-button>
                    <el-button size="small" link @click="read(row)" :disabled="row.isRead">已读</el-button>
                    <el-button size="small" type="danger" link @click="remove(row)">删除</el-button>
                </template>
                <template v-else>
                    <el-button v-if="row.relatedId" size="small" type="primary" link @click="viewDetail(row)">查看</el-button>
                    <el-button size="small" link @click="read(row)" :disabled="tab==='read'">已读</el-button>
                    <el-button size="small" type="danger" link @click="remove(row)">删除</el-button>
                </template>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="replyVisible" :title="replyTarget?.senderNickname || '聊天'" width="600px" custom-class="glass-dialog" destroy-on-close>
      <div class="chat-wrapper">
        <div class="chat-container" ref="chatBox">
          <div v-for="(msg, index) in displayList" :key="msg.id">
            <div v-if="msg.showTime" class="chat-time"><span>{{ formatTime(msg.createdAt) }}</span></div>
            <div class="chat-msg" :class="{ mine: isMine(msg) }">
              <el-avatar class="chat-avatar" :src="getAvatar(msg)" shape="square" :size="40">{{ getNickname(msg)[0]?.toUpperCase() }}</el-avatar>
              <div class="chat-content">
                <div class="chat-bubble">{{ msg.content }}</div>
              </div>
            </div>
          </div>
        </div>
        <div class="chat-input-area">
          <el-input v-model="replyContent" type="textarea" :rows="3" placeholder="输入消息，按 Enter 发送..." @keydown.enter.prevent="sendReply" resize="none" class="chat-input" />
          <div class="chat-actions">
            <el-button @click="replyVisible = false" round>关闭</el-button>
            <el-button type="primary" @click="sendReply" round class="send-btn">发送 <el-icon class="el-icon--right"><Promotion /></el-icon></el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="acceptVisible" title="通过好友请求" width="400px" custom-class="glass-dialog">
      <el-form class="accept-form">
        <el-form-item label="设置备注">
          <el-input v-model="acceptRemark" placeholder="默认为对方用户名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="acceptVisible = false" round>取消</el-button>
          <el-button type="primary" @click="confirmAccept" round class="confirm-btn">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
  </template>
  <script setup lang="ts">
  import { ref, watch, nextTick, computed, onUnmounted, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { useStore } from 'vuex'
  import { ElMessage } from 'element-plus'
  import { Bell, Check, Message, Reading, ChatDotRound, Promotion } from '@element-plus/icons-vue'
  import http from '../api/http'
  
  const store = useStore()
  const router = useRouter()
  const tab = ref('unread')
  const type = ref<string | ''>('')
  const list = ref<any[]>([])
  
  const replyVisible = ref(false)
  const replyContent = ref('')
  const replyTarget = ref<any>(null)
  const chatHistory = ref<any[]>([])
  const chatBox = ref<HTMLElement | null>(null)
  
  const acceptVisible = ref(false)
  const acceptRemark = ref('')
  const currentRequest = ref<any>(null)

  const currentUser = computed(() => store.state.auth.user || {})

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

  let pollingTimer: any = null
  let listPollingTimer: any = null

  onUnmounted(() => {
    if (pollingTimer) clearInterval(pollingTimer)
    if (listPollingTimer) clearInterval(listPollingTimer)
  })

  onMounted(() => {
      // Poll for new messages list every 3 seconds
      listPollingTimer = setInterval(() => {
          if (!replyVisible.value) {
              load()
          }
      }, 3000)
  })

  watch(replyVisible, (val) => {
    if (val) {
      loadHistory()
      // 开启轮询，实现实时同步
      pollingTimer = setInterval(() => loadHistory(true), 2000)
    } else {
      if (pollingTimer) clearInterval(pollingTimer)
    }
  })

  // WebSocket removed to restore stability and use polling instead


  function isMine(msg: any) {
    return msg.senderId === currentUser.value.id
  }

  function getAvatar(msg: any) {
    if (isMine(msg)) {
       return currentUser.value.avatar ? 'http://localhost:8080' + currentUser.value.avatar : ''
    }
    // For the other person, we might need to rely on what we have. 
    // If it's from history, it might have senderAvatar.
    // If it's the person we are replying to, we have replyTarget.
    return msg.senderAvatar ? 'http://localhost:8080' + msg.senderAvatar : 
           (replyTarget.value?.senderAvatar ? 'http://localhost:8080' + replyTarget.value.senderAvatar : '')
  }

  function getNickname(msg: any) {
    if (isMine(msg)) return currentUser.value.nickname || currentUser.value.username
    return msg.senderNickname || replyTarget.value?.senderNickname || '?'
  }

  function formatTime(t: string) {
    return new Date(t).toLocaleString()
  }

  async function openReply(row: any) {
      replyTarget.value = row
      replyVisible.value = true
      replyContent.value = ''
      
      try {
          await http.put(`/chat/read-from/${row.senderId}`)
          load()
      } catch(e) {
          console.error(e)
      }
  }

  async function loadHistory(isPolling = false) {
      if (!replyTarget.value) return
      try {
          // targetId is the sender of the message we are replying to
          const targetId = replyTarget.value.senderId
          
          // Mark as read (keep trying while chat is open)
          await http.put(`/chat/read-from/${targetId}`)

          const { data } = await http.get('/chat/history', { params: { targetId } })
          
          const oldLen = chatHistory.value.length
          const newLen = (data || []).length
          chatHistory.value = data || []
          
          if (!isPolling || newLen > oldLen) {
              scrollToBottom()
          }
      } catch(e) {
          console.error(e)
      }
  }

  function scrollToBottom() {
      nextTick(() => {
          if (chatBox.value) {
              chatBox.value.scrollTop = chatBox.value.scrollHeight
          }
      })
  }

  async function sendReply() {
      if (!replyContent.value.trim()) return
      try {
          const content = replyContent.value
          await http.post('/chat/send', { 
              receiverId: replyTarget.value.senderId, 
              content: content 
          })
          // Optimistically append
          chatHistory.value.push({
              id: Date.now(), // temp id
              senderId: currentUser.value.id,
              receiverId: replyTarget.value.senderId,
              content: content,
              createdAt: new Date().toISOString(),
              isRead: false
          })
          replyContent.value = ''
          scrollToBottom()
      } catch (e) {
          console.error(e)
          ElMessage.error('回复失败')
      }
  }

  function typeText(t:string) { return ({AUDIT:'系统', MATCH:'匹配', CLAIM:'认领', NOTICE:'公告', RECOMMENDATION: '推荐认领', FRIEND_REQUEST: '好友申请'} as any)[t] || '其他' }
  function tagType(t:string) { return ({AUDIT:'info', MATCH:'warning', CLAIM:'success', NOTICE:'primary', RECOMMENDATION: 'danger', FRIEND_REQUEST: 'success'} as any)[t] || 'default' }
  
  async function handleFriend(row: any, accept: boolean) {
      if (accept) {
          currentRequest.value = row
          acceptRemark.value = ''
          acceptVisible.value = true
      } else {
          if (!confirm('确定拒绝该好友请求吗？')) return
          try {
              await http.post('/friend/reject', { friendshipId: row.relatedId })
              ElMessage.success('已拒绝')
              await http.delete(`/message/${row.id}`)
              load()
          } catch(e) {
              console.error(e)
              ElMessage.error((e as any).response?.data || '操作失败')
          }
      }
  }

  async function confirmAccept() {
      if (!currentRequest.value) return
      try {
          await http.post('/friend/accept', { 
              friendshipId: currentRequest.value.relatedId,
              remark: acceptRemark.value
          })
          ElMessage.success('已添加好友')
          acceptVisible.value = false
          await http.delete(`/message/${currentRequest.value.id}`)
          load()
      } catch(e) {
          console.error(e)
          ElMessage.error((e as any).response?.data || '操作失败')
      }
  }

  async function load() {
    if (tab.value === 'chat') {
        const { data } = await http.get('/chat/received', { params: { page: 0, size: 100 } })
        list.value = data.records || []
    } else {
        const isRead = tab.value === 'read' ? true : false
        const { data } = await http.get('/message', { params: { page: 0, size: 100, type: type.value || undefined, isRead } })
        list.value = data.records || []
    }
  }
  watch(tab, load, { immediate: true })
  watch(type, load)
  async function read(row:any) { 
      if (tab.value === 'chat') {
          await http.put(`/chat/read/${row.id}`)
      } else {
          await http.put(`/message/${row.id}/read`)
      }
      load() 
  }
  async function remove(row:any) { 
      if (tab.value === 'chat') {
          if (!confirm('确定要删除这条私信吗？')) return
          await http.delete(`/chat/${row.id}`)
      } else {
          await http.delete(`/message/${row.id}`)
      }
      load() 
  }
  async function markAllRead() { 
      if (tab.value === 'chat') {
          await http.put('/chat/read-all')
      } else {
          await http.put('/message/read-all')
      }
      load() 
  }
  function viewDetail(row: any) {
    if (row.type === 'RECOMMENDATION' && row.relatedId) {
       router.push(`/detail/found/${row.relatedId}`)
    }
  }
  </script>
<style scoped>
.message-page {
  max-width: 1200px;
  margin: 40px auto;
  padding: 0 24px;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title h2 {
  font-size: 28px;
  font-weight: 700;
  color: #2c3e50;
  margin: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.title-icon {
  font-size: 32px;
  color: #764ba2;
  background: rgba(118, 75, 162, 0.1);
  padding: 8px;
  border-radius: 12px;
}

.actions-bar {
  display: flex;
  gap: 16px;
  align-items: center;
}

.mark-read-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 12px;
  padding: 10px 20px;
  height: 40px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(118, 75, 162, 0.3);
  transition: all 0.3s ease;
}

.mark-read-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(118, 75, 162, 0.4);
}

.content-card {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.18);
  min-height: 600px;
}

/* Custom Tabs */
:deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: rgba(0, 0, 0, 0.05);
}

:deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 500;
  color: #606266;
  padding: 0 24px !important;
  height: 50px;
  line-height: 50px;
  transition: all 0.3s;
}

:deep(.el-tabs__item.is-active) {
  color: #764ba2;
  font-weight: 600;
}

:deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #667eea, #764ba2);
  height: 3px;
  border-radius: 3px;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* Custom Table */
.custom-table {
  background: transparent !important;
  margin-top: 20px;
  --el-table-border-color: rgba(0, 0, 0, 0.05);
  --el-table-header-bg-color: rgba(245, 247, 250, 0.5);
  --el-table-row-hover-bg-color: rgba(118, 75, 162, 0.05) !important;
}

:deep(.el-table tr), :deep(.el-table th.el-table__cell) {
  background: transparent !important;
}

:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(0, 0, 0, 0.03) !important;
  padding: 16px 0;
}

:deep(.el-table th.el-table__cell) {
  font-weight: 600;
  color: #2c3e50;
  padding: 16px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05) !important;
}

/* Tags & Content */
.type-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
}

.type-tag.info { background: rgba(144, 147, 153, 0.1); color: #909399; }
.type-tag.warning { background: rgba(230, 162, 60, 0.1); color: #e6a23c; }
.type-tag.success { background: rgba(103, 194, 58, 0.1); color: #67c23a; }
.type-tag.primary { background: rgba(64, 158, 255, 0.1); color: #409eff; }
.type-tag.danger { background: rgba(245, 108, 108, 0.1); color: #f56c6c; }
.type-tag.chat { background: rgba(118, 75, 162, 0.1); color: #764ba2; }
.type-tag.friend { background: rgba(103, 194, 58, 0.15); color: #67c23a; }

.message-title {
  font-weight: 500;
  color: #2c3e50;
}

.sender-name {
  font-weight: 600;
  color: #764ba2;
}

.message-content {
  color: #606266;
  position: relative;
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-content.unread {
  font-weight: 600;
  color: #303133;
}

.unread-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  background: #f56c6c;
  border-radius: 50%;
  margin-left: 6px;
  vertical-align: middle;
}

.time-text {
  color: #909399;
  font-size: 13px;
}

/* Chat Styles */
.chat-wrapper {
  background: #f8f9fa;
  border-radius: 16px;
  overflow: hidden;
}

.chat-container {
  height: 450px;
  overflow-y: auto;
  padding: 20px;
  background: rgba(255, 255, 255, 0.5);
}

.chat-msg {
  display: flex;
  margin-bottom: 20px;
  align-items: flex-start;
}

.chat-msg.mine {
  flex-direction: row-reverse;
}

.chat-avatar {
  margin: 0 12px;
  border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
}

.chat-content {
  max-width: 70%;
}

.chat-bubble {
  background: white;
  padding: 12px 18px;
  border-radius: 16px;
  border-top-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  color: #333;
  line-height: 1.6;
  position: relative;
  font-size: 14px;
}

.chat-msg.mine .chat-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 16px;
  border-top-right-radius: 4px;
  box-shadow: 0 4px 12px rgba(118, 75, 162, 0.2);
}

.chat-time {
  text-align: center;
  margin: 16px 0;
}

.chat-time span {
  background: rgba(0,0,0,0.05);
  color: #999;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.chat-input-area {
  padding: 16px;
  background: white;
  border-top: 1px solid rgba(0,0,0,0.05);
}

.chat-input :deep(.el-textarea__inner) {
  border: none;
  background: #f5f7fa;
  border-radius: 12px;
  padding: 12px;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.02);
}

.chat-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
  gap: 12px;
}

.send-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  padding: 8px 24px;
}

.type-select {
  width: 140px;
}

.type-select :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(10px);
  box-shadow: none;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 12px;
  padding: 4px 12px;
  transition: all 0.3s;
}

.type-select :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #764ba2 inset !important;
  background: rgba(255, 255, 255, 0.9);
}
</style>

<style>
/* Global styles for dynamic components */
.glass-dialog {
  background: rgba(255, 255, 255, 0.9) !important;
  backdrop-filter: blur(24px) !important;
  border-radius: 24px !important;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.1) !important;
  border: 1px solid rgba(255, 255, 255, 0.4) !important;
  overflow: hidden;
}

.glass-dialog .el-dialog__header {
  padding: 24px;
  margin: 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.glass-dialog .el-dialog__title {
  font-weight: 700;
  color: #2c3e50;
  font-size: 20px;
}

.glass-dialog .el-dialog__body {
  padding: 0 !important;
}

.glass-dialog .el-dialog__footer {
  padding: 20px 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.glass-dropdown.el-popper {
  background: rgba(255, 255, 255, 0.95) !important;
  backdrop-filter: blur(20px) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  border-radius: 16px !important;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1) !important;
}

.glass-dropdown .el-select-dropdown__item {
  border-radius: 8px;
  margin: 4px 8px;
  height: 40px;
  line-height: 40px;
}

.glass-dropdown .el-select-dropdown__item.selected {
  background: rgba(118, 75, 162, 0.1);
  color: #764ba2;
  font-weight: 600;
}

.glass-dropdown .el-select-dropdown__item:hover {
  background: rgba(0, 0, 0, 0.03);
}

.glass-dropdown .el-popper__arrow::before {
  background: rgba(255, 255, 255, 0.95) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
}
</style>
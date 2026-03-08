<template>
  <div>
    <el-button type="primary" @click="load">刷新</el-button>
    <el-table :data="list" style="width:100%;margin-top:12px">
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="role" label="角色">
         <template #default="{row}">
            <el-tag v-if="row.role === 'ROLE_ADMIN'" type="danger">管理员</el-tag>
            <el-tag v-else-if="row.role === 'ROLE_MANAGER'" type="warning">负责人</el-tag>
            <el-tag v-else>学生</el-tag>
         </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{row}">
          <el-select v-model="row.role" style="width:140px" @change="updateRole(row)">
            <el-option label="学生" value="ROLE_STUDENT" />
            <el-option label="管理员" value="ROLE_ADMIN" />
            <el-option label="负责人" value="ROLE_MANAGER" />
          </el-select>
          <el-button size="small" type="danger" style="margin-left: 10px" @click="openDeleteDialog(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="deleteDialogVisible" title="警告" width="400px">
      <div style="padding: 20px 0;">
        <p style="color: red; font-weight: bold; margin-bottom: 10px;">您确定要删除此账号吗？此操作不可恢复！</p>
        <p>用户名: {{ userToDelete?.username }}</p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="deleteDialogVisible = false">取消</el-button>
          <el-button type="danger" :disabled="deleteButtonDisabled" @click="confirmDelete">
            确定删除 {{ deleteTimer > 0 ? `(${deleteTimer}s)` : '' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import http from '../api/http'
import { ElMessage } from 'element-plus'

const list = ref<any[]>([])
const deleteDialogVisible = ref(false)
const userToDelete = ref<any>(null)
const deleteTimer = ref(5)
const deleteButtonDisabled = ref(true)
let timerInterval: any = null

async function load() {
  const { data } = await http.get('/user/list')
  list.value = data.map((u: any) => ({
    ...u,
    // Flatten the roles array to a single string for the select binding
    role: u.roles && u.roles.length > 0 ? u.roles[0].code : 'ROLE_STUDENT'
  }))
}

async function updateRole(row:any) { 
  try {
    await http.put('/user/update', { id: row.id, role: row.role }) 
    ElMessage.success('角色更新成功')
  } catch(e) {
    ElMessage.error('更新失败')
    load() // Revert on failure
  }
}

function openDeleteDialog(row: any) {
  userToDelete.value = row
  deleteDialogVisible.value = true
  deleteTimer.value = 5
  deleteButtonDisabled.value = true
  
  if (timerInterval) clearInterval(timerInterval)
  
  timerInterval = setInterval(() => {
    deleteTimer.value--
    if (deleteTimer.value <= 0) {
      deleteButtonDisabled.value = false
      if (timerInterval) clearInterval(timerInterval)
    }
  }, 1000)
}

async function confirmDelete() {
  if (!userToDelete.value) return
  
  try {
    await http.delete('/user/delete', { data: { id: userToDelete.value.id } })
    ElMessage.success('删除成功')
    deleteDialogVisible.value = false
    load()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval)
})

load()
</script>

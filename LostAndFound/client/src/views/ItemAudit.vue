<template>
  <div class="audit-container">
    <h2>管理审核</h2>
    <div style="margin-bottom: 20px;">
       <el-radio-group v-model="auditFilter" @change="fetchItems">
          <el-radio-button :label="0">待审核</el-radio-button>
          <el-radio-button :label="1">已通过</el-radio-button>
          <el-radio-button :label="2">已驳回</el-radio-button>
          <el-radio-button :label="-1">全部</el-radio-button>
       </el-radio-group>
    </div>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="失物发布审核" name="lost">
        <el-table :data="lostItems" border stripe>
          <el-table-column prop="title" label="标题" />
          <el-table-column label="分类" width="100">
            <template #default="scope">
               {{ formatCategory(scope.row.category) }}
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="发布时间" width="180">
            <template #default="scope">{{ new Date(scope.row.createdAt).toLocaleString() }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
             <template #default="scope">
                <el-tag v-if="scope.row.auditStatus===0">待审核</el-tag>
                <el-tag v-else-if="scope.row.auditStatus===1" type="success">通过</el-tag>
                <el-tag v-else type="danger">驳回</el-tag>
             </template>
          </el-table-column>
          <el-table-column prop="auditReply" label="审核回复" show-overflow-tooltip />
          <el-table-column label="操作" width="220">
            <template #default="scope">
              <el-button v-if="scope.row.auditStatus===0" type="success" size="small" @click="openAuditDialog(scope.row, 1, 'item', 'lost')">通过</el-button>
              <el-button v-if="scope.row.auditStatus===0" type="danger" size="small" @click="openAuditDialog(scope.row, 2, 'item', 'lost')">退回</el-button>
              <el-button v-if="scope.row.auditStatus===2" type="warning" size="small" @click="handleAudit(scope.row, 'lost', 0)">撤销驳回</el-button>
              <el-button type="danger" size="small" @click="handleDelete(scope.row, 'lost')">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      
      <el-tab-pane label="拾物发布审核" name="found">
        <el-table :data="foundItems" border stripe>
          <el-table-column prop="title" label="标题" />
          <el-table-column label="分类" width="100">
            <template #default="scope">
               {{ formatCategory(scope.row.category) }}
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="发布时间" width="180">
            <template #default="scope">{{ new Date(scope.row.createdAt).toLocaleString() }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
             <template #default="scope">
                <el-tag v-if="scope.row.auditStatus===0">待审核</el-tag>
                <el-tag v-else-if="scope.row.auditStatus===1" type="success">通过</el-tag>
                <el-tag v-else type="danger">驳回</el-tag>
             </template>
          </el-table-column>
          <el-table-column prop="auditReply" label="审核回复" show-overflow-tooltip />
          <el-table-column label="操作" width="220">
            <template #default="scope">
              <el-button v-if="scope.row.auditStatus===0" type="success" size="small" @click="openAuditDialog(scope.row, 1, 'item', 'found')">通过</el-button>
              <el-button v-if="scope.row.auditStatus===0" type="danger" size="small" @click="openAuditDialog(scope.row, 2, 'item', 'found')">退回</el-button>
              <el-button v-if="scope.row.auditStatus===2" type="warning" size="small" @click="handleAudit(scope.row, 'found', 0)">撤销驳回</el-button>
              <el-button type="danger" size="small" @click="handleDelete(scope.row, 'found')">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="失物认领审核" name="lost-claim">
        <el-table :data="allClaims" border stripe>
          <el-table-column prop="title" label="物品" />
          <el-table-column prop="claimUserNickname" label="申请人昵称" width="120" />
          <el-table-column prop="claimName" label="申请人姓名" width="100" />
          <el-table-column prop="claimPhone" label="联系电话" width="120" />
          <el-table-column prop="claimTime" label="申请时间" width="180">
            <template #default="scope">{{ new Date(scope.row.claimTime).toLocaleString() }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
             <template #default="scope">
                <el-tag v-if="scope.row.claimStatus===1">待审核</el-tag>
                <el-tag v-else-if="scope.row.claimStatus===2" type="success">通过</el-tag>
                <el-tag v-else type="danger">驳回</el-tag>
             </template>
          </el-table-column>
          <el-table-column prop="claimAuditReply" label="审核回复" show-overflow-tooltip />
          <el-table-column label="操作" width="150">
            <template #default="scope">
              <el-button v-if="scope.row.claimStatus===1" type="success" size="small" @click="openAuditDialog(scope.row, 2)">通过</el-button>
              <el-button v-if="scope.row.claimStatus===1" type="danger" size="small" @click="openAuditDialog(scope.row, -1)">驳回</el-button>
              <el-button v-if="scope.row.claimStatus===2 || scope.row.claimStatus===-1" type="danger" size="small" @click="handleDeleteClaim(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- Audit Dialog -->
    <el-dialog v-model="auditDialogVisible" :title="auditForm.source === 'claim' ? (auditForm.status === 2 ? '通过审核' : '驳回审核') : (auditForm.status === 1 ? '通过审核' : '驳回审核')" width="500px">
      <el-form label-position="top">
        <el-form-item :label="auditForm.source === 'claim' ? (auditForm.status === 2 ? '请填写领取时间、地点及所需携带证件（如学生证/身份证）：' : '请填写驳回理由：') : (auditForm.status === 1 ? '请填写审核通过回复（选填）：' : '请填写驳回理由：')">
          <el-input 
            v-model="auditForm.reply" 
            type="textarea" 
            :rows="4"
            :placeholder="auditForm.source === 'claim' ? (auditForm.status === 2 ? '例：请于工作日9:00-17:00到行政楼101领取，请携带学生证。' : '请输入驳回理由') : (auditForm.status === 1 ? '请输入审核通过的回复内容' : '请输入驳回理由')"
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="auditDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAudit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import http from '../api/http'
import { ElMessage } from 'element-plus'

// Suppress ResizeObserver error
const debounce = (fn: any, delay: number) => {
  let timer: any = null
  return function () {
    // @ts-ignore
    let context = this
    let args = arguments
    clearTimeout(timer)
    timer = setTimeout(function () {
      fn.apply(context, args)
    }, delay)
  }
}

// Workaround for Element Plus table resize error
const _ResizeObserver = window.ResizeObserver
window.ResizeObserver = class ResizeObserver extends _ResizeObserver {
  constructor(callback: any) {
    callback = debounce(callback, 16)
    super(callback)
  }
} as any

const activeTab = ref('lost')
const auditFilter = ref(0)
const lostItems = ref<any[]>([])
const foundItems = ref<any[]>([])
const allClaims = ref<any[]>([])

const categoryMap: Record<string, string> = {
  'card': '证件',
  'electronic': '电子设备',
  'book': '书籍',
  'daily': '生活用品',
  'other': '其他'
}

const formatCategory = (key: string) => {
  return categoryMap[key] || key || '其他'
}

const fetchItems = async () => {
  try {
    let statusParam = ''
    const filterVal = Number(auditFilter.value)
    if (filterVal !== -1) {
      statusParam = `?auditStatus=${filterVal}`
    }
    
    // Construct claim status param based on auditFilter mapping
    // UI: 0=Pending, 1=Approved, 2=Rejected, -1=All
    // API (Claim): 1=Pending, 2=Approved, -1=Rejected
    let claimStatusParam = ''
    if (filterVal === 0) claimStatusParam = '?claimStatus=1'
    else if (filterVal === 1) claimStatusParam = '?claimStatus=2'
    else if (filterVal === 2) claimStatusParam = '?claimStatus=-1'
    else claimStatusParam = '' // All
    
    console.log('Fetching items with statusParam:', statusParam, 'claimStatusParam:', claimStatusParam)
    const l = await http.get(`/admin/lost/list${statusParam}`)
    lostItems.value = l.data.records
    const f = await http.get(`/admin/found/list${statusParam}`)
    foundItems.value = f.data.records
    
    const lc = await http.get(`/admin/lost/claims${claimStatusParam}`)
    const fc = await http.get(`/admin/found/claims${claimStatusParam}`)
    const lData = (lc.data.records || []).map((i: any) => ({ ...i, type: 'lost' }))
    const fData = (fc.data.records || []).map((i: any) => ({ ...i, type: 'found' }))
    allClaims.value = [...lData, ...fData]
  } catch (e) {
    console.error(e)
  }
}

const handleAudit = async (row: any, type: string, status: number) => {
  try {
    await http.put(`/${type}/audit`, { id: row.id, auditStatus: status })
    ElMessage.success('操作成功')
    fetchItems()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (row: any, type: string) => {
  if (!confirm('确定要删除这条记录吗？')) return
  try {
    await http.delete(`/admin/${type}/${row.id}`)
    ElMessage.success('删除成功')
    fetchItems()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const handleDeleteClaim = async (row: any) => {
  if (!confirm('确定要删除这条审核记录吗？')) return
  try {
    await http.delete(`/admin/${row.type}/claims/${row.id}`)
    ElMessage.success('删除成功')
    fetchItems()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const auditDialogVisible = ref(false)
const auditForm = ref({
  id: '',
  type: '',
  status: 0,
  reply: '',
  source: 'claim' // 'claim' or 'item'
})

const openAuditDialog = (row: any, status: number, source: string = 'claim', type: string = '') => {
  auditForm.value.id = row.id
  auditForm.value.status = status
  auditForm.value.reply = ''
  auditForm.value.source = source
  if (source === 'item') {
    auditForm.value.type = type
  } else {
    auditForm.value.type = row.type
  }
  auditDialogVisible.value = true
}

const submitAudit = async () => {
  if (auditForm.value.source === 'claim') {
    if (!auditForm.value.reply) {
      ElMessage.warning('请填写' + (auditForm.value.status === 2 ? '领取时间和地点' : '驳回理由'))
      return
    }
  } else {
    if (auditForm.value.status === 2 && !auditForm.value.reply) {
      ElMessage.warning('请填写驳回理由')
      return
    }
  }

  try {
    if (auditForm.value.source === 'claim') {
      await http.put(`/${auditForm.value.type}/claim-audit`, { 
        id: auditForm.value.id, 
        claimStatus: auditForm.value.status,
        reply: auditForm.value.reply 
      })
    } else {
      await http.put(`/${auditForm.value.type}/audit`, { 
        id: auditForm.value.id, 
        auditStatus: auditForm.value.status,
        reply: auditForm.value.reply 
      })
    }
    ElMessage.success('操作成功')
    auditDialogVisible.value = false
    fetchItems()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  fetchItems()
})
</script>

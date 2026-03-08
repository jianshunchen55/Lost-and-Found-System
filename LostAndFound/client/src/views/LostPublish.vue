<template>
  <div class="publish-page container">
    <!-- Left: Form -->
    <div class="form-section">
      <div class="page-header">
        <h2>发布失物信息</h2>
        <span class="sub-title">填写越详细，找回概率越高</span>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        
        <!-- Basic Info -->
        <div class="section-title">基础信息</div>
        <el-form-item label="物品名称" prop="title">
          <el-input v-model="form.title" placeholder="例如：黑色联想笔记本电脑" maxlength="100" show-word-limit />
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="物品类别" prop="category">
              <el-select v-model="form.category" placeholder="请选择类别" style="width: 100%">
                <el-option label="证件 (学生证/身份证等)" value="card" />
                <el-option label="电子设备 (手机/电脑等)" value="electronic" />
                <el-option label="书籍 (教材/小说等)" value="book" />
                <el-option label="生活用品 (水杯/雨伞等)" value="daily" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="丢失时间" prop="lostTime">
               <el-date-picker 
                 v-model="form.lostTime" 
                 type="datetime" 
                 placeholder="选择丢失大致时间" 
                 :disabled-date="disabledDate"
                 style="width: 100%"
               />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Scene Info -->
        <div class="section-title">场景信息</div>
        <el-form-item label="丢失地点" prop="pointId">
           <el-select 
             v-model="form.pointId" 
             placeholder="选择校内地点" 
             filterable 
             clearable
             @change="handlePointChange" 
             style="width: 100%"
           >
             <el-option v-for="p in points" :key="p.id" :label="p.name" :value="p.id" />
           </el-select>
           <div class="sub-input" style="margin-top: 8px; width: 100%">
             <el-input v-model="form.locationDetail" placeholder="补充详细位置（选填），如：一楼119教室讲台" style="width: 100%" />
           </div>
        </el-form-item>
        
        <!-- Description -->
        <div class="section-title">详细描述</div>
        <el-form-item label="物品描述" prop="description">
          <el-input 
            type="textarea" 
            v-model="form.description" 
            :rows="5" 
            placeholder="请详细描述物品特征（颜色、品牌、划痕等）以及丢失时的具体情况，有助于快速找回。"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="认领要求 (选填)" prop="claimRequirement">
           <el-input 
             type="textarea" 
             v-model="form.claimRequirement" 
             :rows="2" 
             placeholder="如：需提供学生证编号，或描述包内物品"
           />
        </el-form-item>

        <el-form-item label="联系方式" prop="contact">
          <el-input v-model="form.contact" placeholder="默认使用个人资料中的联系方式" />
        </el-form-item>

        <!-- Images -->
        <div class="section-title">图片上传</div>
        <el-form-item prop="images">
          <UploadImage :key="uploadKey" action="/api/file/upload" :limit="5" @change="handleImages" :initial="form.images" />
          <div class="tip">支持JPG/PNG，单张≤2MB，最多5张。</div>
        </el-form-item>

        <!-- Actions -->
                <div class="form-actions">
                  <el-button @click="clearForm">清空</el-button>
                  <el-button @click="saveDraft">暂存</el-button>
                  <el-button type="primary" @click="submit" :loading="submitting">提交审核</el-button>
                </div>

      </el-form>
    </div>

    <!-- Right: Map & Preview -->
    <div class="side-section">
      <div class="map-box">
        <h4>📍 地点选择辅助</h4>
        <div class="map-wrapper">
          <MapSelector 
            :key="uploadKey"
            :mapSrc="mapImageSrc" 
            :points="points" 
            :selectedId="form.pointId"
            @select="onMapSelect" 
          />
        </div>
        <p class="map-tip">点击地图上的点位可快速填入地点</p>
      </div>

      <div class="preview-box">
        <h4>📝 填写贴士</h4>
        <ul>
          <li><strong>准确的时间</strong>有助于通过监控查找线索。</li>
          <li><strong>详细的描述</strong>（如贴纸、划痕）能大幅提高匹配度。</li>
          <li><strong>清晰的图片</strong>是找回物品的关键。</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, watch, computed } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import UploadImage from '../components/UploadImage.vue'
import MapSelector from '../components/MapSelector.vue'
import http from '../api/http'

const store = useStore()
const router = useRouter()
const formRef = ref<FormInstance>()
const route = useRoute()
const submitting = ref(false)
const points = ref<any[]>([])
const user = computed(() => store.state.auth.user)

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

// Form Data
  const uploadKey = ref(0)
  const form = reactive({
    title: '',
  category: '',
  lostTime: '',
  pointId: null as number | null,
  locationDetail: '',
  description: '',
  claimRequirement: '',
  contact: '',
  images: [] as string[]
})

// Validation Rules
const rules = reactive<FormRules>({
  title: [{ required: true, message: '请输入物品名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择类别', trigger: 'change' }],
  lostTime: [{ required: true, message: '请选择丢失时间', trigger: 'change' }],
  pointId: [{ required: true, message: '请选择丢失地点', trigger: 'change' }],
  description: [{ required: true, message: '请输入详细描述', trigger: 'blur' }]
})

// Disabled Date: Future dates disabled
const disabledDate = (time: Date) => {
  return time.getTime() > Date.now()
}

// Map Selection
function onMapSelect(p: any) {
  if (p.id) {
    form.pointId = p.id
    ElMessage.success(`已选择地点：${p.name}`)
  } else {
    // If user clicked somewhere without a point, we could allow generic coords
    // But for now, user requirements emphasize "campus points"
    // We can potentially find nearest point or just ignore
    ElMessage.info('请点击地图上的标记点')
  }
}

function handlePointChange(val: number) {
  // Map selector will update via props
}

function handleImages(urls: string[]) {
  form.images = urls
}

// Caching Logic
const CACHE_KEY = computed(() => user.value ? `lost_publish_cache_${user.value.id}` : null)

function loadCache() {
  if (!CACHE_KEY.value) return
  const cached = localStorage.getItem(CACHE_KEY.value)
  if (cached) {
    try {
      const data = JSON.parse(cached)
      Object.assign(form, data)
      ElMessage.info({ message: '已恢复上次未提交的内容', duration: 1500 })
    } catch (e) {
      console.error('Cache parse error', e)
    }
  }
}

// Auto-save every 30s
let autoSaveInterval: any = null

const editId = ref<string | null>(null)

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

onMounted(async () => {
    loadMapConfig()
    if (!user.value) {
      ElMessage.warning('请先登录')
      router.push('/login')
      return
    }
    // Role check
    const roles = (user.value as any).roles || []
    const isStudent = roles.some((r: any) => r.code === 'ROLE_STUDENT' || r.code === 'student') || user.value.role === 'ROLE_STUDENT'
    const isAdmin = roles.some((r: any) => ['ROLE_ADMIN', 'ROLE_MANAGER', 'admin', 'manager'].includes(r.code)) || ['ROLE_ADMIN', 'ROLE_MANAGER', 'admin', 'manager'].includes(user.value.role)
    
    if (!isStudent && !isAdmin) {
       ElMessage.error('仅学生角色可发布失物信息')
    }

    // Initialize Contact
    if (!form.contact) {
      form.contact = user.value.phone || user.value.email || ''
    }

    // Fetch Points
  try {
      const res = await http.get('/map/point', {
        headers: { 'Cache-Control': 'no-cache', 'Pragma': 'no-cache' }
      })
      points.value = res.data || []
    } catch (e) {
      console.error('Failed to load points', e)
    }

    // Check for edit mode
    const queryId = router.currentRoute.value.query.id
    const paramId = route.params.id
    const anyId = queryId || paramId
    if (anyId) {
      editId.value = String(anyId)
      loadEditData(editId.value)
    } else {
      loadCache()
      autoSaveInterval = setInterval(() => {
        if (CACHE_KEY.value) {
          localStorage.setItem(CACHE_KEY.value, JSON.stringify(form))
        }
      }, 30000)
    }
  })

  onBeforeUnmount(() => {
    if (autoSaveInterval) {
      clearInterval(autoSaveInterval)
    }
  })

  async function loadEditData(id: string) {
     try {
       const res = await http.get(`/lost/${id}`)
       const data = res.data
      form.title = data.title
      form.category = data.category
      form.lostTime = data.lostTime
      form.pointId = data.pointId
      form.description = data.description
      form.contact = data.contact
      
      // Restore images
      if (data.images) {
         try {
           form.images = JSON.parse(data.images)
         } catch (e) {
           form.images = data.imageUrl ? data.imageUrl.split(',') : []
         }
      } else if (data.imageUrl) {
          form.images = data.imageUrl.split(',')
      }

      // Restore location detail
      if (data.pointId && points.value.length > 0) {
          const p = points.value.find(x => x.id === data.pointId)
          if (p && data.location && data.location.startsWith(p.name)) {
              form.locationDetail = data.location.substring(p.name.length).trim()
          } else {
              form.locationDetail = data.location || ''
          }
      } else {
          form.locationDetail = data.location || ''
      }

      // Restore claim requirement from description
      if (form.description && form.description.includes('\n\n认领要求: ')) {
          const parts = form.description.split('\n\n认领要求: ')
          form.description = parts[0]
          form.claimRequirement = parts[1]
      }
      
      ElMessage.info('已加载待修改内容')
    } catch (e) {
      ElMessage.error('加载失败')
      router.push('/profile?tab=records')
    }
  }

  function saveDraft() {
    if (CACHE_KEY.value) {
      localStorage.setItem(CACHE_KEY.value, JSON.stringify(form))
      ElMessage.success('已暂存到本地')
    } else {
      ElMessage.warning('无法暂存（未获取到用户信息）')
    }
  }

  function clearForm() {
    ElMessageBox.confirm('确定要清空所有内容吗？', '提示', { type: 'warning' }).then(() => {
      // Clear all fields explicitly
      form.title = ''
      form.category = ''
      form.lostTime = ''
      form.pointId = null
      form.locationDetail = ''
      form.description = ''
      form.claimRequirement = ''
      form.contact = ''
      form.images = []
      
      formRef.value?.clearValidate()
      uploadKey.value++
      if (CACHE_KEY.value) localStorage.removeItem(CACHE_KEY.value)
      ElMessage.success('已清空')
    }).catch(() => {
      // User cancelled, do nothing
    })
  }

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const selectedPoint = points.value.find(p => p.id === form.pointId)
        function formatDateTime(val: any) {
          if (!val) return null
          const d = typeof val === 'string' ? new Date(val) : val as Date
          if (isNaN(d.getTime())) return null
          const pad = (n: number) => (n < 10 ? '0' + n : '' + n)
          const Y = d.getFullYear()
          const M = pad(d.getMonth() + 1)
          const D = pad(d.getDate())
          const h = pad(d.getHours())
          const m = pad(d.getMinutes())
          const s = pad(d.getSeconds())
          return `${Y}-${M}-${D} ${h}:${m}:${s}`
        }
        
        const payload = {
          title: form.title,
          category: form.category,
          lostTime: formatDateTime(form.lostTime),
          pointId: form.pointId,
          location: selectedPoint ? (selectedPoint.name + (form.locationDetail ? ' ' + form.locationDetail : '')) : form.locationDetail,
          latitude: selectedPoint ? selectedPoint.latitude : null,
          longitude: selectedPoint ? selectedPoint.longitude : null,
          description: form.description + (form.claimRequirement ? `\n\n认领要求: ${form.claimRequirement}` : ''),
          contact: form.contact,
          imageUrl: form.images.join(','), // Comma separated
          thumbnailUrl: form.images[0] || '',
          images: JSON.stringify(form.images) // Store full JSON array if backend supports, or just rely on comma string
        }
        
        if (editId.value) {
          // We need to add ID to payload for update
          (payload as any).id = editId.value
          await http.put('/lost/update', payload)
          ElMessage.success('修改成功，请等待审核')
          if (CACHE_KEY.value) localStorage.removeItem(CACHE_KEY.value)
          router.push('/profile?tab=mypost')
        } else {
          await http.post('/lost/add', payload)
          ElMessage.success('发布成功，请等待审核')
          
          // Clear form and cache
          if (CACHE_KEY.value) localStorage.removeItem(CACHE_KEY.value)
          
          // Reset form data to prevent it from being saved again if interval triggers before unmount
          form.title = ''
          form.category = ''
          form.lostTime = ''
          form.pointId = null
          form.locationDetail = ''
          form.description = ''
          form.claimRequirement = ''
          form.contact = ''
          form.images = []
          
          router.push('/profile?tab=mypost')
        }
        
      } catch (e) {
        ElMessage.error('操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}
</script>

<style scoped>
.publish-page {
  display: flex;
  gap: 24px;
  margin-top: 30px;
  padding-bottom: 40px;
}

.form-section {
  flex: 1.5;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  padding: 40px;
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.side-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.map-box, .preview-box {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  padding: 24px;
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.6);
  transition: transform 0.3s ease;
}

.map-box:hover, .preview-box:hover {
  transform: translateY(-5px);
}

.map-box h4, .preview-box h4 {
  margin: 0 0 16px 0;
  color: #2c3e50;
  font-size: 18px;
  font-weight: 700;
}

.page-header {
  margin-bottom: 32px;
  border-bottom: 2px solid rgba(0, 0, 0, 0.05);
  padding-bottom: 20px;
}

.page-header h2 {
  font-size: 28px;
  font-weight: 800;
  margin-bottom: 8px;
  background: linear-gradient(135deg, #FF6B6B 0%, #FF8E53 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.sub-title {
  color: #666;
  font-size: 14px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  margin: 32px 0 24px;
  padding-left: 16px;
  border-left: 4px solid #FF6B6B;
  color: #2c3e50;
}

.tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.map-tip {
  font-size: 12px;
  color: #666;
  text-align: center;
  margin-top: 12px;
}

/* Custom Element Plus overrides */
:deep(.el-form-item__label) {
  font-weight: 600;
  color: #2c3e50;
}

:deep(.el-input__wrapper), :deep(.el-textarea__inner) {
  box-shadow: none !important;
  background: rgba(245, 247, 250, 0.5);
  border: 1px solid rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  border-radius: 12px;
  padding: 8px 12px;
}

:deep(.el-input__wrapper:hover), :deep(.el-textarea__inner:hover) {
  background: rgba(255, 255, 255, 0.8);
  border-color: #FF6B6B;
}

:deep(.el-input__wrapper.is-focus), :deep(.el-textarea__inner:focus) {
  background: #fff;
  border-color: #FF6B6B;
  box-shadow: 0 0 0 3px rgba(255, 107, 107, 0.1) !important;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #FF6B6B 0%, #FF8E53 100%);
  border: none;
  border-radius: 12px;
  padding: 12px 32px;
  height: auto;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
  transition: all 0.3s ease;
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 107, 107, 0.4);
}

:deep(.el-button:not(.el-button--primary)) {
  border-radius: 12px;
  padding: 12px 24px;
  height: auto;
  border: 1px solid rgba(0,0,0,0.1);
  background: transparent;
}

:deep(.el-button:not(.el-button--primary):hover) {
  color: #FF6B6B;
  border-color: #FF6B6B;
  background: rgba(255, 107, 107, 0.05);
}

.preview-box ul {
  padding-left: 20px;
  line-height: 1.8;
  color: #555;
}

.preview-box li {
  margin-bottom: 8px;
}

@media (max-width: 992px) {
  .publish-page {
    flex-direction: column;
  }
  .side-section {
    order: -1;
  }
}
</style>

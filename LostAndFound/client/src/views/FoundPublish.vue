<template>
  <div class="publish-page container">
    <!-- Left: Form -->
    <div class="form-section">
      <div class="page-header">
        <h2>发布拾物信息</h2>
        <span class="sub-title">帮助失主找回物品，传递校园正能量</span>
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
            <el-form-item label="拾获时间" prop="foundTime">
               <el-date-picker 
                 v-model="form.foundTime" 
                 type="datetime" 
                 placeholder="选择拾获时间" 
                 :disabled-date="disabledDate"
                 style="width: 100%"
               />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Scene Info -->
        <div class="section-title">拾获地点</div>
        <el-form-item label="地点选择" prop="pointId">
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
             <el-input v-model="form.locationDetail" placeholder="补充详细位置（选填），如：三楼楼梯口" style="width: 100%" />
           </div>
        </el-form-item>
        
        <!-- Description -->
        <div class="section-title">详细描述</div>
        <el-form-item label="物品描述" prop="description">
          <el-input 
            type="textarea" 
            v-model="form.description" 
            :rows="5" 
            placeholder="请描述物品特征（颜色、品牌等）。建议不要描述得过于详细（如具体证件号），以便核实失主身份。"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="认领方式 (选填)" prop="claimRequirement">
           <el-input 
             type="textarea" 
             v-model="form.claimRequirement" 
             :rows="2" 
             placeholder="如：请携带学生证到图书馆前台领取"
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
          <el-button type="primary" @click="submit" :loading="submitting">发布拾物</el-button>
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
        <h4>📝 拾金不昧贴士</h4>
        <ul>
          <li><strong>保护隐私</strong>：证件类物品建议遮挡关键信息。</li>
          <li><strong>核实身份</strong>：归还时请务必核对对方身份信息。</li>
          <li><strong>安全交接</strong>：建议在公共场所或保卫处进行交接。</li>
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
  foundTime: '',
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
  foundTime: [{ required: true, message: '请选择拾获时间', trigger: 'change' }],
  pointId: [{ required: true, message: '请选择拾获地点', trigger: 'change' }],
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
const CACHE_KEY = computed(() => user.value ? `found_publish_cache_${user.value.id}` : null)

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
      points.value = []
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
       const res = await http.get(`/found/${id}`)
       const data = res.data
      form.title = data.title
      form.category = data.category
      form.foundTime = data.foundTime
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
      
      // Restore claim requirement
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
      form.foundTime = ''
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
      // User cancelled
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
          foundTime: formatDateTime(form.foundTime),
          pointId: form.pointId,
          // IMPORTANT: Synchronize location text for Home.vue display
          location: selectedPoint ? (selectedPoint.name + (form.locationDetail ? ' ' + form.locationDetail : '')) : form.locationDetail,
          latitude: selectedPoint ? selectedPoint.latitude : null,
          longitude: selectedPoint ? selectedPoint.longitude : null,
          description: form.description + (form.claimRequirement ? `\n\n认领要求: ${form.claimRequirement}` : ''),
          contact: form.contact,
          imageUrl: form.images.join(','),
          thumbnailUrl: form.images[0] || '',
          images: JSON.stringify(form.images)
        }
        
        if (editId.value) {
          (payload as any).id = editId.value
          await http.put('/found/update', payload)
          ElMessage.success('修改成功，已自动审核通过')
          if (CACHE_KEY.value) localStorage.removeItem(CACHE_KEY.value)
          router.push('/profile?tab=mypost')
        } else {
          await http.post('/found/add', payload)
          ElMessage.success('发布成功，已自动审核通过')
          
          // Clear form and cache
          if (CACHE_KEY.value) localStorage.removeItem(CACHE_KEY.value)
          
          // Reset form data
          form.title = ''
          form.category = ''
          form.foundTime = ''
          form.pointId = null
          form.locationDetail = ''
          form.description = ''
          form.claimRequirement = ''
          form.contact = ''
          form.images = []

          router.push('/profile?tab=mypost')
        }
        
      } catch (e) {
        console.error(e)
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
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
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
  border-left: 4px solid #4facfe;
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
  border-color: #4facfe;
}

:deep(.el-input__wrapper.is-focus), :deep(.el-textarea__inner:focus) {
  background: #fff;
  border-color: #4facfe;
  box-shadow: 0 0 0 3px rgba(79, 172, 254, 0.1) !important;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border: none;
  border-radius: 12px;
  padding: 12px 32px;
  height: auto;
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
  padding: 12px 24px;
  height: auto;
  border: 1px solid rgba(0,0,0,0.1);
  background: transparent;
}

:deep(.el-button:not(.el-button--primary):hover) {
  color: #4facfe;
  border-color: #4facfe;
  background: rgba(79, 172, 254, 0.05);
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
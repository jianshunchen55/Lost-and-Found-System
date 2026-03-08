<template>
  <div class="map-manage container">
    <div class="page-header">
      <h2>地图管理</h2>
      <span class="sub-title">管理校园地图图片及定位点</span>
    </div>

    <el-card class="box-card mb-4">
      <template #header>
        <div class="card-header">
          <span>地图设置</span>
          <el-button type="primary" size="small" @click="saveMapConfig" :loading="savingConfig">保存配置</el-button>
        </div>
      </template>
      <div class="map-upload-section">
        <div class="current-map">
          <p>当前地图预览：</p>
          <img :src="mapPreview" class="preview-img" />
        </div>
        <div class="upload-control">
          <p>更换地图：</p>
          <UploadImage 
            :key="uploadKey" 
            action="/api/file/upload" 
            :limit="1" 
            @change="handleMapUpload" 
            :initial="currentMapUrl ? [currentMapUrl] : []" 
          />
          <div class="tip">建议上传高分辨率的校园平面图，格式JPG/PNG。</div>
        </div>
      </div>
    </el-card>

    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>点位管理</span>
          <div class="actions">
            <span class="tip-text">点击下方地图添加/编辑点位</span>
          </div>
        </div>
      </template>
      
      <div class="map-editor-container">
        <div class="map-wrapper">
          <MapSelector 
            :key="mapSizeKey"
            :mapSrc="mapPreview" 
            :points="points" 
            :selectedId="selectedPointId"
            :initialPoint="tempPoint"
            @select="onMapSelect" 
          />
        </div>
        
        <div class="point-list">
          <h4>已有点位 ({{ points.length }})</h4>
          <el-table :data="points" style="width: 100%" height="400">
            <el-table-column prop="name" label="名称" />
            <el-table-column label="坐标" width="120">
              <template #default="scope">
                {{ Number(scope.row.longitude).toFixed(1) }}%, {{ Number(scope.row.latitude).toFixed(1) }}%
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="scope">
                <el-button type="text" size="small" @click="editPoint(scope.row)">编辑</el-button>
                <el-button type="text" size="small" class="danger-text" @click="deletePoint(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-card>

    <!-- Point Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="400px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如：图书馆" />
        </el-form-item>
        <el-form-item label="X坐标" prop="longitude">
          <el-input-number v-model="form.longitude" :disabled="true" />
        </el-form-item>
        <el-form-item label="Y坐标" prop="latitude">
          <el-input-number v-model="form.latitude" :disabled="true" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitPoint" :loading="submitting">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import http from '../api/http'
import UploadImage from '../components/UploadImage.vue'
import MapSelector from '../components/MapSelector.vue'

const uploadKey = ref(0)
const mapSizeKey = ref(0)
const currentMapUrl = ref('')
const mapPreview = computed(() => {
  if (!currentMapUrl.value) return defaultMapSvg
  if (currentMapUrl.value.startsWith('http')) return currentMapUrl.value
  return 'http://localhost:8080' + currentMapUrl.value
})

const defaultMapSvg = 'data:image/svg+xml;utf8,' + encodeURIComponent(`
  <svg xmlns="http://www.w3.org/2000/svg" width="800" height="500">
    <rect width="100%" height="100%" fill="#fafafa"/>
    <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="#999">暂无地图，请上传</text>
  </svg>
`)

const savingConfig = ref(false)
const points = ref<any[]>([])
const selectedPointId = ref<number | null>(null)
const tempPoint = ref<{x:number, y:number} | null>(null)

// Dialog
const dialogVisible = ref(false)
const dialogTitle = ref('添加点位')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: null as number | null,
  name: '',
  latitude: 0,
  longitude: 0
})
const rules = reactive<FormRules>({
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
})

onMounted(() => {
  loadConfig()
  loadPoints()
})

async function loadConfig() {
  try {
    const res = await http.get('/config/map_url')
    if (res.data && res.data.value) {
      currentMapUrl.value = res.data.value
      uploadKey.value++ // force refresh upload component
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadPoints() {
  try {
    const res = await http.get('/map/point', {
      params: { _t: Date.now() },
      headers: { 'Cache-Control': 'no-cache', 'Pragma': 'no-cache' }
    })
    points.value = res.data || []
  } catch (e) {
    ElMessage.error('加载点位失败')
  }
}

function handleMapUpload(urls: string[]) {
  if (urls.length > 0) {
    currentMapUrl.value = urls[0]
  } else {
    currentMapUrl.value = ''
  }
}

async function saveMapConfig() {
  if (!currentMapUrl.value) {
    return ElMessage.warning('请先上传图片')
  }
  savingConfig.value = true
  try {
    await http.post('/config', {
      cfgKey: 'map_url',
      cfgValue: currentMapUrl.value,
      description: 'Campus Map Image URL'
    })
    ElMessage.success('地图配置已保存')
    mapSizeKey.value++ // refresh map selector
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    savingConfig.value = false
  }
}

function onMapSelect(p: any) {
  if (p.id) {
    // Edit existing
    editPoint(p)
  } else {
    // Add new
    selectedPointId.value = null
    tempPoint.value = { x: p.x, y: p.y }
    
    form.id = null
    form.name = ''
    form.longitude = Number(p.x.toFixed(2))
    form.latitude = Number(p.y.toFixed(2))
    dialogTitle.value = '添加点位'
    dialogVisible.value = true
  }
}

function editPoint(p: any) {
  selectedPointId.value = p.id
  form.id = p.id
  form.name = p.name
  form.longitude = p.longitude
  form.latitude = p.latitude
  dialogTitle.value = '编辑点位'
  dialogVisible.value = true
}

function deletePoint(p: any) {
  ElMessageBox.confirm(`确定要删除点位“${p.name}”吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await http.delete('/map/point', { data: { id: p.id } })
      ElMessage.success('已删除')
      loadPoints()
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    // cancelled
  })
}

async function submitPoint() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (form.id) {
          await http.put('/map/point', form)
        } else {
          await http.post('/map/point', form)
        }
        ElMessage.success(form.id ? '更新成功' : '添加成功')
        dialogVisible.value = false
        loadPoints()
        tempPoint.value = null
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
.mb-4 { margin-bottom: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.map-upload-section {
  display: flex;
  gap: 20px;
}
.current-map {
  flex: 2;
  border: 1px solid #eee;
  padding: 10px;
  border-radius: 4px;
  background: #f9f9f9;
  text-align: center;
}
.preview-img {
  max-width: 100%;
  max-height: 300px;
  object-fit: contain;
}
.upload-control {
  flex: 1;
}
.tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}
.map-editor-container {
  display: flex;
  gap: 20px;
}
.map-wrapper {
  flex: 3;
}
.point-list {
  flex: 1;
  border-left: 1px solid #eee;
  padding-left: 20px;
}
.danger-text { color: #f56c6c; }
</style>

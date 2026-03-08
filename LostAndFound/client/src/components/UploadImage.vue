<template>
  <div v-loading="uploading" element-loading-text="上传中...">
    <el-upload
      drag
      multiple
      :action="action"
      :headers="headers"
      :before-upload="beforeUpload"
      :on-progress="onProgress"
      :on-success="onSuccess"
      :on-error="onError"
      :limit="limit"
      :on-exceed="onExceed"
      :show-file-list="false">
      <i class="el-icon-upload"></i>
      <div class="el-upload__text">拖拽图片到此处，或 <em>点击上传</em></div>
      <div class="el-upload__tip" slot="tip">只能上传jpg/png文件，且不超过5MB（超过自动压缩）</div>
    </el-upload>
    <div class="image-list">
      <div v-for="(url,i) in urls" :key="i" class="image-item">
        <img :src="url" />
        <div class="actions">
          <span @click="remove(i)">删除</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import store from '../store'
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{ action: string, limit?: number, initial?: string[] }>()
const emit = defineEmits(['change'])
const headers = { Authorization: `Bearer ${store.state.auth.token}` }
const urls = ref<string[]>([])
const limit = props.limit || 5
const uploading = ref(false)

watch(() => props.initial, (val) => {
  if (val && val.length > 0) {
    if (urls.value.length === 0) urls.value = [...val]
  }
}, { immediate: true })

function beforeUpload(file: File) {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png'
  if (!isImage) {
    ElMessage.error('只能上传JPG/PNG图片')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    // Compress
    return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.readAsDataURL(file)
        reader.onload = (e) => {
            const img = new Image()
            img.src = e.target?.result as string
            img.onload = () => {
                const canvas = document.createElement('canvas')
                const ctx = canvas.getContext('2d')
                const maxWidth = 1024
                const maxHeight = 1024
                let width = img.width
                let height = img.height
                
                if (width > height) {
                    if (width > maxWidth) {
                        height = Math.round(height * maxWidth / width)
                        width = maxWidth
                    }
                } else {
                    if (height > maxHeight) {
                        width = Math.round(width * maxHeight / height)
                        height = maxHeight
                    }
                }
                
                canvas.width = width
                canvas.height = height
                ctx?.drawImage(img, 0, 0, width, height)
                
                canvas.toBlob((blob) => {
                    if (blob) {
                        const compressedFile = new File([blob], file.name, { type: file.type, lastModified: Date.now() })
                        resolve(compressedFile)
                    } else {
                        reject()
                    }
                }, file.type, 0.7)
            }
        }
    })
  }
  return true
}

function onProgress() {
  uploading.value = true
}

function onSuccess(resp: any) {
  uploading.value = false
  if (resp && resp.url) {
    urls.value.push(resp.url)
    emit('change', urls.value)
  } else {
    ElMessage.error('上传失败')
  }
}

function onError(err: any) {
  uploading.value = false
  ElMessage.error('上传出错: ' + err.message)
}

function onExceed() {
  ElMessage.warning(`最多只能上传 ${limit} 张图片`)
}

function remove(i: number) {
  urls.value.splice(i, 1)
  emit('change', urls.value)
}
</script>

<style scoped>
.image-list {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 12px;
}
.image-item {
  position: relative;
  width: 100px;
  height: 100px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}
.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.actions {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0,0,0,0.6);
  color: white;
  text-align: center;
  font-size: 12px;
  padding: 4px 0;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}
.image-item:hover .actions {
  opacity: 1;
}
</style>

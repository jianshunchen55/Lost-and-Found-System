<template>
  <div>
    <el-button type="primary" @click="openText">新建纯文字公告</el-button>
    <el-button type="success" @click="openImage">新建带图片公告</el-button>
    <el-table :data="list" style="width:100%;margin-top:12px">
      <el-table-column prop="title" label="标题" show-overflow-tooltip />
      <el-table-column label="封面图" width="120">
        <template #default="{row}">
          <el-image v-if="row.imageUrl" :src="row.imageUrl" style="width:50px;height:50px" fit="cover" :preview-src-list="[row.imageUrl]" preview-teleported/>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
      <el-table-column label="操作" width="180">
        <template #default="{row}">
          <el-button size="small" @click="edit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="visible" title="公告编辑" width="60%">
      <el-input v-if="isImageNotice" v-model="form.title" placeholder="标题" style="margin-bottom:8px" />
      <el-input 
        v-else 
        v-model="form.content" 
        type="textarea" 
        :rows="6" 
        placeholder="请输入内容" 
        style="margin-bottom:8px" 
      />
      <div v-if="isImageNotice">
        <div v-if="form.imageUrl" style="display:flex;align-items:center;gap:15px;margin-bottom:10px">
          <el-image 
            :src="form.imageUrl" 
            style="width: 100px; height: 100px; border-radius: 4px;" 
            fit="cover"
            :preview-src-list="[form.imageUrl]"
            preview-teleported
          />
          <div style="display:flex;flex-direction:column;gap:10px">
             <el-upload
                action="/api/file/upload"
                :headers="headers"
                :show-file-list="false"
                :on-success="handleUploadSuccess"
                :before-upload="beforeUpload"
             >
                <el-button type="primary" size="small">更换图片</el-button>
             </el-upload>
             <el-button type="danger" size="small" @click="form.imageUrl=''">移除图片</el-button>
          </div>
        </div>
        <el-upload
          v-else
          class="avatar-uploader"
          action="/api/file/upload"
          :headers="headers"
          :show-file-list="false"
          :on-success="handleUploadSuccess"
          :before-upload="beforeUpload"
          style="margin-bottom:10px"
        >
          <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
          <div style="font-size:12px;color:#999;margin-left:10px;display:inline-block">点击上传封面图片</div>
        </el-upload>
      </div>
      <template #footer>
        <el-button @click="visible=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import http from '../api/http'
import store from '../store'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const list = ref<any[]>([])
const visible = ref(false)
const isImageNotice = ref(false)
const form = reactive<any>({ id:null, title:'', content:'', imageUrl:'' })

const headers = computed(() => {
  const token = (store.state as any).auth?.token
  return token ? { Authorization: `Bearer ${token}` } : {}
})

function openText(){ 
  form.id=null; form.title=''; form.content=''; form.imageUrl=''; 
  isImageNotice.value = false; 
  visible.value=true 
}
function openImage(){ 
  form.id=null; form.title=''; form.content=''; form.imageUrl=''; 
  isImageNotice.value = true; 
  visible.value=true 
}
function edit(row:any){ 
  form.id=row.id; form.title=row.title; form.content=row.content; form.imageUrl=row.imageUrl; 
  isImageNotice.value = !!row.imageUrl; 
  visible.value=true 
}
async function save(){ 
  if (isImageNotice.value) {
    if (!form.title) {
      ElMessage.error('请输入标题');
      return;
    }
    if (!form.imageUrl) {
      ElMessage.error('带图片公告必须上传图片');
      return;
    }
    // Fill dummy content for image notice
    if (!form.content) form.content = 'Image Notice';
  } else {
    // Pure text notice
    if (!form.content) {
      ElMessage.error('请输入内容');
      return;
    }
    // Use content as title (truncate if necessary)
    form.title = form.content.substring(0, 255);
    form.imageUrl = '';
  }

  if(form.id){ await http.put('/notice/update', form) } else { await http.post('/notice/add', form) } 
  visible.value=false; 
  load() 
}
async function remove(row:any){ await http.delete('/notice/delete', { data: { id: row.id } }); load() }
async function load(){ const { data } = await http.get('/notice/list'); list.value=data }

const handleUploadSuccess = (res: any) => {
  if (res && res.url) {
    form.imageUrl = res.url
  } else if (typeof res === 'string') {
     form.imageUrl = res
  }
}
const beforeUpload = (file: any) => {
   const isJPG = file.type === 'image/jpeg' || file.type === 'image/png';
   const isLt50M = file.size / 1024 / 1024 < 50;

   if (!isJPG) {
     ElMessage.error('上传图片只能是 JPG/PNG 格式!');
     return false;
   }
   if (!isLt50M) {
     ElMessage.error('上传图片大小不能超过 50MB!');
     return false;
   }
   return true;
 }

load()
</script>
<style scoped>
.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-uploader .el-upload:hover {
  border-color: #409EFF;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  line-height: 100px;
  text-align: center;
  border: 1px dashed #ccc;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>

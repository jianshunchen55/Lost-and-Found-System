<template>
  <div class="register-bg">
    <div class="register-container">
      <el-card class="register-card" shadow="always">
        <template #header>
          <div class="card-header">
            <h2>注册账号</h2>
            <p>加入我们，一起互助</p>
          </div>
        </template>
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" size="large">
          <el-form-item label="我是" prop="role">
            <el-radio-group v-model="form.role" class="role-group">
              <el-radio-button label="ROLE_STUDENT">学生</el-radio-button>
              <el-radio-button label="ROLE_MANAGER">负责人</el-radio-button>
              <el-radio-button label="ROLE_ADMIN">管理员</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="isSpecialRole" label="授权码" prop="adminKey">
            <el-input v-model="form.adminKey" placeholder="请输入管理员/负责人授权码" />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="学号/工号" prop="username">
                <el-input v-model="form.username" placeholder="作为登录账号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
               <el-form-item label="邮箱" prop="email">
                <el-input v-model="form.email" placeholder="用于找回密码" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="设置密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入8位以上密码" show-password />
          </el-form-item>

          <el-form-item style="margin-top: 30px;">
            <el-button type="primary" class="submit-btn" @click="submit">立即注册</el-button>
          </el-form-item>
          
          <div class="form-footer">
            <el-button link @click="$router.push('/login')">
              <span style="color: #909399;">已有账号？</span>
              <span style="color: #409EFF;">返回登录</span>
            </el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>
<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { register } from '../api/auth'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const formRef = ref()
const form = reactive({ 
  username: '', 
  email: '', 
  password: '',
  role: 'ROLE_STUDENT',
  adminKey: ''
})

const isSpecialRole = computed(() => {
  return form.role === 'ROLE_ADMIN' || form.role === 'ROLE_MANAGER'
})

const rules = computed(() => {
  const baseRules = {
    username: [{ required: true, message: '请输入学号/工号', trigger: 'blur' }],
    email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '邮箱格式不正确' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 8, message: '密码长度至少8位' }],
    role: [{ required: true, message: '请选择身份', trigger: 'change' }]
  }
  
  if (isSpecialRole.value) {
    return {
      ...baseRules,
      adminKey: [{ required: true, message: '请输入管理员授权码', trigger: 'blur' }]
    }
  }
  return baseRules
})

function submit() {
  ;(formRef.value as any).validate(async (valid: boolean) => {
    if (!valid) return
    try {
      await register(form)
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (error: any) {
      console.error(error)
      const msg = error.response?.data?.message || '注册失败'
      ElMessage.error(msg)
    }
  })
}
</script>
<style scoped>
.register-bg {
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #8ec5fc 0%, #e0c3fc 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.register-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.register-card {
  width: 480px;
  border-radius: 12px;
  border: none;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.37);
}

.register-card :deep(.el-card__header) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
}

.register-card :deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.6);
}

.card-header {
  text-align: center;
}

.card-header h2 {
  font-size: 24px;
  margin: 0;
  color: #333;
}

.card-header p {
  margin: 8px 0 0;
  color: #909399;
  font-size: 14px;
}

.role-group {
  width: 100%;
  display: flex;
}
.role-group :deep(.el-radio-button__inner) {
  width: 100%;
  padding: 10px 20px;
}
.role-group :deep(.el-radio-button) {
  flex: 1;
  text-align: center;
}

.submit-btn {
  width: 100%;
  font-weight: bold;
  height: 45px;
  font-size: 16px;
  background: linear-gradient(90deg, #36d1dc 0%, #409EFF 100%);
  border: none;
}

.submit-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.form-footer {
  text-align: center;
  margin-top: 10px;
}
</style>
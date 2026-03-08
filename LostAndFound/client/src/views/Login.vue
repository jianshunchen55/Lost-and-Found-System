<template>
  <div class="login-bg">
    <div class="login-container">
      <div class="login-header">
        <div class="logo-circle">
          <span>LF</span>
        </div>
        <h2>校园失物招领</h2>
        <p class="subtitle">让每一个失物都能回家</p>
      </div>
      <el-card class="login-card" shadow="always">
        <template #header>
          <div class="card-header">
            <span>账号登录</span>
          </div>
        </template>
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" size="large">
          <el-form-item label="账号" prop="username">
            <el-input v-model="form.username" placeholder="请输入学号/工号"></el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password @keyup.enter="submit"></el-input>
          </el-form-item>
          <el-form-item style="margin-top: 30px;">
            <el-button type="primary" class="submit-btn" @click="submit">登 录</el-button>
          </el-form-item>
          <div class="form-footer">
            <span class="text-gray">还没有账号？</span>
            <el-button link type="primary" @click="$router.push('/register')">立即注册</el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>
<script setup lang="ts">
import { reactive, ref } from 'vue'
import { login } from '../api/auth'
import store from '../store'
import { ElMessage } from 'element-plus'

const formRef = ref()
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入学号/工号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
function submit() {
  ;(formRef.value as any).validate(async (valid: boolean) => {
    if (!valid) return
    try {
      const { data } = await login(form)
      store.commit('auth/setAuth', { 
        token: data.accessToken, 
        refreshToken: data.refreshToken, 
        user: data.user 
      })
      ElMessage.success('登录成功')
      location.href = '/'
    } catch (error: any) {
      console.error(error)
      const msg = error.response?.data?.message || '登录失败'
      ElMessage.error(msg)
    }
  })
}
</script>
<style scoped>
.login-bg {
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #e0c3fc 0%, #8ec5fc 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.login-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.login-header {
  text-align: center;
  color: #fff;
  text-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.logo-circle {
  width: 60px;
  height: 60px;
  background: rgba(255,255,255,0.9);
  border-radius: 50%;
  margin: 0 auto 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: #409EFF;
  font-size: 24px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
}

.login-header h2 {
  font-size: 28px;
  margin: 0 0 10px;
  font-weight: 600;
  letter-spacing: 1px;
}

.subtitle {
  font-size: 14px;
  opacity: 0.9;
  margin: 0;
}

.login-card {
  width: 400px;
  border-radius: 12px;
  border: none;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.37);
}

.login-card :deep(.el-card__header) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
}

.login-card :deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.6);
}

.card-header {
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.submit-btn {
  width: 100%;
  font-weight: bold;
  letter-spacing: 2px;
  height: 45px;
  font-size: 16px;
  background: linear-gradient(90deg, #409EFF 0%, #36d1dc 100%);
  border: none;
}

.submit-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.form-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 10px;
  font-size: 14px;
}

.text-gray {
  color: #909399;
}
</style>
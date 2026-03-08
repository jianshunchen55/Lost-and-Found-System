<template>
  <div style="display:flex;height:100%">
    <el-menu default-active="1" style="width:220px">
      <el-menu-item v-if="isAdmin" @click="$router.push('/admin/users')">用户管理</el-menu-item>
      <el-menu-item @click="$router.push('/admin/audit')">物品审核</el-menu-item>
      <el-menu-item @click="$router.push('/admin/notice')">公告管理</el-menu-item>
      <el-menu-item v-if="isAdmin" @click="$router.push('/admin/map')">地图管理</el-menu-item>
      <el-menu-item @click="$router.push('/admin/stats')">数据统计</el-menu-item>
    </el-menu>
    <div style="flex:1;padding:16px">
      <router-view />
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import store from '../store'

const isAdmin = computed(() => {
  const user = (store.state as any).auth?.user
  return user?.role === 'ROLE_ADMIN' || (Array.isArray(user?.roles) && user.roles.some((r: any) => r.code === 'ROLE_ADMIN'))
})
</script>

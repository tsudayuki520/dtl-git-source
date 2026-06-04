<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => {
  if (route.path.startsWith('/meeting/')) return '/home'
  return route.path
})

function handleMenuClick(index: string) {
  router.push(index)
}
</script>

<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="layout-aside">
      <div class="logo">赛事管理系统</div>
      <el-menu
        :default-active="activeMenu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        @select="handleMenuClick"
      >
        <el-menu-item index="/home">
          <span>运动会管理</span>
        </el-menu-item>
        <el-menu-item index="/banner">
          <span>轮播图管理</span>
        </el-menu-item>
        <el-menu-item index="/global-notice">
          <span>全局通知管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <span class="header-title" @click="router.push('/home')">后台管理系统</span>
        <el-breadcrumb separator="/" v-if="route.name === 'MeetingDetail'">
          <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>运动会管理</el-breadcrumb-item>
        </el-breadcrumb>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
}
.layout-aside {
  background-color: #304156;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  border-bottom: 1px solid #3a4a5b;
}
.layout-header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 14px;
}
.header-title {
  font-weight: bold;
  font-size: 16px;
  cursor: pointer;
}
.layout-main {
  background-color: #f0f2f5;
}
</style>

<template>
  <el-container class="layout-container">
    <el-header height="60px" class="header">
      <div class="logo">
        <el-icon :size="24" color="#409EFC"><Grid /></el-icon>
        <span>Custom Field System</span>
      </div>
    </el-header>
    <el-main>
      <div class="main-content">
        <FieldList />
      </div>
    </el-main>
  </el-container>
</template>

<script setup>
import { Grid } from '@element-plus/icons-vue'
import FieldList from './components/FieldList.vue'
</script>

<style scoped>
.layout-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}
.header {
  background: white;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  padding: 0 24px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
.main-content {
  max-width: 1200px;
  margin: 0 auto;
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
</style>

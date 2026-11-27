<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <div class="page-shell">
    <h1 class="page-title">{{ title }}</h1>
    <div class="placeholder-card">
      <el-tag type="info" effect="plain" class="status-tag">下一阶段开发</el-tag>
      <h2>模块建设中</h2>
      <p>{{ description ?? '本模块将在后续里程碑中接入完整功能，当前用于验证路由与布局。' }}</p>
    </div>
  </div>
</template>

<style scoped>
.page-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
}

.status-tag {
  margin-bottom: 12px;
}
</style>
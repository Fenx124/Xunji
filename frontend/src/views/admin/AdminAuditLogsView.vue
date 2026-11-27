<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listAuditLogs } from '@/api/admin'
import type { AuditLogVO } from '@/types/admin'
import { formatDateTime } from '@/utils/date'

const MODULES = ['AUTH', 'NOTE', 'SCHEDULE', 'FOCUS', 'AI', 'ADMIN'] as const

const loading = ref(false)
const logs = ref<AuditLogVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

const moduleFilter = ref<string>('')
const username = ref('')
const statusFilter = ref<'all' | '0' | '1'>('all')

async function loadLogs() {
  loading.value = true
  try {
    const params: {
      pageNum: number
      pageSize: number
      module?: string
      username?: string
      status?: number
    } = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (moduleFilter.value) params.module = moduleFilter.value
    if (username.value.trim()) params.username = username.value.trim()
    if (statusFilter.value === '0') params.status = 0
    if (statusFilter.value === '1') params.status = 1
    const res = await listAuditLogs(params)
    logs.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  pageNum.value = 1
  void loadLogs()
}

function moduleTagType(module: string) {
  const map: Record<string, string> = {
    AUTH: 'info',
    NOTE: 'primary',
    SCHEDULE: 'warning',
    FOCUS: 'success',
    AI: 'danger',
    ADMIN: '',
  }
  return map[module] ?? 'info'
}

onMounted(loadLogs)
</script>

<template>
  <div class="admin-page page-shell">
    <div class="filter-bar">
      <el-select v-model="moduleFilter" clearable placeholder="模块" style="width: 140px">
        <el-option v-for="m in MODULES" :key="m" :label="m" :value="m" />
      </el-select>
      <el-input
        v-model="username"
        clearable
        placeholder="用户名"
        style="width: 160px"
        @keyup.enter="onSearch"
      />
      <el-select v-model="statusFilter" style="width: 120px">
        <el-option label="全部状态" value="all" />
        <el-option label="成功" value="1" />
        <el-option label="异常" value="0" />
      </el-select>
      <el-button type="primary" @click="onSearch">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="logs" stripe border size="small">
      <el-table-column label="时间" min-width="150">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column prop="username" label="用户" width="100" show-overflow-tooltip />
      <el-table-column label="模块" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="moduleTagType(row.module)">{{ row.module }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="action" label="操作" min-width="140" show-overflow-tooltip />
      <el-table-column label="请求" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.httpMethod }} {{ row.requestUri }}</template>
      </el-table-column>
      <el-table-column prop="clientIp" label="IP" width="120" />
      <el-table-column label="耗时" width="90" align="right">
        <template #default="{ row }">{{ row.costMs }} ms</template>
      </el-table-column>
      <el-table-column label="状态" width="88" align="center">
        <template #default="{ row }">
          <el-tooltip v-if="row.status !== 1 && row.errorMsg" :content="row.errorMsg" placement="top">
            <el-tag type="danger" size="small">异常</el-tag>
          </el-tooltip>
          <el-tag v-else-if="row.status !== 1" type="danger" size="small">异常</el-tag>
          <el-tag v-else type="success" size="small">成功</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadLogs"
      />
    </div>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
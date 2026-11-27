<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  kickAdminUser,
  listAdminUsers,
  patchAdminUserStatus,
  putAdminUserAiLimit,
  putAdminUserRoles,
} from '@/api/admin'
import type { AdminUserVO } from '@/types/admin'
import { formatDateTime } from '@/utils/date'

const auth = useAuthStore()
const currentUserId = computed(() => auth.user?.id)

const loading = ref(false)
const users = ref<AdminUserVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const statusFilter = ref<'all' | '0' | '1'>('all')

const roleDialog = ref(false)
const roleTarget = ref<AdminUserVO | null>(null)
const roleSelection = ref<string[]>([])
const roleSaving = ref(false)

const limitDialog = ref(false)
const limitTarget = ref<AdminUserVO | null>(null)
const limitValue = ref(50)
const limitSaving = ref(false)

async function loadUsers() {
  loading.value = true
  try {
    const params: {
      pageNum: number
      pageSize: number
      keyword?: string
      status?: 0 | 1
    } = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (statusFilter.value !== 'all') params.status = Number(statusFilter.value) as 0 | 1
    const res = await listAdminUsers(params)
    users.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  pageNum.value = 1
  void loadUsers()
}

function isSelf(row: AdminUserVO) {
  return row.id === currentUserId.value
}

async function onStatusChange(row: AdminUserVO, next: boolean) {
  if (isSelf(row)) return
  const status = next ? 1 : 0
  const prev = row.status
  row.status = status as 0 | 1
  try {
    const res = await patchAdminUserStatus(row.id, { status: status as 0 | 1 })
    const idx = users.value.findIndex((u) => u.id === row.id)
    if (idx >= 0) users.value[idx] = res.data
    ElMessage.success(status === 1 ? '已启用' : '已禁用')
  } catch {
    row.status = prev
  }
}

function openRoles(row: AdminUserVO) {
  roleTarget.value = row
  roleSelection.value = [...row.roles]
  roleDialog.value = true
}

async function saveRoles() {
  if (!roleTarget.value) return
  roleSaving.value = true
  try {
    const res = await putAdminUserRoles(roleTarget.value.id, { roleCodes: roleSelection.value })
    const idx = users.value.findIndex((u) => u.id === roleTarget.value!.id)
    if (idx >= 0) users.value[idx] = res.data
    ElMessage.success('角色已更新')
    roleDialog.value = false
  } finally {
    roleSaving.value = false
  }
}

function openLimit(row: AdminUserVO) {
  limitTarget.value = row
  limitValue.value = row.aiDailyLimit
  limitDialog.value = true
}

async function saveLimit() {
  if (!limitTarget.value) return
  limitSaving.value = true
  try {
    const res = await putAdminUserAiLimit(limitTarget.value.id, {
      aiDailyLimit: limitValue.value,
    })
    const idx = users.value.findIndex((u) => u.id === limitTarget.value!.id)
    if (idx >= 0) users.value[idx] = res.data
    ElMessage.success('AI 配额已更新')
    limitDialog.value = false
  } finally {
    limitSaving.value = false
  }
}

async function kickUser(row: AdminUserVO) {
  try {
    await ElMessageBox.confirm(`确定强制下线用户「${row.username}」？`, '强制下线', {
      type: 'warning',
    })
    await kickAdminUser(row.id)
    ElMessage.success('已强制下线')
  } catch {
    /* cancelled */
  }
}

onMounted(loadUsers)
</script>

<template>
  <div class="admin-page page-shell">
    <div class="filter-bar">
      <el-input
        v-model="keyword"
        clearable
        placeholder="用户名 / 昵称 / 邮箱"
        style="width: 260px"
        @keyup.enter="onSearch"
      />
      <el-select v-model="statusFilter" style="width: 120px" @change="onSearch">
        <el-option label="全部" value="all" />
        <el-option label="正常" value="1" />
        <el-option label="禁用" value="0" />
      </el-select>
      <el-button type="primary" @click="onSearch">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="users" stripe border class="user-table">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="username" label="用户名" min-width="100" />
      <el-table-column prop="nickname" label="昵称" min-width="100" />
      <el-table-column prop="email" label="邮箱" min-width="140" show-overflow-tooltip />
      <el-table-column label="角色" min-width="120">
        <template #default="{ row }">
          <el-tag v-for="r in row.roles" :key="r" size="small" style="margin-right: 4px">
            {{ r }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="aiDailyLimit" label="AI 日配额" width="100" align="center" />
      <el-table-column label="状态" width="88" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            :disabled="isSelf(row)"
            @change="(v: boolean) => onStatusChange(row, v)"
          />
        </template>
      </el-table-column>
      <el-table-column label="注册时间" min-width="150">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openRoles(row)">角色分配</el-button>
          <el-button link type="primary" @click="openLimit(row)">调整 AI 配额</el-button>
          <el-button link type="danger" :disabled="isSelf(row)" @click="kickUser(row)">
            强制下线
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadUsers"
      />
    </div>

    <el-dialog v-model="roleDialog" title="角色分配" width="400px" destroy-on-close>
      <p v-if="roleTarget" class="dialog-hint">
        用户：{{ roleTarget.username }}
        <el-text v-if="isSelf(roleTarget)" type="warning">（不可移除自身 ADMIN）</el-text>
      </p>
      <el-checkbox-group v-model="roleSelection">
        <el-checkbox value="USER">USER</el-checkbox>
        <el-checkbox value="ADMIN" :disabled="roleTarget ? isSelf(roleTarget) : false">ADMIN</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialog = false">取消</el-button>
        <el-button type="primary" :loading="roleSaving" @click="saveRoles">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="limitDialog" title="调整 AI 配额" width="400px" destroy-on-close>
      <p v-if="limitTarget" class="dialog-hint">用户：{{ limitTarget.username }}</p>
      <el-input-number v-model="limitValue" :min="0" :max="10000" style="width: 100%" />
      <template #footer>
        <el-button @click="limitDialog = false">取消</el-button>
        <el-button type="primary" :loading="limitSaving" @click="saveLimit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.user-table {
  border-radius: 8px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.dialog-hint {
  margin: 0 0 12px;
  color: var(--ssp-muted);
}
</style>
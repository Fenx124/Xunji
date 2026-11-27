<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const activeTab = ref('basic')

const profileRef = ref<FormInstance>()
const passwordRef = ref<FormInstance>()
const savingProfile = ref(false)
const savingPassword = ref(false)

const profileForm = reactive({
  nickname: '',
  email: '',
  avatar: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateNewConfirm = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
    return
  }
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
    return
  }
  callback()
}

const profileRules: FormRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 32, message: '昵称不超过 32 位', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [{ validator: validateNewConfirm, trigger: 'blur' }],
}

function syncProfileForm() {
  profileForm.nickname = auth.user?.nickname ?? ''
  profileForm.email = auth.user?.email ?? ''
  profileForm.avatar = auth.user?.avatar ?? ''
}

onMounted(() => {
  syncProfileForm()
})

async function saveProfile() {
  if (!profileRef.value) return
  await profileRef.value.validate(async (valid) => {
    if (!valid) return
    savingProfile.value = true
    try {
      await auth.updateProfile({
        nickname: profileForm.nickname,
        email: profileForm.email || null,
        avatar: profileForm.avatar || null,
      })
      ElMessage.success('资料已更新')
    } finally {
      savingProfile.value = false
    }
  })
}

async function savePassword() {
  if (!passwordRef.value) return
  await passwordRef.value.validate(async (valid) => {
    if (!valid) return
    savingPassword.value = true
    try {
      await auth.updatePassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword,
      })
      ElMessage.success('密码已修改')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      passwordRef.value?.resetFields()
    } finally {
      savingPassword.value = false
    }
  })
}
</script>

<template>
  <div class="page-shell profile-page">
    <h1 class="page-title">个人中心</h1>
    <el-card shadow="never" class="profile-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-form ref="profileRef" :model="profileForm" :rules="profileRules" label-width="88px" class="profile-form">
            <el-form-item label="用户名">
              <el-input :model-value="auth.user?.username" disabled />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="profileForm.nickname" maxlength="32" show-word-limit />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" placeholder="用于日程提醒邮件" clearable />
            </el-form-item>
            <el-form-item label="头像 URL" prop="avatar">
              <el-input v-model="profileForm.avatar" placeholder="可选，图片地址" clearable />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="修改密码" name="password">
          <el-form
            ref="passwordRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="100px"
            class="profile-form"
          >
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingPassword" @click="savePassword">更新密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.page-title {
  margin: 0 0 16px;
  font-size: 1.5rem;
}

.profile-card {
  max-width: 640px;
  border-radius: 12px;
}

.profile-form {
  max-width: 480px;
  padding-top: 8px;
}
</style>
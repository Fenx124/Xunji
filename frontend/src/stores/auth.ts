import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '@/api/auth'
import { getStoredToken, setStoredToken } from '@/utils/request'
import type {
  LoginRequest,
  RegisterRequest,
  UpdatePasswordRequest,
  UpdateProfileRequest,
  UserInfo,
} from '@/types/api'

const USER_KEY = 'smart_study_user'

function loadUserFromStorage(): UserInfo | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserInfo
  } catch {
    return null
  }
}

function saveUserToStorage(user: UserInfo | null): void {
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  } else {
    localStorage.removeItem(USER_KEY)
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getStoredToken())
  const user = ref<UserInfo | null>(loadUserFromStorage())

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.roles?.includes('ADMIN') ?? false)

  function applyAuth(accessToken: string, profile: UserInfo) {
    token.value = accessToken
    user.value = profile
    setStoredToken(accessToken)
    saveUserToStorage(profile)
  }

  async function login(payload: LoginRequest) {
    const res = await authApi.login(payload)
    applyAuth(res.data.accessToken, res.data.user)
    return res.data
  }

  async function register(payload: RegisterRequest) {
    const res = await authApi.register(payload)
    user.value = res.data
    return res.data
  }

  async function fetchMe() {
    const res = await authApi.getMe()
    user.value = res.data
    saveUserToStorage(res.data)
    return res.data
  }

  async function logout() {
    try {
      if (token.value) {
        await authApi.logout()
      }
    } finally {
      clearAuth()
    }
  }

  function clearAuth() {
    token.value = null
    user.value = null
    setStoredToken(null)
    saveUserToStorage(null)
  }

  async function updateProfile(payload: UpdateProfileRequest) {
    const res = await authApi.updateProfile(payload)
    user.value = res.data
    saveUserToStorage(res.data)
    return res.data
  }

  async function updatePassword(payload: UpdatePasswordRequest) {
    await authApi.updatePassword(payload)
  }

  return {
    token,
    user,
    isLoggedIn,
    isAdmin,
    login,
    register,
    fetchMe,
    logout,
    clearAuth,
    updateProfile,
    updatePassword,
  }
})
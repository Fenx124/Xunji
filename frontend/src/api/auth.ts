import request from '@/utils/request'
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  Result,
  UpdatePasswordRequest,
  UpdateProfileRequest,
  UserInfo,
} from '@/types/api'

export function register(data: RegisterRequest) {
  return request.post<unknown, Result<UserInfo>>('/auth/register', data)
}

export function login(data: LoginRequest) {
  return request.post<unknown, Result<LoginResponse>>('/auth/login', data)
}

export function logout() {
  return request.post<unknown, Result<null>>('/auth/logout')
}

export function getMe() {
  return request.get<unknown, Result<UserInfo>>('/auth/me')
}

export function updateProfile(data: UpdateProfileRequest) {
  return request.put<unknown, Result<UserInfo>>('/user/profile', data)
}

export function updatePassword(data: UpdatePasswordRequest) {
  return request.put<unknown, Result<null>>('/user/password', data)
}
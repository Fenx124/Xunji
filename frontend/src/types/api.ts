/** 统一响应结构，对齐 docs/api-contract.md §1.1 */
export interface Result<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

/** 标准分页结构，对齐 docs/api-contract.md §1.2 */
export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

/** 当前登录用户基础信息 */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string | null
  avatar: string | null
  roles: string[]
  aiDailyLimit: number
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  nickname: string
  email?: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: UserInfo
}

export interface UpdateProfileRequest {
  nickname: string
  email?: string | null
  avatar?: string | null
}

export interface UpdatePasswordRequest {
  oldPassword: string
  newPassword: string
}
# 前后端接口契约（API Contract v1.0）

> 本文件是 `backend/` 与 `frontend/` 并行开发的**单一事实来源**。任何字段命名、URL 或响应格式必须严格遵守本契约。

---

## 1. 全局规范

- **Base URL**：`/api/v1`
- **字符编码与格式**：UTF-8，`application/json`（流式 AI 问答使用 `text/event-stream`）
- **时间格式**：统一使用 `yyyy-MM-dd HH:mm:ss`（时区 `Asia/Shanghai`）
- **认证头**：`Authorization: Bearer <accessToken>`
- **权限框架**：Spring Security 6 + JWT + Redis

### 1.1 统一响应结构 `Result<T>`

```json
{
  "code": 200,
  "message": "OK",
  "data": {},
  "timestamp": "2026-11-20 21:00:00"
}
```

### 1.2 标准分页结构 `PageResult<T>`

```json
{
  "records": [],
  "total": 42,
  "pageNum": 1,
  "pageSize": 10
}
```

### 1.3 标准业务错误码

| `code` | HTTP Status | 含义 |
| :--- | :--- | :--- |
| `200` | 200 | 成功 |
| `40001` | 400 | 请求参数校验失败 |
| `40101` | 401 | 未登录 / Token 无效 / 已注销或被管理员强制下线 |
| `40301` | 403 | 无权限访问（如普通用户访问管理员接口或越权访问他人数据） |
| `40401` | 404 | 目标资源不存在 |
| `40901` | 409 | 业务冲突（如用户名重复、乐观锁版本冲突） |
| `42901` | 429 | 触发 Redis 限流或 AI 今日配额已用尽 |
| `50001` | 500 | 服务器内部异常 |
| `50301` | 503 | 外部 AI 服务超时或不可用（已触发降级） |

### 1.4 Redis Key 命名规范（后端统一使用）

| Key 模式 | 数据类型 | TTL | 用途 |
| :--- | :--- | :--- | :--- |
| `auth:blacklist:{jti}` | String | Token 剩余有效期 | 主动登出的 JWT 黑名单 |
| `auth:revoked:{userId}` | String | 24h | 管理员强制踢人下线的撤销时间戳 |
| `note:detail:{noteId}` | String (JSON) | 15m | 热点笔记详情 Cache-Aside 缓存 |
| `dashboard:overview:{userId}` | String (JSON) | 5m | 首页学习统计看板缓存 |
| `ai:rate:min:{userId}` | String (计数器) | 60s | AI 每分钟防刷限流（默认 6 次/分钟） |
| `ai:quota:day:{userId}:{yyyyMMdd}` | String (计数器) | 25h | AI 每日配额计数（受 `sys_user.ai_daily_limit` 控制） |

---

## 2. M1 认证与个人中心（P0）

| 方法 | 路径 | 权限 | 说明 |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/register` | 公开 | 用户注册（用户名 3–50 位，仅字母、数字、下划线；默认赋予 `USER` 角色） |
| `POST` | `/auth/login` | 公开 | 登录，返回 `accessToken` 与用户基础信息（含 `roles`） |
| `POST` | `/auth/logout` | 登录 | 注销，将当前 JWT 的 `jti` 写入 Redis 黑名单 |
| `GET` | `/auth/me` | 登录 | 获取当前登录用户信息与角色列表 |
| `PUT` | `/user/profile` | 登录 | 更新昵称、邮箱、头像 |
| `PUT` | `/user/password` | 登录 | 修改密码（校验旧密码） |

**`POST /auth/login` 响应 `data` 示例**：
```json
{
  "accessToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": 1,
    "username": "student01",
    "nickname": "小明",
    "email": "student@example.com",
    "avatar": null,
    "roles": ["USER"],
    "aiDailyLimit": 50
  }
}
```

---

## 3. M2 笔记中心（P0）

### 3.1 笔记本
| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| `GET` | `/notebooks` | 获取当前用户全部笔记本，返回 `NotebookVO[]`：`{ id, name, description, icon, sortOrder, noteCount, createdAt, updatedAt }` |
| `POST` | `/notebooks` | 新建笔记本 `{ name, description?, icon?, sortOrder? }` → `NotebookVO` |
| `PUT` | `/notebooks/{id}` | 修改笔记本 `{ name, description?, icon?, sortOrder? }` → `NotebookVO` |
| `DELETE` | `/notebooks/{id}` | 逻辑删除笔记本（事务内将其下笔记的 `notebook_id` 置空）→ `null` |

### 3.2 标签
| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| `GET` | `/tags` | 获取当前用户全部标签，返回 `TagVO[]`：`{ id, name, color, noteCount, createdAt }` |
| `POST` | `/tags` | 新建标签 `{ name, color? }` → `TagVO` |
| `PUT` | `/tags/{id}` | 修改标签 `{ name, color? }` → `TagVO` |
| `DELETE` | `/tags/{id}` | 删除标签（级联解除 `note_tag` 关联）→ `null` |

### 3.3 笔记与回收站
| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| `GET` | `/notes` | 分页查询笔记（query: `notebookId`, `tagId`, `keyword`[MySQL `FULLTEXT` ngram], `isFavorite`, `pageNum=1`, `pageSize=20`）→ `PageResult<NoteListItemVO>` |
| `GET` | `/notes/{id}` | 获取笔记详情（含 `tags` 与 `relatedSchedules`）。未删除笔记走 Redis Cache-Aside `note:detail:{noteId}`；回收站笔记直接读库且不写入该缓存 → `NoteDetailVO` |
| `POST` | `/notes` | 新建笔记 `{ notebookId?, title, content, tagIds?, isPinned?, isFavorite? }` → `NoteDetailVO` |
| `PUT` | `/notes/{id}` | 更新笔记 `{ notebookId?, title, content, tagIds?, isPinned?, isFavorite?, version }`（带 `version` 乐观锁校验，冲突返 `40901`；事务更新 `note_tag` 并删除 Redis 缓存）→ `NoteDetailVO` |
| `PATCH` | `/notes/{id}/pin` | 切换置顶状态（可选体 `{ isPinned?: boolean }`，缺省则取反）→ `{ id, isPinned, isFavorite }` |
| `PATCH` | `/notes/{id}/favorite` | 切换收藏状态（可选体 `{ isFavorite?: boolean }`，缺省则取反）→ `{ id, isPinned, isFavorite }` |
| `DELETE` | `/notes/{id}` | 移入回收站（逻辑删除、记录 `deleted_at` 并失效缓存）→ `null` |
| `GET` | `/notes/trash` | 分页查看回收站笔记（query: `pageNum=1`, `pageSize=20`）→ `PageResult<NoteListItemVO>` |
| `POST` | `/notes/{id}/restore` | 从回收站恢复（恢复后 `deleted=0, deleted_at=null`）→ `NoteDetailVO` |
| `DELETE` | `/notes/{id}/permanent` | 彻底物理删除回收站中的笔记 → `null` |

**核心 VO 字段约定**：
- `TagSimpleVO`：`{ id, name, color }`
- `ScheduleBriefVO`：`{ id, title, startTime, status, priority, sourceType }`
- `NoteListItemVO`：`{ id, notebookId, notebookName, title, aiSummary, isPinned, isFavorite, wordCount, version, tags: TagSimpleVO[], deletedAt, createdAt, updatedAt }`（`isPinned`/`isFavorite` 为 `boolean`）
- `NoteDetailVO`：在 `NoteListItemVO` 基础上增加 `content` 与 `relatedSchedules: ScheduleBriefVO[]`

### 3.4 笔记历史版本（P2）

保存笔记时，若标题或正文相对当前库中的值有变化，先把**旧标题和旧正文**写入 `note_history`，再更新笔记。每篇笔记只保留最近 **10** 条快照。只改置顶、收藏或标签时不产生快照。回滚前也会把当前正文再存一条，避免回滚不可逆。

| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| `GET` | `/notes/{id}/history` | 当前用户该笔记的快照列表，按 `versionNo` 降序，最多 10 条 → `NoteHistoryItemVO[]` |
| `GET` | `/notes/{id}/history/{historyId}` | 单条快照全文，供前端做差异对比 → `NoteHistoryDetailVO` |
| `POST` | `/notes/{id}/history/{historyId}/restore` | 用该快照覆盖当前标题和正文，笔记 `version` 加 1，并失效笔记详情缓存 → `NoteDetailVO` |

**字段约定**：
- `NoteHistoryItemVO`：`{ id, noteId, versionNo, title, changeNote, createdAt }`（列表不含正文）
- `NoteHistoryDetailVO`：`NoteHistoryItemVO` 加上 `content`
- 快照 `versionNo` 等于被替换掉的那一版 `note.version`
- 归属失败：笔记不存在 `40401`，不是当前用户 `40301`，快照不属于该笔记 `40401`

---

## 4. M3 日程中心（P0）

| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| `GET` | `/schedules` | 按时间区间或筛选条件查询（query: `startDate`, `endDate`, `status`, `priority`, `noteId`），供 FullCalendar 与四象限视图共用 → `ScheduleVO[]` |
| `POST` | `/schedules` | 新建日程 `{ title, description?, noteId?, startTime, endTime?, remindTime?, priority? }` → `ScheduleVO` |
| `PUT` | `/schedules/{id}` | 更新日程 `{ title, description?, noteId?, startTime, endTime?, remindTime?, priority?, status? }` → `ScheduleVO` |
| `PATCH` | `/schedules/{id}/status` | 切换状态 `{ status: 0 | 1 | 2 }`（完成时自动写 `completedAt` 并失效看板缓存）→ `ScheduleVO` |
| `DELETE` | `/schedules/{id}` | 逻辑删除日程 → `null` |
| `POST` | `/schedules/ebbinghaus` | **艾宾浩斯一键生成复习日程**：`{ noteId, baseDate? }`（`baseDate` 可选 `yyyy-MM-dd`，默认今天），事务批量生成 `+1/+2/+4/+7/+15` 天 5 条日程（`sourceType='EBBINGHAUS'`）→ `ScheduleVO[]` |
| `GET` | `/schedules/reminders/upcoming` | 获取当前用户临期（未来 24 小时内或已过提醒时间）且未完成的站内提醒列表 → `ScheduleVO[]` |

**`ScheduleVO` 字段约定**：
`{ id, noteId, noteTitle, title, description, startTime, endTime, remindTime, reminded, priority, status, sourceType, completedAt, createdAt, updatedAt }`（`reminded` 为 `boolean`，`priority` 为 `1|2|3|4`，`status` 为 `0|1|2`，`sourceType` 为 `MANUAL|EBBINGHAUS|AI`）

---

## 5. M4 AI 学习助手（P1）

| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| `POST` | `/ai/summarize` | 笔记智能摘要与标签推荐：`{ noteId }` → `AiSummarizeVO`（自动更新 `note.ai_summary`、失效笔记详情缓存并记入 `ai_chat_log`） |
| `POST` | `/ai/qa/stream` | 笔记/学习流式问答（**SSE `text/event-stream`**）：请求 `{ noteId?, question }`，逐帧推送 `data: {"chunk":"...","done":false,"fallback":false}`，结束帧 `{"chunk":"","done":true,"fallback":false,"logId":123,"latencyMs":850}` |
| `POST` | `/ai/parse-schedule` | 自然语言解析日程：`{ text }` → `AiParsedScheduleVO`（前端确认后调 `POST /schedules` 入库，支持传 `sourceType: "AI"`） |
| `GET` | `/ai/logs` | 分页查看当前用户的 AI 交互历史（query: `noteId`, `featureType`, `pageNum=1`, `pageSize=20`）→ `PageResult<AiChatLogVO>` |

**M4 核心 VO 字段约定**：
- `AiSummarizeVO`：`{ noteId, summary, suggestedTags: string[], fallback: boolean, latencyMs }`
- `AiParsedScheduleVO`：`{ title, description, startTime, endTime, remindTime, priority, fallback: boolean }`（时间格式 `yyyy-MM-dd HH:mm:ss`，`priority` 为 `1|2|3|4`）
- `AiChatLogVO`：`{ id, noteId, noteTitle, featureType, provider, modelName, promptText, responseText, promptTokens, completionTokens, latencyMs, success, errorMessage, createdAt }`
- 限流保护：受 Redis 每分钟频次（`ai:rate:min:{userId}`）与每日配额（`ai:quota:day:{userId}:{yyyyMMdd}`，联动 `sys_user.ai_daily_limit`）控制，超限统一返回 HTTP 429 / `code: 42901`。
- 容错降级：当 `DEEPSEEK_API_KEY` 未配置或外部接口超时且开启 `app.ai.fallback-enabled=true` 时，自动切换至本地规则降级策略并标记 `fallback: true`；关闭降级时返回 HTTP 503 / `code: 50301`。

---

## 6. M5 番茄钟专注模块（P1）

| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| `POST` | `/focus/records` | 上报一次专注记录 `{ noteId?, scheduleId?, plannedMinutes, actualMinutes, status, startedAt, endedAt, remark? }`（`status`: `COMPLETED` / `ABORTED`，失效看板缓存）→ `FocusRecordVO` |
| `GET` | `/focus/records` | 分页查询历史专注记录（query: `startDate`, `endDate`, `pageNum=1`, `pageSize=20`）→ `PageResult<FocusRecordVO>` |
| `GET` | `/focus/stats/today` | 获取今日专注统计 → `FocusTodayStatsVO`：`{ completedCount, totalMinutes, totalSessions }` |

**`FocusRecordVO` 字段约定**：
`{ id, noteId, noteTitle, scheduleId, scheduleTitle, plannedMinutes, actualMinutes, status, startedAt, endedAt, remark, createdAt }`

---

## 7. M7 首页学习数据看板（P1）

| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| `GET` | `/dashboard/overview` | 综合统计看板（走 Redis Cache-Aside `dashboard:overview:{userId}`，TTL 5m）→ `DashboardOverviewVO` |

**`DashboardOverviewVO` 字段约定**：
```json
{
  "summary": {
    "totalNotes": 12,
    "todayPendingSchedules": 3,
    "monthCompletionRate": 80.5,
    "weekFocusMinutes": 175,
    "todayFocusMinutes": 50
  },
  "heatmap": [
    { "date": "2026-11-20", "count": 5 }
  ],
  "weeklyTrend": [
    { "date": "2026-11-14", "notesUpdated": 2, "schedulesCompleted": 3, "focusMinutes": 50 }
  ],
  "notebookDistribution": [
    { "name": "数据库原理", "count": 6 }
  ],
  "tagDistribution": [
    { "name": "MySQL", "color": "#409EFF", "count": 4 }
  ]
}
```

---

## 8. M8 管理员治理后台（P1，仅 `ADMIN` 角色可访问）

| 方法 | 路径 | 说明 |
| :--- | :--- | :--- |
| `GET` | `/admin/users` | 分页查询用户列表（query: `keyword`, `status`, `pageNum=1`, `pageSize=20`）→ `PageResult<AdminUserVO>` |
| `PATCH` | `/admin/users/{id}/status` | 启用/禁用用户 `{ status: 0 \| 1 }`（禁用时自动写 `auth:revoked:{userId}` 强制失效；禁止禁用自己）→ `AdminUserVO` |
| `PUT` | `/admin/users/{id}/roles` | 分配角色 `{ roleCodes: ["USER", "ADMIN"] }`（禁止移除自身 `ADMIN` 角色）→ `AdminUserVO` |
| `PUT` | `/admin/users/{id}/ai-limit` | 修改该用户的 AI 每日配额 `{ aiDailyLimit: 100 }`（`0..10000`）→ `AdminUserVO` |
| `POST` | `/admin/users/{id}/kick` | 强制下线（向 Redis 写入 `auth:revoked:{userId}` 当前秒级时间戳，TTL 24h）→ `null` |
| `GET` | `/admin/ai-stats` | AI 用量统计（query: `days=7`，范围 `1..90`）→ `AdminAiStatsVO` |
| `GET` | `/admin/audit-logs` | 分页查询 AOP 系统审计日志（query: `module`, `username`, `status`, `pageNum=1`, `pageSize=20`）→ `PageResult<AuditLogVO>` |

**核心后台 VO 字段约定**：
- `AdminUserVO`：`{ id, username, nickname, email, status, roles: string[], aiDailyLimit, createdAt }`
- `AuditLogVO`：`{ id, userId, username, module, action, httpMethod, requestUri, clientIp, costMs, status, errorMsg, createdAt }`
- `AdminAiStatsVO`：
  ```json
  {
    "totalCalls": 0,
    "successCalls": 0,
    "successRate": 0.0,
    "avgLatencyMs": 0,
    "totalTokens": 0,
    "byFeature": [{ "featureType": "NOTE_QA", "calls": 0, "avgLatencyMs": 0 }],
    "byUser": [{ "userId": 1, "username": "admin", "nickname": "管理员", "calls": 0, "todayCalls": 0, "aiDailyLimit": 50 }],
    "dailyTrend": [{ "date": "2026-11-20", "calls": 0, "successCalls": 0 }]
  }
  ```

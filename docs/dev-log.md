# 项目开发与架构决策日志（Dev Log）

> 用于持续记录项目推进进度、关键架构决策（ADR，可直接复用到《软件体系结构分析》报告）、技术踩坑与范围变更记录。

---

## 日志记录模板（后续每日/每阶段复制追加）

```markdown
## YYYY-MM-DD | 阶段名称
### 今日完成
- [x] 具体事项 1
- [x] 具体事项 2

### 关键决策 / 变更记录
- **决策/变更**：
- **原因与权衡**：

### 遇到的问题与解决方案
- **问题**：
- **解决**：

### 下一步计划
- [ ] 待办 1
```

---

## 2026-11-20 | 项目立项与需求范围冻结

### 今日完成
- [x] 完成项目可行性分析，明确项目同时服务于 Java Web、数据库及软件体系结构分析课程目标。
- [x] 结合“单人开发、3–4 周周期、具备 Vue 3 基础、无硬核偏门考点”的条件，完成功能范围裁剪与优先级划分。
- [x] 确定核心功能闭环：笔记整理 + 日程管理（含艾宾浩斯复习计划与笔记双向关联）+ AI 学习助手 + **番茄钟专注模块** + 数据看板 + 管理员后台。
- [x] 明确将**笔记历史版本快照**划定为 P2 后期可选扩展项，防止前期开发周期失控。
- [x] 创建项目专属目录 `smart-study-platform` 及 `docs/requirements.md`、`docs/dev-log.md`。
- [x] 将当前会话工作区根目录迁移至 `D:\CursorWorkSpace\smart-study-platform`，完成 Git 仓库初始化（`main` 分支）与 `.gitignore` 配置。
- [x] 构建多代理（主指挥 + 子代理）协作基础设施：编写 `AGENTS.md` 目录边界协议与 `.cursor/rules/` 持久化规则（全局约束、后端 Java 规范、前端 Vue 规范），并预建 `sql/`、`docker/`、`backend/`、`frontend/` 标准目录骨架。

### 关键架构决策记录（ADR）

- **ADR-001：采用“前后端分离 + 分层模块化单体（Modular Monolith）”而非微服务**
  - **背景**：单人 3–4 周交付，且需兼顾三门课程展示。
  - **决策**：后端使用单一 Spring Boot 3 应用，按业务领域严格划分包边界（`auth`、`note`、`schedule`、`focus`、`ai`、`system`）。
  - **权衡**：避免微服务带来的分布式事务、注册中心运维与本地资源开销，同时保持模块高内聚低耦合，为体系结构报告提供“架构演进至微服务”的合理切入点。

- **ADR-002：明确 MySQL 与 Redis 的混合存储职责（纠正 RDBMS/NoSQL 概念边界）**
  - **背景**：项目同时引入 MySQL 和 Redis，需避免“为用而用”。
  - **决策**：MySQL 作为关系型数据库承担强一致主存储、多对多关联、事务控制与 `FULLTEXT` 全文检索；Redis 作为非关系型内存数据库承担 JWT 注销/登录态管理、Cache-Aside 热点与看板缓存、AI 接口限流。
  - **权衡**：在保证数据一致性的同时提升高频读取与防刷保护能力，契合数据库课程的混合持久化与缓存一致性分析要点。

- **ADR-003：AI 模块采用适配器/策略模式解耦外部厂商，并使用 SSE 流式响应**
  - **背景**：外部大模型 API 存在响应慢（3–15s）、厂商接口变动及偶发超时风险。
  - **决策**：封装统一的 `AiProvider` 接口屏蔽底层模型差异；问答场景采用 SSE 流式推送；配置超时与降级兜底策略，确保 AI 故障不影响笔记、日程与番茄钟等核心功能可用性。
  - **权衡**：直接体现软件体系结构中的“可修改性”与“可用性/容错”质量属性战术。

- **ADR-004：范围红线与功能优先级分层**
  - **决策**：保留高性价比、能补齐学习闭环的**番茄钟**进入 P1；将**笔记历史版本**归入 P2 后期可选；明确排除多人实时协同、社区互动、全库向量 RAG 等高复杂度特性。
  - **权衡**：确保第 2 周末产出可演示保底版，第 3 周完成全部亮点，第 4 周留出充足时间完成架构文档与答辩准备。

### 下一步计划
- [x] 设计并输出完整的 MySQL 8.0 建表 DDL 脚本（`sql/schema.sql`，涵盖 11 张核心表及 `note_history` 预留表、中文 `ngram` 全文索引与复合索引）。
- [x] 搭建 `docker/docker-compose.yml` 与 `.env.example`（本地一键启动 MySQL 8.0 + Redis 7）。
- [x] 输出前后端统一接口契约 `docs/api-contract.md`（含统一响应体、错误码、Redis Key 规范与 P0/P1 全模块端点）。
- [x] 并行派发子代理（Grok 4.7 High Fast / Composer 2.5 Fast）初始化后端（Spring Boot 3.5.16 + Spring Security 6 + JWT + MyBatis-Plus 3.5.16 + Redis + OpenAPI 3）与前端（Vue 3 + Vite + Pinia + Vue Router + Element Plus）工程骨架，完成 M1 认证与个人中心全部 6 个接口及页面，`mvn test`（5/5）与 `npm run build`（`vue-tsc` + Vite）均一次性通过，且主指挥已完成前后端 DTO/VO 契约一致性审查。
- [x] 完成 M1 本地全链路真机联调（MySQL 8.0 导入 12 张表 + 本地 Redis 鉴权黑名单 + Spring Boot 8080 + Vite 5173 代理）：9 项端到端测试（OpenAPI、管理员初始化登录、注册、普通用户登录、资料更新、修改密码、RBAC 40301 拦截、注销与 Redis JWT 黑名单 40101 生效）全部通过，零代码缺陷。
- [x] 并行完成 P0 核心业务闭环 **M2 笔记中心 + M3 日程中心**：
  - 后端（Grok 4.7 High Fast）：落地 `note` 与 `schedule` 两大业务域（严格通过只读 Service 接口跨模块解耦，引入 `AfterCommit` 事务提交后失效 Redis 缓存、`note:detail:{noteId}` Cache-Aside、MySQL 8.0 `ngram` 中文全文检索、乐观锁 `version` 40901 校验、回收站逻辑删除与物理清除、艾宾浩斯 `+1/+2/+4/+7/+15` 天批量生成复习日程），`mvn clean test` 编译 96 源文件、**23/23 单元测试全部通过**。
  - 前端（Composer 2.5 Fast）：接入 `md-editor-v3` 与 `FullCalendar`，完成三栏 Markdown 笔记工作台（笔记本/标签 CRUD、检索、回收站、`.md` 导入导出、一键生成艾宾浩斯、`?noteId=` 深链跳转）、日程中心（日历/四象限/今日待办三视图、临期提醒抽屉、笔记双向跳转）及顶栏提醒铃铛，`npm run build`（`vue-tsc` + Vite）一次性通过。
  - 主指挥已复核前后端 `NoteController` / `ScheduleController` 与 `src/api/note.ts` / `schedule.ts` 契约完全对齐。
- [x] 完成第三轮并行开发（**M5 番茄钟 + M6 定时邮件提醒 + M7 首页 ECharts 数据看板 + M8 管理员后台与 AOP 审计日志**）：
  - 后端（Grok 4.7 High Fast）：落地 `focus`、`schedule/job/ScheduleReminderJob`、`dashboard`（Redis Cache-Aside 5 分钟缓存）、`common/audit`（`@AuditLog` + `AuditLogAspect`）与 `system`（`/api/v1/admin/**`），单元测试增至 **36/36 通过**。
  - 前端（Composer 2.5 Fast）：引入 `echarts`，完成番茄钟专注页（SVG 环形倒计时 + 关联笔记/日程 + 专注历史）、4 面板学习统计看板（90 天热力图、7 天趋势、笔记本分布、标签分布）及管理员用户管理/AI 用量监控/AOP 审计日志页，`npm run build` 零错误通过。
- [x] 完成 **M1 + M2 + M3 + M5 + M7 + M8 全链路真机联调**（MySQL 8.0 + Redis + Spring Boot 8080 + Vite 5173 代理）：
  - 实测通过：`ngram` 中文全文检索与单字 `LIKE` 降级、`note:detail:{noteId}` 与 `dashboard:overview:{userId}` Redis 缓存、乐观锁 `40901` 冲突拦截、回收站恢复与物理删除、艾宾浩斯 5 条复习日程双向关联、番茄钟入库与看板汇总、AOP 多模块审计日志记录、管理员强制踢人下线 `40101` 生效。
  - 联调捕获并修复真实缺陷：`GET /notes/trash` 在笔记未归类（`notebook_id = null`）时调用 `Map.of().get(null)` 抛出 NPE，已修复 `NotebookNames.lookup` 并补充单测回归通过。
- [x] 完成第四轮并行开发与真机联调（**M4 AI 学习助手**）：
  - 后端（Grok 4.7 High Fast）：基于策略/适配器模式实现 `AiProvider`（`OpenAiCompatibleAiProvider` 兼容 DeepSeek/OpenAI + `LocalFallbackAiProvider` 本地规则降级 + `AiProviderRouter`）、`@AiRateLimit`（Redis 每分钟频次 + 联动 `sys_user.ai_daily_limit` 每日配额）、`SseEmitter` 流式问答（放行 `DispatcherType.ASYNC`）与 4 个 `/api/v1/ai` 接口，`mvn clean test` **51/51 单元测试全部通过**。
  - 前端（Composer 2.5 Fast）：封装 Fetch + `ReadableStream` + `AbortController` 的 `streamPostSse`，落地完整 `AiView`（笔记 SSE 流式问答、自然语言一句话建日程、历史记录抽屉），并打通笔记编辑器（AI 摘要卡片 + 一键添加推荐标签 + 跳 AI 问答）与日程中心（AI 一句话建日程弹窗），`npm run build` 零错误通过。
  - M4 真机端到端联调 5/5 一次性通过（摘要持久化、中文日程解析与 `sourceType=AI` 入库、Vite 代理下 13 帧 SSE 流式输出、学生日志与管理员 AI 统计联动、管理员下调配额后 Redis 限流精准拦截 HTTP 429 / `42901`）。
- [x] 收官文档与启动脚本（2026-11-27）：
  - [x] `docs/architecture.md`：C4 上下文/容器、模块化单体包边界、JWT、Cache-Aside、AI SSE 时序、质量属性战术与 ADR-001～007。
  - [x] `docs/database-design.md`：E-R、3NF 与有意反范式、索引与事务、Redis 一致性；配套 `sql/explain-lab.sql`（约 10000 行后的三组 `EXPLAIN`，需在本机手动执行并截图）。
  - [x] `scripts/start-local.ps1`、`scripts/stop-local.ps1` 与 `docs/demo.md`（8 分钟答辩顺序）。
  - [ ] （可选 P2）**O1 笔记历史版本快照**仍未做。`note_history` 只建了表，需要时再实现。

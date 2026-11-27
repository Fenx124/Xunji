# 循记（Xunji）

个人学习闭环：把笔记记下来，排进复习日程，用番茄钟做完，再在看板上看见这段时间学了什么。面向 Java Web、数据库和软件体系结构课程的单人全栈项目，后端是分层模块化单体，不拆微服务。

```text
笔记  →  日程与复习  →  番茄钟  →  学习看板
  ▲                              │
  └──────── AI 摘要 / 问答 / 一句话建日程 ─┘
```

## 功能

| 模块 | 能做什么 |
| :--- | :--- |
| 账号与权限 | 注册登录、JWT、退出后 Token 失效、用户 / 管理员两级权限、个人资料与改密 |
| 笔记 | Markdown 编辑、笔记本、标签、全文检索、回收站、导入导出、最近 10 条历史版本与恢复 |
| 日程 | 月 / 周 / 日、四象限、今日待办；笔记和日程互相跳转；一键生成 1 / 2 / 4 / 7 / 15 天复习计划 |
| 番茄钟 | 专注计时，可绑笔记或日程，完成后写入专注记录 |
| 看板 | 近 90 天活跃热力图、完成率、笔记本与标签分布、专注时长 |
| AI | 笔记摘要与标签、针对当前笔记的流式问答、一句话解析成日程。未配置密钥时走本地降级 |
| 管理后台 | 用户启停与踢下线、AI 用量、审计日志 |

不做多人协同、社区、全库向量检索和微服务拆分。笔记插图对象存储和重复日程也不在本期范围内。

## 技术栈

| 层 | 选型 |
| :--- | :--- |
| 后端 | Java 21、Spring Boot 3.5、Spring Security、MyBatis-Plus |
| 数据 | MySQL 8.0（事务、范式、ngram 全文索引）、Redis（登录态、Cache-Aside、AI 限流） |
| 前端 | Vue 3、Vite、TypeScript、Pinia、Element Plus、md-editor-v3、FullCalendar、ECharts |
| 接口 | REST + SSE，OpenAPI 在 `http://127.0.0.1:8080/swagger-ui.html` |

业务按 `auth`、`note`、`schedule`、`focus`、`ai`、`dashboard`、`system` 分包。跨模块只通过对方的 Service 接口访问，不直接调用别人的 Mapper。

## 本地启动

需要 JDK 21+、Maven、Node.js、本机 MySQL 8.0。Redis 可由启动脚本拉起。

```powershell
# 1. 导入表结构（库名 smart_study）
mysql -u root -p < sql/schema.sql

# 2. 准备环境变量。口令、JWT、API Key 只放在 .env，不要提交
copy .env.example .env

# 3. 安装前端依赖（首次）
cd frontend
npm install
cd ..

# 4. 启动 Redis、后端和前端
powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1
```

浏览器打开 [http://127.0.0.1:5173](http://127.0.0.1:5173) 或 [http://localhost:5173](http://localhost:5173)。管理员账号在 `.env` 的 `INIT_ADMIN_USERNAME` / `INIT_ADMIN_PASSWORD`，示例文件里的默认值是 `admin` / `Admin@123456`。

停止：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-local.ps1
```

脚本里的 Redis 路径是 `D:\Redis\Redis-x64-5.0.14.1`。本机 Redis 不在这个位置时，先自己启动 Redis，或改 `scripts/start-local.ps1` 里的路径。MySQL 和 Redis 也可以用 `docker/docker-compose.yml` 只起中间件，应用仍在本机用 Maven 和 npm 跑。

`DEEPSEEK_API_KEY` 留空时，AI 功能返回本地降级结果，笔记和日程照常使用。`MAIL_ENABLED=false` 时，到期提醒只记日志。

分开调试时：

```powershell
mvn -f backend/pom.xml spring-boot:run
cd frontend
npm run dev
```

## 目录

```text
├── backend/          Spring Boot 模块化单体
├── frontend/         Vue 3 前端
├── sql/              schema.sql、explain-lab.sql
├── docker/           MySQL 与 Redis 的 Compose
├── scripts/          本机启停脚本
└── docs/             需求、接口、架构、数据库、演示步骤
```

## 文档

| 文档 | 内容 |
| :--- | :--- |
| [docs/requirements.md](docs/requirements.md) | 需求范围和明确不做的事 |
| [docs/architecture.md](docs/architecture.md) | 结构、质量属性和架构决策 |
| [docs/database-design.md](docs/database-design.md) | 表、索引和 EXPLAIN 实验 |
| [docs/api-contract.md](docs/api-contract.md) | 前后端接口契约 |
| [docs/demo.md](docs/demo.md) | 答辩时的演示顺序 |
| [docs/dev-log.md](docs/dev-log.md) | 进度和决策记录 |

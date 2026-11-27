# 循记（Xunji）

面向 Java Web、数据库原理及软件体系结构分析课程的单人全栈实战项目（周期 3–4 周）。

## 目录结构

```text
smart-study-platform/
├── AGENTS.md                 # 多代理（主指挥 + 子代理）协作协议与目录边界
├── .cursor/rules/            # Cursor 持久化项目规则（全局 / 后端 / 前端）
├── docs/
│   ├── requirements.md       # 已冻结的需求规格与优先级清单（范围红线）
│   ├── api-contract.md       # 前后端接口契约
│   ├── architecture.md       # 软件体系结构分析（C4、时序、质量属性、ADR）
│   ├── database-design.md    # 数据库设计、范式与索引实验说明
│   ├── demo.md               # 本地启动与答辩演示顺序
│   └── dev-log.md            # 开发进度与架构决策日志（ADR）
├── scripts/                  # start-local.ps1 / stop-local.ps1
├── sql/                      # schema.sql 与 explain-lab.sql
├── docker/                   # Docker Compose 本地编排（MySQL + Redis）
├── backend/                  # Spring Boot 3 + MyBatis-Plus + Redis + AI 后端
└── frontend/                 # Vue 3 + Vite + Pinia + Element Plus 前端
```

## 快速入口

- 需求边界：`docs/requirements.md`
- 体系结构分析：`docs/architecture.md`
- 数据库设计与 `EXPLAIN` 实验：`docs/database-design.md`
- 启动与答辩步骤：`docs/demo.md`
- 开发进展：`docs/dev-log.md`

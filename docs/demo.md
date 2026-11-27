# 本地启动与答辩演示流程

演示账号来自根目录 `.env` 的 `INIT_ADMIN_USERNAME` / `INIT_ADMIN_PASSWORD`（默认用户名 `admin`）。不要把真实口令写进幻灯片。

## 1. 启动

1. 确认 Windows 服务 `MySQL80` 正在运行，且已执行过 `sql/schema.sql`。
2. 根目录有 `.env`（由 `.env.example` 复制，`MYSQL_ROOT_PASSWORD` 为本机 root 口令）。
3. 在仓库根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1
```

4. 看到后端窗口出现启动完成、前端窗口出现 `Local: http://localhost:5173/` 后，浏览器打开 `http://127.0.0.1:5173`。
5. 结束演示：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-local.ps1
```

接口文档：`http://127.0.0.1:8080/swagger-ui.html`。

若要改用真实大模型，在 `.env` 填写 `DEEPSEEK_API_KEY` 后重启后端。不填则 AI 走本地规则，页面上会标出降级，笔记和日程不受影响。邮件同理：`MAIL_ENABLED=false` 时到期提醒只记日志。

## 2. 建议的 8 分钟演示顺序

按这个顺序讲，能把权限、数据库、缓存和 AI 结构一次走完。

1. **登录与权限**  
   用管理员登录。打开管理后台里的用户、AI 用量、审计日志，说明普通用户路由进不了这些页面。

2. **笔记**  
   新建笔记本和标签，写一篇带两个以上汉字标题的 Markdown。搜索这两个字，说明走 MySQL `ngram` 全文索引。再搜一个单字，说明应用对单字改用 `LIKE`。

3. **复习闭环**  
   在笔记里点生成艾宾浩斯复习计划。切到日程的日历或四象限，点关联笔记跳回去。指出 5 条日程的来源是 `EBBINGHAUS`。

4. **专注与看板**  
   番茄钟选 1 分钟或直接提前完成，绑定刚才的笔记。回到首页看板，看今日专注分钟、笔记本分布和热力图。说明看板先读 Redis，写入后删除 `dashboard:overview:{userId}`。

5. **AI**  
   笔记上点「AI 摘要与标签」，再进 AI 问答看流式输出，最后用一句话生成日程并确认入库（来源 `AI`）。若未配 Key，指出这是 `AiProvider` 的本地降级，换 Key 不用改业务代码。

6. **安全收尾**  
   退出后再用原请求会失败（JWT 黑名单）。或在用户管理里把测试账号强制下线，说明 Redis 里的撤销时间戳。审计日志里应能看到登录、删笔记、AI 调用等模块名。

## 3. 数据库实验（单独做，不放在现场主路径）

需要优化器截图时再跑 `sql/explain-lab.sql`，步骤写在 `docs/database-design.md` 第 6 节。它会给 `admin` 插入约 10000 条 `LAB:` 笔记。答辩演示前若已跑过，可用脚本末尾的 `DELETE` 清掉，避免列表里全是实验数据。

## 4. 现场若某项不可用

| 现象 | 可以怎么讲 |
| :--- | :--- |
| 没有外网或没有 API Key | AI 仍返回摘要、问答和日程草稿，标记为本地降级 |
| 没有配置邮箱 | 提醒任务把到期日程标为已处理，并写日志，不中断进程 |
| Redis 没起来 | 后端登录以外的鉴权会失败。先看 `start-local.ps1` 是否找到 `D:\Redis\Redis-x64-5.0.14.1` |
| 看板数字没变 | 缓存最多 5 分钟；刚写完的数据会主动删键，刷新即可 |

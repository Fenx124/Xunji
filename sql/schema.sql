-- ============================================================================
-- Smart Study Platform (综合学习辅助平台) - MySQL 8.0 数据库初始化脚本
-- 说明：
--   1. 包含 11 张核心业务表（P0/P1）+ 1 张预留扩展表 note_history（P2）
--   2. 字符集统一使用 utf8mb4 / utf8mb4_0900_ai_ci，引擎统一使用 InnoDB
--   3. 笔记表使用 MySQL 8.0 原生 ngram 全文索引，支持中文关键词检索
--   4. 本文件为数据库结构的单一事实来源（Source of Truth）
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `smart_study`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `smart_study`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 1. 权限与用户模块 (M1 / M8)
-- ============================================================================

DROP TABLE IF EXISTS `sys_user_role`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `sys_user`;

CREATE TABLE `sys_user` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
  `username`       VARCHAR(50)  NOT NULL                  COMMENT '登录用户名（唯一）',
  `password`       VARCHAR(100) NOT NULL                  COMMENT 'BCrypt加密密码',
  `nickname`       VARCHAR(60)  NOT NULL                  COMMENT '用户昵称',
  `email`          VARCHAR(120) DEFAULT NULL              COMMENT '绑定邮箱（用于日程提醒）',
  `avatar`         VARCHAR(255) DEFAULT NULL              COMMENT '头像URL',
  `status`         TINYINT      NOT NULL DEFAULT 1        COMMENT '账号状态：1=正常，0=禁用',
  `ai_daily_limit` INT          NOT NULL DEFAULT 50       COMMENT 'AI每日调用配额上限',
  `deleted`        TINYINT      NOT NULL DEFAULT 0        COMMENT '逻辑删除：0=未删，1=已删',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  UNIQUE KEY `uk_sys_user_email` (`email`),
  KEY `idx_sys_user_status` (`status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表';

CREATE TABLE `sys_role` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色主键ID',
  `role_code`   VARCHAR(32)  NOT NULL                  COMMENT '角色编码：USER / ADMIN',
  `role_name`   VARCHAR(50)  NOT NULL                  COMMENT '角色名称',
  `description` VARCHAR(200) DEFAULT NULL              COMMENT '角色说明',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色表';

CREATE TABLE `sys_user_role` (
  `user_id`    BIGINT   NOT NULL COMMENT '用户ID',
  `role_id`    BIGINT   NOT NULL COMMENT '角色ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_user_role_role_id` (`role_id`),
  CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-角色关联表（RBAC多对多）';

-- ============================================================================
-- 2. 笔记中心模块 (M2)
-- ============================================================================

DROP TABLE IF EXISTS `note_history`;
DROP TABLE IF EXISTS `note_tag`;
DROP TABLE IF EXISTS `tag`;
DROP TABLE IF EXISTS `note`;
DROP TABLE IF EXISTS `notebook`;

CREATE TABLE `notebook` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '笔记本ID',
  `user_id`     BIGINT       NOT NULL                  COMMENT '所属用户ID',
  `name`        VARCHAR(80)  NOT NULL                  COMMENT '笔记本名称',
  `description` VARCHAR(255) DEFAULT NULL              COMMENT '笔记本简介',
  `icon`        VARCHAR(40)  NOT NULL DEFAULT 'Notebook' COMMENT '前端图标标识',
  `sort_order`  INT          NOT NULL DEFAULT 0        COMMENT '排序值（升序）',
  `deleted`     TINYINT      NOT NULL DEFAULT 0        COMMENT '逻辑删除：0=未删，1=已删',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_notebook_user_sort` (`user_id`, `deleted`, `sort_order`),
  CONSTRAINT `fk_notebook_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='笔记本分类表（单层分类）';

CREATE TABLE `note` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '笔记ID',
  `user_id`     BIGINT        NOT NULL                  COMMENT '所属用户ID',
  `notebook_id` BIGINT        DEFAULT NULL              COMMENT '所属笔记本ID（可为空表示未归类）',
  `title`       VARCHAR(200)  NOT NULL                  COMMENT '笔记标题',
  `content`     MEDIUMTEXT    NOT NULL                  COMMENT 'Markdown正文内容',
  `ai_summary`  VARCHAR(1000) DEFAULT NULL              COMMENT 'AI生成摘要（适度反范式冗余）',
  `is_pinned`   TINYINT       NOT NULL DEFAULT 0        COMMENT '是否置顶：0=否，1=是',
  `is_favorite` TINYINT       NOT NULL DEFAULT 0        COMMENT '是否收藏：0=否，1=是',
  `word_count`  INT           NOT NULL DEFAULT 0        COMMENT '正文字数统计',
  `version`     INT           NOT NULL DEFAULT 1        COMMENT '乐观锁版本号',
  `deleted`     TINYINT       NOT NULL DEFAULT 0        COMMENT '逻辑删除（回收站）：0=正常，1=回收站',
  `deleted_at`  DATETIME      DEFAULT NULL              COMMENT '移入回收站时间',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_note_user_list` (`user_id`, `deleted`, `notebook_id`, `is_pinned`, `updated_at`),
  KEY `idx_note_user_fav` (`user_id`, `deleted`, `is_favorite`, `updated_at`),
  FULLTEXT KEY `ft_note_title_content` (`title`, `content`) WITH PARSER ngram,
  CONSTRAINT `fk_note_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_note_notebook` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Markdown笔记主表';

CREATE TABLE `tag` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `user_id`    BIGINT      NOT NULL                  COMMENT '所属用户ID',
  `name`       VARCHAR(40) NOT NULL                  COMMENT '标签名',
  `color`      VARCHAR(20) NOT NULL DEFAULT '#409EFF' COMMENT '标签色值',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_user_name` (`user_id`, `name`),
  CONSTRAINT `fk_tag_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户笔记标签表';

CREATE TABLE `note_tag` (
  `note_id` BIGINT NOT NULL COMMENT '笔记ID',
  `tag_id`  BIGINT NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`note_id`, `tag_id`),
  KEY `idx_note_tag_tag_id` (`tag_id`),
  CONSTRAINT `fk_note_tag_note` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_note_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='笔记-标签关联表（多对多）';

-- P2 后期可选扩展表：提前预留结构，不阻塞 P0/P1 开发
CREATE TABLE `note_history` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '历史快照ID',
  `note_id`     BIGINT       NOT NULL                  COMMENT '关联笔记ID',
  `user_id`     BIGINT       NOT NULL                  COMMENT '所属用户ID',
  `version_no`  INT          NOT NULL                  COMMENT '历史版本序号',
  `title`       VARCHAR(200) NOT NULL                  COMMENT '快照标题',
  `content`     MEDIUMTEXT   NOT NULL                  COMMENT '快照Markdown正文',
  `change_note` VARCHAR(200) DEFAULT NULL              COMMENT '变更备注',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_note_history_note_ver` (`note_id`, `version_no`),
  KEY `idx_note_history_user` (`user_id`, `created_at`),
  CONSTRAINT `fk_note_history_note` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='笔记历史版本快照表（P2可选）';

-- ============================================================================
-- 3. 日程管理模块 (M3)
-- ============================================================================

DROP TABLE IF EXISTS `schedule`;

CREATE TABLE `schedule` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '日程ID',
  `user_id`      BIGINT        NOT NULL                  COMMENT '所属用户ID',
  `note_id`      BIGINT        DEFAULT NULL              COMMENT '双向关联的笔记ID（可为空）',
  `title`        VARCHAR(160)  NOT NULL                  COMMENT '日程标题',
  `description`  VARCHAR(1000) DEFAULT NULL              COMMENT '日程详情说明',
  `start_time`   DATETIME      NOT NULL                  COMMENT '开始时间',
  `end_time`     DATETIME      DEFAULT NULL              COMMENT '结束时间',
  `remind_time`  DATETIME      DEFAULT NULL              COMMENT '提醒触发时间',
  `reminded`     TINYINT       NOT NULL DEFAULT 0        COMMENT '是否已发送提醒：0=未发，1=已发',
  `priority`     TINYINT       NOT NULL DEFAULT 2        COMMENT '四象限优先级：1=重要且紧急，2=重要不紧急，3=紧急不重要，4=不重要不紧急',
  `status`       TINYINT       NOT NULL DEFAULT 0        COMMENT '状态：0=待办，1=已完成，2=已取消',
  `source_type`  VARCHAR(20)   NOT NULL DEFAULT 'MANUAL' COMMENT '创建来源：MANUAL / EBBINGHAUS / AI',
  `completed_at` DATETIME      DEFAULT NULL              COMMENT '完成时间',
  `deleted`      TINYINT       NOT NULL DEFAULT 0        COMMENT '逻辑删除：0=未删，1=已删',
  `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_schedule_user_time` (`user_id`, `deleted`, `start_time`),
  KEY `idx_schedule_user_quadrant` (`user_id`, `deleted`, `status`, `priority`),
  KEY `idx_schedule_remind_scan` (`deleted`, `status`, `reminded`, `remind_time`),
  KEY `idx_schedule_note` (`user_id`, `note_id`),
  CONSTRAINT `fk_schedule_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_schedule_note` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习日程与复习计划表';

-- ============================================================================
-- 4. 番茄钟专注模块 (M5)
-- ============================================================================

DROP TABLE IF EXISTS `focus_record`;

CREATE TABLE `focus_record` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '专注记录ID',
  `user_id`         BIGINT       NOT NULL                  COMMENT '所属用户ID',
  `note_id`         BIGINT       DEFAULT NULL              COMMENT '关联笔记ID（可选）',
  `schedule_id`     BIGINT       DEFAULT NULL              COMMENT '关联日程ID（可选）',
  `planned_minutes` INT          NOT NULL DEFAULT 25       COMMENT '计划专注分钟数',
  `actual_minutes`  INT          NOT NULL DEFAULT 0        COMMENT '实际专注分钟数',
  `status`          VARCHAR(20)  NOT NULL DEFAULT 'COMPLETED' COMMENT '专注状态：COMPLETED / ABORTED',
  `started_at`      DATETIME     NOT NULL                  COMMENT '开始时间',
  `ended_at`        DATETIME     NOT NULL                  COMMENT '结束时间',
  `remark`          VARCHAR(255) DEFAULT NULL              COMMENT '专注小结备注',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录入库时间',
  PRIMARY KEY (`id`),
  KEY `idx_focus_user_started` (`user_id`, `started_at`),
  KEY `idx_focus_note` (`note_id`),
  KEY `idx_focus_schedule` (`schedule_id`),
  CONSTRAINT `fk_focus_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_focus_note` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_focus_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `schedule` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='番茄钟专注记录表';

-- ============================================================================
-- 5. AI 助手与系统审计模块 (M4 / M8)
-- ============================================================================

DROP TABLE IF EXISTS `ai_chat_log`;
DROP TABLE IF EXISTS `sys_audit_log`;

CREATE TABLE `ai_chat_log` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'AI交互日志ID',
  `user_id`           BIGINT       NOT NULL                  COMMENT '调用用户ID',
  `note_id`           BIGINT       DEFAULT NULL              COMMENT '关联笔记ID（可为空）',
  `feature_type`      VARCHAR(30)  NOT NULL                  COMMENT '功能类型：SUMMARY / TAG_SUGGEST / NOTE_QA / SCHEDULE_PARSE',
  `provider`          VARCHAR(30)  NOT NULL DEFAULT 'deepseek' COMMENT '模型提供方标识',
  `model_name`        VARCHAR(60)  NOT NULL                  COMMENT '模型名称',
  `prompt_text`       TEXT         NOT NULL                  COMMENT '用户输入/摘要指令',
  `response_text`     MEDIUMTEXT   DEFAULT NULL              COMMENT 'AI返回内容',
  `prompt_tokens`     INT          NOT NULL DEFAULT 0        COMMENT '输入Token估算',
  `completion_tokens` INT          NOT NULL DEFAULT 0        COMMENT '输出Token估算',
  `latency_ms`        INT          NOT NULL DEFAULT 0        COMMENT '请求耗时（毫秒）',
  `success`           TINYINT      NOT NULL DEFAULT 1        COMMENT '是否成功：1=成功，0=失败/降级',
  `error_message`     VARCHAR(500) DEFAULT NULL              COMMENT '失败原因',
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_log_user_time` (`user_id`, `created_at`),
  KEY `idx_ai_log_feature_time` (`feature_type`, `created_at`),
  CONSTRAINT `fk_ai_log_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ai_log_note` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI调用记录与用量统计表';

CREATE TABLE `sys_audit_log` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '审计日志ID',
  `user_id`      BIGINT       DEFAULT NULL              COMMENT '操作用户ID（未登录可空）',
  `username`     VARCHAR(50)  DEFAULT NULL              COMMENT '操作用户名快照',
  `module`       VARCHAR(40)  NOT NULL                  COMMENT '所属模块：AUTH / NOTE / SCHEDULE / FOCUS / AI / ADMIN',
  `action`       VARCHAR(60)  NOT NULL                  COMMENT '操作描述',
  `http_method`  VARCHAR(10)  NOT NULL                  COMMENT 'HTTP方法',
  `request_uri`  VARCHAR(255) NOT NULL                  COMMENT '请求URI',
  `client_ip`    VARCHAR(64)  DEFAULT NULL              COMMENT '客户端IP',
  `cost_ms`      INT          NOT NULL DEFAULT 0        COMMENT '接口耗时（毫秒）',
  `status`       TINYINT      NOT NULL DEFAULT 1        COMMENT '结果：1=成功，0=异常',
  `error_msg`    VARCHAR(500) DEFAULT NULL              COMMENT '异常信息摘要',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_audit_time` (`created_at`),
  KEY `idx_audit_user_time` (`user_id`, `created_at`),
  KEY `idx_audit_module` (`module`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Spring AOP系统审计日志表';

-- ============================================================================
-- 6. 初始化基础角色数据（默认管理员账号由后端启动初始化器安全生成BCrypt密码）
-- ============================================================================

INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `description`) VALUES
  (1, 'USER',  '普通学习者', '拥有个人笔记、日程、番茄钟、AI助手与看板权限'),
  (2, 'ADMIN', '系统管理员', '拥有用户管理、角色分配、AI限流监控与审计日志权限')
ON DUPLICATE KEY UPDATE `role_name` = VALUES(`role_name`);

SET FOREIGN_KEY_CHECKS = 1;

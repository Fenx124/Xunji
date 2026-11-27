-- 索引实验脚本（手动执行，不要放进 docker-entrypoint-initdb.d）
-- 作用：给 admin 插入约 10000 条标题以 LAB: 开头的笔记，并输出三组 EXPLAIN。
-- 清理：执行文末 DELETE。不会删除标题不是 LAB: 的笔记。

USE `smart_study`;

SET @lab_user_id := (SELECT `id` FROM `sys_user` WHERE `username` = 'admin' AND `deleted` = 0 LIMIT 1);

-- 若这里结果为 NULL，先启动一次后端让 AdminInitializer 创建 admin，再重跑本脚本。
SELECT @lab_user_id AS lab_user_id;

INSERT INTO `note` (`user_id`, `notebook_id`, `title`, `content`, `word_count`, `version`, `deleted`)
SELECT
  @lab_user_id,
  NULL,
  CONCAT('LAB:', LPAD(n.n, 5, '0'), ' 事务隔离与索引'),
  CONCAT('实验正文 ', n.n, '。InnoDB MVCC ReadView 间隙锁 全文检索实验数据。'),
  40,
  1,
  0
FROM (
  SELECT a.d + b.d * 10 + c.d * 100 + d.d * 1000 + e.d * 10000 AS n
  FROM
    (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a,
    (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b,
    (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c,
    (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d,
    (SELECT 0 d UNION ALL SELECT 1) e
) n
WHERE @lab_user_id IS NOT NULL
  AND n.n BETWEEN 1 AND 10000
  AND NOT EXISTS (
    SELECT 1 FROM `note` existing
    WHERE existing.user_id = @lab_user_id
      AND existing.title = CONCAT('LAB:', LPAD(n.n, 5, '0'), ' 事务隔离与索引')
  );

ANALYZE TABLE `note`;

-- 1) 用户笔记列表：期望走 idx_note_user_list 左前缀
EXPLAIN
SELECT `id`, `title`, `is_pinned`, `updated_at`
FROM `note`
WHERE `user_id` = @lab_user_id
  AND `deleted` = 0
ORDER BY `is_pinned` DESC, `updated_at` DESC
LIMIT 20;

-- 2) 两字及以上全文检索：期望 key = ft_note_title_content
EXPLAIN
SELECT `id`, `title`
FROM `note`
WHERE `user_id` = @lab_user_id
  AND `deleted` = 0
  AND MATCH(`title`, `content`) AGAINST ('事务' IN NATURAL LANGUAGE MODE)
LIMIT 20;

-- 3) 单字 LIKE：通常用不上全文索引，对应应用里的单字降级
EXPLAIN
SELECT `id`, `title`
FROM `note`
WHERE `user_id` = @lab_user_id
  AND `deleted` = 0
  AND (`title` LIKE '%事%' OR `content` LIKE '%事%')
LIMIT 20;

-- 实验结束后取消下面注释再执行，只删除本脚本插入的行：
-- DELETE FROM `note` WHERE `user_id` = @lab_user_id AND `title` LIKE 'LAB:%';

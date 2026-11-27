package com.study.platform.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要写入 sys_audit_log 的接口。
 * 切面只记录结果，审计失败不会影响原来的业务调用。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /** 模块编码：AUTH / NOTE / SCHEDULE / FOCUS / AI / ADMIN。 */
    String module();

    /** 操作描述，展示在管理后台审计列表。 */
    String action();
}

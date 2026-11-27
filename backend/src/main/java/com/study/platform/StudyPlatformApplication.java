package com.study.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 综合学习辅助平台启动类。
 * Mapper 按业务域放在各模块的 mapper 包下，由统一扫描注册。
 * 定时提醒默认开启；app.schedule.enabled=false 时不注册 ScheduleReminderJob，测试不会跑后台扫描。
 */
@SpringBootApplication
@MapperScan("com.study.platform.**.mapper")
@EnableScheduling
public class StudyPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyPlatformApplication.class, args);
    }
}

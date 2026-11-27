package com.study.platform.schedule.job;

import com.study.platform.auth.service.UserContact;
import com.study.platform.auth.service.UserContactQueryService;
import com.study.platform.schedule.entity.Schedule;
import com.study.platform.schedule.mapper.ScheduleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 扫描到期未提醒的待办。邮件关闭或没有收件人时只记一条站内日志，并仍然标记已提醒。
 * 单条失败被吃掉，避免一封邮件异常拖垮整个定时任务。
 */
@Component
@ConditionalOnProperty(prefix = "app.schedule", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ScheduleReminderJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduleReminderJob.class);
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ScheduleMapper scheduleMapper;
    private final UserContactQueryService userContactQueryService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean mailEnabled;
    private final String mailFrom;

    public ScheduleReminderJob(ScheduleMapper scheduleMapper,
                               UserContactQueryService userContactQueryService,
                               ObjectProvider<JavaMailSender> mailSenderProvider,
                               @Value("${app.mail.enabled:false}") boolean mailEnabled,
                               @Value("${spring.mail.username:}") String mailFrom) {
        this.scheduleMapper = scheduleMapper;
        this.userContactQueryService = userContactQueryService;
        this.mailSenderProvider = mailSenderProvider;
        this.mailEnabled = mailEnabled;
        this.mailFrom = mailFrom == null ? "" : mailFrom.trim();
    }

    @Scheduled(fixedDelayString = "${app.schedule.remind-interval-ms:60000}")
    public void remindDueSchedules() {
        List<Schedule> due;
        try {
            due = scheduleMapper.selectDueReminders();
        } catch (Exception ex) {
            log.error("扫描待提醒日程失败", ex);
            return;
        }
        if (due == null || due.isEmpty()) {
            return;
        }
        Map<Long, UserContact> contacts = loadContacts(due);
        for (Schedule schedule : due) {
            try {
                remindOne(schedule, contacts.get(schedule.getUserId()));
            } catch (Exception ex) {
                log.error("日程提醒失败，本轮跳过 scheduleId={}", schedule.getId(), ex);
            }
        }
    }

    private Map<Long, UserContact> loadContacts(List<Schedule> due) {
        Set<Long> userIds = new HashSet<>();
        for (Schedule schedule : due) {
            if (schedule.getUserId() != null) {
                userIds.add(schedule.getUserId());
            }
        }
        Map<Long, UserContact> contacts = new HashMap<>();
        for (UserContact contact : userContactQueryService.findByIds(userIds)) {
            if (contact.id() != null) {
                contacts.put(contact.id(), contact);
            }
        }
        return contacts;
    }

    private void remindOne(Schedule schedule, UserContact contact) {
        String email = contact == null ? null : contact.email();
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailEnabled && mailSender != null && !mailFrom.isBlank() && email != null && !email.isBlank()) {
            mailSender.send(message(schedule, contact, email));
            log.info("已发送日程提醒邮件 scheduleId={} to={}", schedule.getId(), email);
        } else {
            log.info("日程提醒（站内/模拟，未发邮件）scheduleId={} userId={} title={} remindTime={} mailEnabled={} emailPresent={}",
                    schedule.getId(),
                    schedule.getUserId(),
                    schedule.getTitle(),
                    schedule.getRemindTime(),
                    mailEnabled,
                    email != null && !email.isBlank());
        }
        scheduleMapper.markReminded(schedule.getId());
    }

    private SimpleMailMessage message(Schedule schedule, UserContact contact, String email) {
        String nickname = contact.nickname() == null || contact.nickname().isBlank() ? "同学" : contact.nickname().trim();
        String when = schedule.getStartTime() == null ? "未设置" : schedule.getStartTime().format(CLOCK);
        String description = schedule.getDescription() == null || schedule.getDescription().isBlank()
                ? "无"
                : schedule.getDescription().trim();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("学习日程提醒：" + schedule.getTitle());
        message.setText(nickname + "，你好：\n\n"
                + "你有一条学习日程到了提醒时间。\n"
                + "标题：" + schedule.getTitle() + "\n"
                + "开始时间：" + when + "\n"
                + "说明：" + description + "\n\n"
                + "—— 综合学习辅助平台");
        return message;
    }
}

package com.study.platform.schedule.mapper;

/**
 * 只取日程标题，给专注记录回填 scheduleTitle。
 */
public class ScheduleTitleRow {

    private Long id;
    private String title;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

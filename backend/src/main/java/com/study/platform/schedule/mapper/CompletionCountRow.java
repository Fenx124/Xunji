package com.study.platform.schedule.mapper;

/**
 * 本月完成率的分子和分母。counted 不含已取消。
 */
public class CompletionCountRow {

    private Long completed;
    private Long counted;

    public Long getCompleted() {
        return completed;
    }

    public void setCompleted(Long completed) {
        this.completed = completed;
    }

    public Long getCounted() {
        return counted;
    }

    public void setCounted(Long counted) {
        this.counted = counted;
    }
}

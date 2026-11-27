package com.study.platform.system.mapper;

public class AiUserUsageRow {

    private Long userId;
    private Long calls;
    private Long todayCalls;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCalls() {
        return calls;
    }

    public void setCalls(Long calls) {
        this.calls = calls;
    }

    public Long getTodayCalls() {
        return todayCalls;
    }

    public void setTodayCalls(Long todayCalls) {
        this.todayCalls = todayCalls;
    }
}

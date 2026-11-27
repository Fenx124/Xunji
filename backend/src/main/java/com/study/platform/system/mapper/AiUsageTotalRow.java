package com.study.platform.system.mapper;

/**
 * ai_chat_log 在统计窗口内的合计。没有记录时 SQL 仍返回一行 0。
 */
public class AiUsageTotalRow {

    private Long totalCalls;
    private Long successCalls;
    private Long totalLatencyMs;
    private Long totalTokens;

    public Long getTotalCalls() {
        return totalCalls;
    }

    public void setTotalCalls(Long totalCalls) {
        this.totalCalls = totalCalls;
    }

    public Long getSuccessCalls() {
        return successCalls;
    }

    public void setSuccessCalls(Long successCalls) {
        this.successCalls = successCalls;
    }

    public Long getTotalLatencyMs() {
        return totalLatencyMs;
    }

    public void setTotalLatencyMs(Long totalLatencyMs) {
        this.totalLatencyMs = totalLatencyMs;
    }

    public Long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Long totalTokens) {
        this.totalTokens = totalTokens;
    }
}

package com.study.platform.common;

/**
 * 按天聚合的计数或分钟数。bucket 是 yyyy-MM-dd。
 */
public class DayTotalRow {

    private String bucket;
    private Long total;

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}

package com.study.platform.system.mapper;

public class AiFeatureUsageRow {

    private String featureType;
    private Long calls;
    private Long avgLatencyMs;

    public String getFeatureType() {
        return featureType;
    }

    public void setFeatureType(String featureType) {
        this.featureType = featureType;
    }

    public Long getCalls() {
        return calls;
    }

    public void setCalls(Long calls) {
        this.calls = calls;
    }

    public Long getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public void setAvgLatencyMs(Long avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
    }
}

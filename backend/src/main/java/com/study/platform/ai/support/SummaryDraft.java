package com.study.platform.ai.support;

import java.util.List;

/**
 * 解析后的摘要。tags 已经收口到 3 到 5 个。
 */
public record SummaryDraft(
        String summary,
        List<String> tags
) {
}

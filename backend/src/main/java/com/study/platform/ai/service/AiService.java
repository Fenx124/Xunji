package com.study.platform.ai.service;

import com.study.platform.ai.vo.AiChatLogVO;
import com.study.platform.ai.vo.AiParsedScheduleVO;
import com.study.platform.ai.vo.AiSummarizeVO;
import com.study.platform.common.PageResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiService {

    AiSummarizeVO summarize(Long noteId);

    /**
     * 调用前必须已经完成登录态、笔记归属和限流检查。
     * 返回的 SseEmitter 在独立线程里推送增量。
     */
    SseEmitter streamQa(Long noteId, String question);

    AiParsedScheduleVO parseSchedule(String text);

    PageResult<AiChatLogVO> logs(Long noteId, String featureType, long pageNum, long pageSize);
}

package com.study.platform.note.service;

/**
 * 给 AI 模块用的笔记读写门面。AI 服务依赖这个接口，不注入 NoteMapper。
 */
public interface NoteAiAccessService {

    /**
     * 笔记必须属于该用户且不在回收站。不存在返回 40401，越权返回 40301。
     */
    NoteAiSnapshot requireOwned(Long userId, Long noteId);

    /**
     * 回写 note.ai_summary，并在事务提交后删除 note:detail:{noteId}。
     */
    void saveSummary(Long userId, Long noteId, String summary);
}

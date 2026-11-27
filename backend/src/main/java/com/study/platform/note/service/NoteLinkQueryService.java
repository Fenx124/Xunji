package com.study.platform.note.service;

import java.util.Collection;
import java.util.Map;

/**
 * 给日程模块用的只读笔记查询。
 * 日程服务依赖这个接口，而不是 NoteMapper，避免两个业务模块互相注入 Mapper。
 */
public interface NoteLinkQueryService {

    /**
     * 笔记必须属于该用户且不在回收站。不存在返回 40401，越权返回 40301。
     */
    String requireActiveTitle(Long userId, Long noteId);

    /**
     * 批量取标题。回收站里的笔记仍返回标题，永久删除后的笔记不会出现。
     */
    Map<Long, String> findTitles(Long userId, Collection<Long> noteIds);
}

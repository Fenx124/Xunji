package com.study.platform.note.service;

import com.study.platform.common.PageResult;
import com.study.platform.note.dto.NoteCreateRequest;
import com.study.platform.note.dto.NoteUpdateRequest;
import com.study.platform.note.vo.NoteDetailVO;
import com.study.platform.note.vo.NoteFlagVO;
import com.study.platform.note.vo.NoteHistoryDetailVO;
import com.study.platform.note.vo.NoteHistoryItemVO;
import com.study.platform.note.vo.NoteListItemVO;

import java.util.List;

public interface NoteService {

    PageResult<NoteListItemVO> page(Long notebookId, Long tagId, String keyword, Boolean isFavorite,
                                    long pageNum, long pageSize);

    NoteDetailVO detail(Long id);

    NoteDetailVO create(NoteCreateRequest request);

    NoteDetailVO update(Long id, NoteUpdateRequest request);

    List<NoteHistoryItemVO> listHistory(Long noteId);

    NoteHistoryDetailVO historyDetail(Long noteId, Long historyId);

    NoteDetailVO restoreHistory(Long noteId, Long historyId);

    NoteFlagVO pin(Long id, Boolean isPinned);

    NoteFlagVO favorite(Long id, Boolean isFavorite);

    void moveToTrash(Long id);

    PageResult<NoteListItemVO> trash(long pageNum, long pageSize);

    NoteDetailVO restore(Long id);

    void deletePermanent(Long id);
}

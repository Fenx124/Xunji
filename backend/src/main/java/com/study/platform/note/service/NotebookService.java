package com.study.platform.note.service;

import com.study.platform.note.dto.NotebookSaveRequest;
import com.study.platform.note.vo.NotebookVO;

import java.util.List;

public interface NotebookService {

    List<NotebookVO> list();

    NotebookVO create(NotebookSaveRequest request);

    NotebookVO update(Long id, NotebookSaveRequest request);

    void delete(Long id);
}

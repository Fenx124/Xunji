package com.study.platform.note.service;

import com.study.platform.note.dto.TagSaveRequest;
import com.study.platform.note.vo.TagVO;

import java.util.List;

public interface TagService {

    List<TagVO> list();

    TagVO create(TagSaveRequest request);

    TagVO update(Long id, TagSaveRequest request);

    void delete(Long id);
}

package com.study.platform.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.platform.note.entity.Notebook;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotebookMapper extends BaseMapper<Notebook> {
}

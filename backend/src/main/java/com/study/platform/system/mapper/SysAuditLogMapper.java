package com.study.platform.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.platform.system.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {
}

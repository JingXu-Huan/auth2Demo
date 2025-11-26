package com.example.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.org.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志Mapper
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
    // 使用BaseMapper提供的基本CRUD功能
    // 复杂查询可根据需要添加
}

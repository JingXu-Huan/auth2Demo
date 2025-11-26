package com.example.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.file.entity.FileOperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileOperationLogMapper extends BaseMapper<FileOperationLog> {
}

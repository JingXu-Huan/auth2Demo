package com.example.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.file.entity.FileUploadSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件上传会话 Mapper
 */
@Mapper
public interface FileUploadSessionMapper extends BaseMapper<FileUploadSession> {
}

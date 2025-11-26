package com.example.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.file.entity.UserFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户文件 Mapper
 */
@Mapper
public interface UserFileMapper extends BaseMapper<UserFile> {
}

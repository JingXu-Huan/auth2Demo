package com.example.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.collab.entity.Document;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档 Mapper
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}

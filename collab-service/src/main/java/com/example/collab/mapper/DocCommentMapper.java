package com.example.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.collab.entity.DocComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocCommentMapper extends BaseMapper<DocComment> {
    
    @Select("SELECT * FROM doc_comments WHERE doc_id = #{docId} AND deleted_at IS NULL ORDER BY created_at")
    List<DocComment> selectByDocId(@Param("docId") Long docId);
    
    @Select("SELECT * FROM doc_comments WHERE parent_id = #{parentId} AND deleted_at IS NULL ORDER BY created_at")
    List<DocComment> selectReplies(@Param("parentId") Long parentId);
}

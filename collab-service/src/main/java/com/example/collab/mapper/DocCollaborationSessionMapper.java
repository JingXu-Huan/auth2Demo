package com.example.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.collab.entity.DocCollaborationSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文档协同会话 Mapper
 */
@Mapper
public interface DocCollaborationSessionMapper extends BaseMapper<DocCollaborationSession> {

    /**
     * 获取文档的活跃协同用户
     */
    @Select("SELECT * FROM doc_collaboration_sessions WHERE doc_id = #{docId} AND is_active = true")
    List<DocCollaborationSession> findActiveSessionsByDocId(@Param("docId") Long docId);
}

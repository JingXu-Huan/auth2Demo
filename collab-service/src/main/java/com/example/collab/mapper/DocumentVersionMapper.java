package com.example.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.collab.entity.DocumentVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocumentVersionMapper extends BaseMapper<DocumentVersion> {
    
    @Select("SELECT * FROM document_versions WHERE doc_id = #{docId} ORDER BY version_number DESC")
    List<DocumentVersion> selectByDocId(@Param("docId") Long docId);
    
    @Select("SELECT * FROM document_versions WHERE doc_id = #{docId} ORDER BY version_number DESC LIMIT 1")
    DocumentVersion selectLatestByDocId(@Param("docId") Long docId);
    
    @Select("SELECT MAX(version_number) FROM document_versions WHERE doc_id = #{docId}")
    Long selectMaxVersionNumber(@Param("docId") Long docId);
}

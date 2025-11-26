package com.example.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.file.entity.FileShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FileShareMapper extends BaseMapper<FileShare> {
    
    @Select("SELECT * FROM file_shares WHERE file_id = #{fileId}")
    List<FileShare> selectByFileId(@Param("fileId") Long fileId);
    
    @Select("SELECT * FROM file_shares WHERE shared_by = #{userId} ORDER BY created_at DESC")
    List<FileShare> selectBySharedBy(@Param("userId") Long userId);
    
    @Update("UPDATE file_shares SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);
    
    @Update("UPDATE file_shares SET download_count = download_count + 1 WHERE id = #{id}")
    int incrementDownloadCount(@Param("id") Long id);
}

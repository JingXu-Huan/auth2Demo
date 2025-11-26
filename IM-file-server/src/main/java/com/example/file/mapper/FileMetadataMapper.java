package com.example.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.file.entity.FileMetadata;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 文件元数据 Mapper
 */
@Mapper
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {

    /**
     * 增加引用计数
     */
    @Update("UPDATE file_metadata SET ref_count = ref_count + 1 WHERE file_hash = #{fileHash}")
    int incrementRefCount(@Param("fileHash") String fileHash);

    /**
     * 减少引用计数
     */
    @Update("UPDATE file_metadata SET ref_count = ref_count - 1 WHERE file_hash = #{fileHash}")
    int decrementRefCount(@Param("fileHash") String fileHash);
}

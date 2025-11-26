package com.example.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.file.entity.FileTranscodeTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FileTranscodeTaskMapper extends BaseMapper<FileTranscodeTask> {
    
    @Select("SELECT * FROM file_transcode_tasks WHERE status = 0 ORDER BY created_at ASC LIMIT #{limit}")
    List<FileTranscodeTask> selectPendingTasks(@Param("limit") int limit);
    
    @Select("SELECT * FROM file_transcode_tasks WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<FileTranscodeTask> selectByUserId(@Param("userId") Long userId);
}

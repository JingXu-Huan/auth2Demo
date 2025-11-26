package com.example.async.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.async.entity.ScheduledTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务 Mapper
 */
@Mapper
public interface ScheduledTaskMapper extends BaseMapper<ScheduledTask> {

    /**
     * 查询需要执行的任务
     */
    @Select("SELECT * FROM scheduled_tasks WHERE enabled = true " +
            "AND (next_run_at IS NULL OR next_run_at <= #{now}) " +
            "ORDER BY next_run_at LIMIT #{limit}")
    List<ScheduledTask> findDueTasks(@Param("now") LocalDateTime now, @Param("limit") int limit);
}

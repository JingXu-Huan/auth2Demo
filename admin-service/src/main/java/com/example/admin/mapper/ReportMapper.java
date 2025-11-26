package com.example.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.admin.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 举报Mapper
 */
@Mapper
public interface ReportMapper extends BaseMapper<Report> {
    
    @Select("SELECT * FROM reports WHERE status = #{status} ORDER BY created_at DESC LIMIT #{limit}")
    List<Report> findByStatus(@Param("status") int status, @Param("limit") int limit);
    
    @Select("SELECT COUNT(*) FROM reports WHERE status = 0")
    int countPending();
}

package com.example.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.collab.entity.DocActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocActivityMapper extends BaseMapper<DocActivity> {
    
    @Select("SELECT * FROM doc_activities WHERE doc_id = #{docId} ORDER BY created_at DESC LIMIT #{limit}")
    List<DocActivity> selectRecentByDocId(@Param("docId") Long docId, @Param("limit") int limit);
    
    @Select("SELECT * FROM doc_activities WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<DocActivity> selectRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);
}

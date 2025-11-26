package com.example.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.collab.entity.DocTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DocTemplateMapper extends BaseMapper<DocTemplate> {
    
    @Select("SELECT * FROM doc_templates WHERE is_public = TRUE ORDER BY use_count DESC")
    List<DocTemplate> selectPublicTemplates();
    
    @Select("SELECT * FROM doc_templates WHERE category = #{category} AND is_public = TRUE")
    List<DocTemplate> selectByCategory(@Param("category") String category);
    
    @Update("UPDATE doc_templates SET use_count = use_count + 1 WHERE id = #{id}")
    int incrementUseCount(@Param("id") Long id);
}

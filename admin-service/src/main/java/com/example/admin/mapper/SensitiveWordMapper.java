package com.example.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.admin.entity.SensitiveWord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 敏感词Mapper
 */
@Mapper
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {
    
    @Select("SELECT word FROM sensitive_words")
    List<String> findAllWords();
    
    @Select("SELECT * FROM sensitive_words WHERE category = #{category}")
    List<SensitiveWord> findByCategory(String category);
}

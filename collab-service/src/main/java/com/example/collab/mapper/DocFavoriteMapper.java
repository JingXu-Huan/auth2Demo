package com.example.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.collab.entity.DocFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocFavoriteMapper extends BaseMapper<DocFavorite> {
    
    @Select("SELECT * FROM doc_favorites WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<DocFavorite> selectByUserId(@Param("userId") Long userId);
    
    @Select("SELECT * FROM doc_favorites WHERE user_id = #{userId} AND doc_id = #{docId}")
    DocFavorite selectByUserAndDoc(@Param("userId") Long userId, @Param("docId") Long docId);
}

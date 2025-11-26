package com.example.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.file.entity.Folder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FolderMapper extends BaseMapper<Folder> {
    
    @Select("SELECT * FROM folders WHERE user_id = #{userId} AND parent_id IS NULL AND deleted_at IS NULL")
    List<Folder> selectRootFolders(@Param("userId") Long userId);
    
    @Select("SELECT * FROM folders WHERE parent_id = #{parentId} AND deleted_at IS NULL ORDER BY name")
    List<Folder> selectByParentId(@Param("parentId") Long parentId);
    
    @Select("SELECT * FROM folders WHERE user_id = #{userId} AND path LIKE #{pathPrefix} || '%' AND deleted_at IS NULL")
    List<Folder> selectByPathPrefix(@Param("userId") Long userId, @Param("pathPrefix") String pathPrefix);
}

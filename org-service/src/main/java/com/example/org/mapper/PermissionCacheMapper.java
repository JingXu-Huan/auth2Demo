package com.example.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.org.entity.PermissionCache;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 权限缓存Mapper
 */
@Mapper
public interface PermissionCacheMapper extends BaseMapper<PermissionCache> {

    @Select("SELECT * FROM permission_cache WHERE user_id = #{userId} AND expires_at > NOW()")
    PermissionCache findValidByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM permission_cache WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM permission_cache WHERE expires_at < NOW()")
    int deleteExpired();
}

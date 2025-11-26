package com.example.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.org.entity.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 数据权限Mapper
 */
@Mapper
public interface DataPermissionMapper extends BaseMapper<DataPermission> {
    
    /**
     * 查询用户的数据权限
     */
    @Select("SELECT * FROM data_permissions WHERE principal_type = 'user' AND principal_id = #{userId}")
    List<DataPermission> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 查询角色的数据权限
     */
    @Select("SELECT * FROM data_permissions WHERE principal_type = 'role' AND principal_id = #{roleId}")
    List<DataPermission> selectByRoleId(@Param("roleId") Long roleId);
    
    /**
     * 查询用户对特定资源类型的数据权限
     */
    @Select("SELECT * FROM data_permissions " +
            "WHERE principal_type = 'user' AND principal_id = #{userId} AND resource_type = #{resourceType}")
    DataPermission selectByUserAndResource(@Param("userId") Long userId, @Param("resourceType") String resourceType);
}

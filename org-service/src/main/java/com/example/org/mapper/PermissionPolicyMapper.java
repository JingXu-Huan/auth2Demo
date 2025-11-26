package com.example.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.org.entity.PermissionPolicy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限策略Mapper
 */
@Mapper
public interface PermissionPolicyMapper extends BaseMapper<PermissionPolicy> {

    @Select("SELECT * FROM permission_policies WHERE enabled = true ORDER BY priority ASC")
    List<PermissionPolicy> findAllEnabled();

    @Select("SELECT * FROM permission_policies WHERE effect = #{effect} AND enabled = true ORDER BY priority ASC")
    List<PermissionPolicy> findByEffect(@Param("effect") String effect);
}

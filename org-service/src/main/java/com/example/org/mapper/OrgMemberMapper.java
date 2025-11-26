package com.example.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.org.entity.OrgMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 组织成员Mapper
 */
@Mapper
public interface OrgMemberMapper extends BaseMapper<OrgMember> {
    
    /**
     * 根据用户ID查询所有组织成员关系
     */
    @Select("SELECT * FROM org_members WHERE user_id = #{userId} AND status = 1")
    List<OrgMember> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 根据部门ID查询成员
     */
    @Select("SELECT * FROM org_members WHERE dept_id = #{deptId} AND status = 1")
    List<OrgMember> selectByDeptId(@Param("deptId") Long deptId);
    
    /**
     * 根据组织ID和用户ID查询成员
     */
    @Select("SELECT * FROM org_members WHERE org_id = #{orgId} AND user_id = #{userId} AND status = 1")
    OrgMember selectByOrgAndUser(@Param("orgId") Long orgId, @Param("userId") Long userId);
    
    /**
     * 查询直属下级
     */
    @Select("SELECT * FROM org_members WHERE direct_manager_id = #{managerId} AND status = 1")
    List<OrgMember> selectDirectReports(@Param("managerId") Long managerId);
}

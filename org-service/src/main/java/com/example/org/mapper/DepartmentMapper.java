package com.example.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.org.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 部门Mapper
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
    
    /**
     * 查询组织下的子部门
     */
    @Select("SELECT * FROM departments WHERE org_id = #{orgId} AND parent_id = #{parentId} AND status = 1 ORDER BY sort_order")
    List<Department> findByOrgAndParent(@Param("orgId") Long orgId, @Param("parentId") Long parentId);
    
    /**
     * 查询子部门
     */
    @Select("SELECT * FROM departments WHERE parent_id = #{parentId} AND status = 1 ORDER BY sort_order")
    List<Department> findByParentId(@Param("parentId") Long parentId);
    
    /**
     * 查询所有子孙部门
     */
    @Select("SELECT * FROM departments WHERE path LIKE #{pathPrefix} || '%' AND status = 1")
    List<Department> findDescendants(@Param("pathPrefix") String pathPrefix);
    
    /**
     * 批量更新子孙路径
     */
    @Update("UPDATE departments SET path = overlay(path placing #{newPrefix} from 1 for length(#{oldPrefix})) WHERE path LIKE #{oldPrefix} || '%'")
    int updateChildPaths(@Param("oldPrefix") String oldPrefix, @Param("newPrefix") String newPrefix);
    
    /**
     * 检查部门下是否有成员
     */
    @Select("SELECT COUNT(*) FROM dept_user_relation WHERE dept_id = #{deptId}")
    int countMembers(@Param("deptId") Long deptId);
    
    /**
     * 检查部门下是否有子部门
     */
    @Select("SELECT COUNT(*) FROM departments WHERE parent_id = #{deptId} AND status = 1")
    int countChildren(@Param("deptId") Long deptId);
    
    /**
     * 增加成员数
     */
    @Update("UPDATE departments SET member_count = member_count + 1 WHERE id = #{deptId}")
    void incrementMemberCount(@Param("deptId") Long deptId);
    
    /**
     * 减少成员数
     */
    @Update("UPDATE departments SET member_count = member_count - 1 WHERE id = #{deptId} AND member_count > 0")
    void decrementMemberCount(@Param("deptId") Long deptId);
}

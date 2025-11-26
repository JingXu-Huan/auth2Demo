package com.example.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.org.dto.MemberVO;
import com.example.org.entity.DeptUserRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 部门-用户关联Mapper
 */
@Mapper
public interface DeptUserRelationMapper extends BaseMapper<DeptUserRelation> {
    
    /**
     * 查询用户所属部门
     */
    @Select("SELECT * FROM dept_user_relation WHERE user_id = #{userId}")
    List<DeptUserRelation> findByUserId(@Param("userId") Long userId);
    
    /**
     * 查询用户主属部门
     */
    @Select("SELECT * FROM dept_user_relation WHERE user_id = #{userId} AND is_primary = true LIMIT 1")
    DeptUserRelation findPrimaryByUserId(@Param("userId") Long userId);
    
    /**
     * 查询部门成员
     */
    @Select("SELECT * FROM dept_user_relation WHERE dept_id = #{deptId}")
    List<DeptUserRelation> findByDeptId(@Param("deptId") Long deptId);
    
    /**
     * 清除用户主属标记
     */
    @Update("UPDATE dept_user_relation SET is_primary = false WHERE user_id = #{userId}")
    void clearPrimary(@Param("userId") Long userId);
    
    /**
     * 设置主属部门
     */
    @Update("UPDATE dept_user_relation SET is_primary = true WHERE user_id = #{userId} AND dept_id = #{deptId}")
    void setPrimary(@Param("userId") Long userId, @Param("deptId") Long deptId);
    
    /**
     * 查询关联
     */
    @Select("SELECT * FROM dept_user_relation WHERE user_id = #{userId} AND dept_id = #{deptId}")
    DeptUserRelation findByUserAndDept(@Param("userId") Long userId, @Param("deptId") Long deptId);
}

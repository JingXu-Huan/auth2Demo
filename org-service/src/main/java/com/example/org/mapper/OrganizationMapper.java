package com.example.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.org.entity.Organization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 组织Mapper
 */
@Mapper
public interface OrganizationMapper extends BaseMapper<Organization> {
    
    /**
     * 更新员工数量
     */
    @Update("UPDATE organizations SET employee_count = employee_count + #{delta} WHERE id = #{orgId}")
    int updateEmployeeCount(@Param("orgId") Long orgId, @Param("delta") int delta);
}

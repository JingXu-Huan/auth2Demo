package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.domain.model.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 * 基于新数据库设计的用户数据访问层
 * 
 * @author System
 * @since 2024-11-25
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 使用MyBatis Plus提供的基础CRUD方法
    // 不再需要自定义SQL，因为新设计中密码直接存储在users表中
}

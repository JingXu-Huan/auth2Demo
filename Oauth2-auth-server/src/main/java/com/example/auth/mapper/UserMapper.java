package com.example.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.auth.entity.OAuthBinding;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<OAuthBinding.User> {
    // MyBatis Plus提供了基础的CRUD操作，无需额外定义
}

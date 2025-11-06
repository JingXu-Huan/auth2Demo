package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.model.UserCredential;
import org.apache.ibatis.annotations.*;

/**
 * 用户凭证 Mapper
 * 继承 MyBatis-Plus 的 BaseMapper，自动拥有 CRUD 方法
 */
@Mapper
public interface UserCredentialMapper extends BaseMapper<UserCredential> {
    
    /**
     * 根据用户名和提供方查询凭证
     */
    @Select("SELECT uc.* FROM user_credentials uc " +
            "INNER JOIN users u ON uc.user_id = u.id " +
            "WHERE u.username = #{username} AND uc.provider = #{provider}")
    UserCredential selectByUsernameAndProvider(@Param("username") String username, 
                                                @Param("provider") String provider);
}

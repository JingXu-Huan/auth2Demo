package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.domain.model.UserCredential;
import org.apache.ibatis.annotations.*;

/**
 * UserCredential Mapper 接口
 * 用户凭证数据访问层
 */
@Mapper
public interface UserCredentialMapper extends BaseMapper<UserCredential> {
    
    /**
     * 根据用户ID和登录方式查询凭证
     */
    @Select("SELECT * FROM user_credentials WHERE user_id = #{userId} AND provider = #{provider}")
    UserCredential findByUserIdAndProvider(@Param("userId") Long userId, 
                                          @Param("provider") String provider);
    
    /**
     * 根据第三方平台ID查询凭证
     */
    @Select("SELECT * FROM user_credentials WHERE provider = #{provider} AND provider_user_id = #{providerUserId}")
    UserCredential findByProviderAndProviderUserId(@Param("provider") String provider, 
                                                   @Param("providerUserId") String providerUserId);
    
    /**
     * 更新密码
     */
    @Update("UPDATE user_credentials SET password_hash = #{passwordHash} WHERE user_id = #{userId} AND provider = 'email'")
    int updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);
}

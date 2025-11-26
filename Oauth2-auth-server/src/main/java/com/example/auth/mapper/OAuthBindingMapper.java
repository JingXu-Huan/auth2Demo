package com.example.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.auth.entity.OAuthBinding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * OAuth绑定 Mapper
 */
@Mapper
public interface OAuthBindingMapper extends BaseMapper<OAuthBinding> {
    
    /**
     * 根据提供商和用户ID查询绑定信息
     */
    @Select("SELECT * FROM oauth_bindings WHERE provider = #{provider} AND provider_user_id = #{providerUserId} AND bind_status = 1")
    OAuthBinding findByProviderAndUserId(@Param("provider") String provider, @Param("providerUserId") String providerUserId);
    
    /**
     * 查询用户的所有OAuth绑定
     */
    @Select("SELECT * FROM oauth_bindings WHERE user_id = #{userId} AND bind_status = 1")
    List<OAuthBinding> findByUserId(@Param("userId") Long userId);
    
    /**
     * 检查用户是否已绑定该OAuth账号
     */
    @Select("SELECT COUNT(1) > 0 FROM oauth_bindings WHERE user_id = #{userId} AND provider = #{provider} AND bind_status = 1")
    boolean existsByUserIdAndProvider(@Param("userId") Long userId, @Param("provider") String provider);
}

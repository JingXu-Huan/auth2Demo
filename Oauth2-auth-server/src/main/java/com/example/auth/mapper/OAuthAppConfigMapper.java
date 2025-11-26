package com.example.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.auth.entity.OAuthAppConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * OAuth应用配置Mapper
 */
@Mapper
public interface OAuthAppConfigMapper extends BaseMapper<OAuthAppConfig> {

    @Select("SELECT * FROM oauth_app_configs WHERE provider = #{provider} AND enabled = true")
    OAuthAppConfig findByProvider(@Param("provider") String provider);

    @Select("SELECT * FROM oauth_app_configs WHERE enabled = true ORDER BY provider")
    List<OAuthAppConfig> findAllEnabled();
}

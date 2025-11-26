package com.example.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.auth.entity.OAuth2Client;
import org.apache.ibatis.annotations.Mapper;

/**
 * OAuth2客户端Mapper
 */
@Mapper
public interface OAuth2ClientMapper extends BaseMapper<OAuth2Client> {
}

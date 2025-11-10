package com.example.auth.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 授权服务器配置
 * 继承认证服务器的基类
 */
@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    //导入认证管理器,密码模式使用他验证账号和密码
    @Autowired
    private AuthenticationManager authenticationManager;

    //密码编码器.用于与数据库的密码配对,通常是加密过的
    @Autowired
    private PasswordEncoder passwordEncoder;

    //配置令牌的存储位置，已在 RedisTokenStoreConfig.java 中配置
    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private CustomTokenEnhancer tokenEnhancer;

       /**
     * 配置客户端详情
     * 定义哪些客户端可以访问授权服务器
     * 位于 AuthorizationServerConfig.java 的 configure 方法中
        使用 ClientDetailsServiceConfigurer 来设定可访问的客户端
        当前实现留空待填充示例/扩展（注释显示不同存储方案的 TokenStore 选项）
     */
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception
    {
        clients.inMemory()
                // 客户端ID（相当于客户端的用户名）
                .withClient("client")
                // 客户端密钥（相当于客户端的密码，必须加密）
                .secret(passwordEncoder.encode("secret"))
                // 授权类型：password=密码模式, refresh_token=刷新令牌
                .authorizedGrantTypes("password", "refresh_token")
                // 授权范围：客户端能访问的资源范围
                .scopes("read", "write")
                // 访问令牌有效期（秒）：2小时（安全改进：从12小时缩短）
                .accessTokenValiditySeconds(7200)
                // 刷新令牌有效期（秒）：7天
                .refreshTokenValiditySeconds(604800);
    }


    /**
     * 配置授权服务器端点
     * 设置令牌存储和认证管理器
     */
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoint)
    {
        endpoint.tokenStore(tokenStore)
                .tokenEnhancer(tokenEnhancer)
                .authenticationManager(authenticationManager);
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer security)
    {
        security// 允许客户端使用表单认证（而不是Basic Auth）
                .allowFormAuthenticationForClients()
                // 开放 /oauth/token_key 端点（如果使用JWT需要）
                .tokenKeyAccess("permitAll()")
                // 开放 /oauth/check_token 端点（用于验证令牌）
                .checkTokenAccess("isAuthenticated()");
    }
}

package com.example.auth.config;

import com.example.auth.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * Web安全配置
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
    
    /**
     * 配置用户认证信息
     * 从数据库读取用户信息（支持多种登录方式）
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder());
    }

    /*
     * 配置 HTTP 安全设置
     * 禁用 CORS，由网关统一处理
     * 启用 CSRF 保护，但对 OAuth2 端点和 API 端点禁用（因为使用 Token 认证）
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .cors().disable()  // CORS 由网关统一处理
                // 启用 CSRF 保护，但对 OAuth2 端点和 API 端点禁用（因为使用 Token 认证）
                .csrf()
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringAntMatchers(
                        "/oauth/**",           // OAuth2 端点
                        "/api/auth/**",        // API 认证端点
                        "/doc.html",           // Swagger 文档
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v2/api-docs"
                    )
                .and()
                .authorizeRequests()
                // 允许访问 Knife4j 文档
                .antMatchers("/doc.html", "/webjars/**", "/swagger-resources/**", "/v2/api-docs").permitAll()
                .antMatchers("/oauth/**", "/login.html", "/api/auth/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .formLogin().permitAll();
    }
}

package com.example.auth.config;

import com.example.auth.handler.LoginFailureHandler;
import com.example.auth.handler.LoginSuccessHandler;
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
    
    @Autowired
    private LoginSuccessHandler loginSuccessHandler;
    
    @Autowired
    private LoginFailureHandler loginFailureHandler;
    
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
                .cors()  // 启用 CORS（使用 CorsConfig 的配置）
                .and()
                // 启用 CSRF 保护，但对 OAuth2 端点和 API 端点禁用（因为使用 Token 认证）
                .csrf()
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringAntMatchers(
                        "/oauth/**",           // OAuth2 端点
                        "/api/**",             // 所有 API 端点
                        "/login",              // Spring Security 登录端点
                        "/doc.html",           // Swagger 文档
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v2/api-docs"
                    )
                .and()
                .authorizeRequests()
                // 允许访问 Knife4j 文档
                .antMatchers("/doc.html", "/webjars/**", "/swagger-resources/**", "/v2/api-docs").permitAll()
                .antMatchers("/oauth/**", "/login.html", "/api/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .formLogin()
                    .loginProcessingUrl("/login")         // 登录处理 URL
                    .usernameParameter("username")         // 用户名参数名（邮箱）
                    .passwordParameter("password")         // 密码参数名
                    .successHandler(loginSuccessHandler)  // 登录成功处理器
                    .failureHandler(loginFailureHandler)  // 登录失败处理器
                    .permitAll();
    }
}

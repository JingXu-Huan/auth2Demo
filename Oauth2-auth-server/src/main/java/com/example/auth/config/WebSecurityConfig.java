package com.example.auth.config;

import com.example.auth.handler.LoginFailureHandler;
import com.example.auth.handler.LoginSuccessHandler;
import com.example.auth.service.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * ====================================================================
 * Spring Security 安全配置类
 * ====================================================================
 * 
 * 【核心作用】
 * 配置Web应用的安全策略，包括：
 * - 身份认证（Authentication）：验证用户是谁
 * - 授权（Authorization）：验证用户能做什么
 * - 密码加密策略
 * - 登录/登出处理
 * 
 * 【Spring Security 6 新特性】
 * Spring Boot 3.x 使用 Spring Security 6，配置方式有重大变化：
 * - 废弃 WebSecurityConfigurerAdapter（旧版继承方式）
 * - 改用 SecurityFilterChain Bean（新版组件方式）
 * - 使用 Lambda DSL 配置更清晰
 * 
 * 【认证流程】
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. 用户提交 username + password                            │
 * │                    ↓                                        │
 * │  2. UsernamePasswordAuthenticationFilter 拦截请求           │
 * │                    ↓                                        │
 * │  3. AuthenticationManager 调用 UserDetailsService          │
 * │                    ↓                                        │
 * │  4. 加载用户信息，比对密码（BCrypt）                          │
 * │                    ↓                                        │
 * │  5. 成功 → LoginSuccessHandler（生成JWT）                    │
 * │     失败 → LoginFailureHandler（返回错误）                    │
 * └─────────────────────────────────────────────────────────────┘
 * 
 * @author 学习笔记
 * @see UserDetailsServiceImpl 用户信息加载服务
 * @see LoginSuccessHandler 登录成功处理器
 */
@Configuration           // 标记为配置类，Spring会扫描并注册其中的@Bean
@EnableWebSecurity       // 启用Spring Security的Web安全功能
@RequiredArgsConstructor // Lombok: 为final字段生成构造函数
public class WebSecurityConfig {
    
    /** 用户详情服务 - 用于从数据库加载用户信息 */
    private final UserDetailsServiceImpl userDetailsService;
    
    /** 登录成功处理器 - 生成JWT Token */
    private final LoginSuccessHandler loginSuccessHandler;
    
    /** 登录失败处理器 - 返回错误信息 */
    private final LoginFailureHandler loginFailureHandler;
    
    /**
     * 密码编码器 Bean
     * 
     * 【BCrypt算法特点】
     * - 自动加盐（salt），每次加密结果不同
     * - 可配置计算强度（默认10轮）
     * - 抗彩虹表攻击
     * 
     * 使用示例：
     * String encoded = passwordEncoder.encode("123456");
     * boolean match = passwordEncoder.matches("123456", encoded);
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 认证管理器 Bean
     * 
     * 【组件说明】
     * - DaoAuthenticationProvider：基于数据库的认证提供者
     * - UserDetailsService：加载用户信息的服务
     * - PasswordEncoder：密码比对器
     * 
     * 认证流程：
     * 1. 接收用户名密码
     * 2. 调用 userDetailsService.loadUserByUsername() 加载用户
     * 3. 使用 passwordEncoder 比对密码
     * 4. 返回认证结果
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);  // 设置用户加载服务
        provider.setPasswordEncoder(passwordEncoder());      // 设置密码编码器
        return new ProviderManager(provider);
    }
    
    /**
     * 安全过滤链配置
     * 
     * 【核心配置项】
     * 1. CORS - 跨域资源共享
     * 2. CSRF - 跨站请求伪造防护（REST API通常禁用）
     * 3. 授权规则 - 哪些URL需要认证
     * 4. 表单登录 - 登录页面和处理器
     * 5. 异常处理 - 未认证时的响应
     * 
     * 【请求处理流程】
     * HTTP请求 → CORS过滤器 → 认证过滤器 → 授权过滤器 → Controller
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ============ 1. CORS配置 ============
            // 跨域配置，允许前端(不同端口)访问后端API
            .cors(cors -> cors.configure(http))
            
            // ============ 2. CSRF配置 ============
            // 禁用CSRF防护（因为使用JWT无状态认证，不需要CSRF）
            // 注意：如果使用Session，应该开启CSRF
            .csrf(AbstractHttpConfigurer::disable)
            
            // ============ 3. 授权规则 ============
            .authorizeHttpRequests(auth -> auth
                // Swagger文档 - 允许匿名访问
                .requestMatchers("/doc.html", "/webjars/**", "/swagger-resources/**", "/v3/api-docs/**").permitAll()
                // OAuth和登录相关 - 允许匿名访问
                .requestMatchers("/oauth/**", "/login.html", "/api/**", "/login").permitAll()
                // 健康检查端点 - 允许匿名访问
                .requestMatchers("/actuator/**").permitAll()
                // 其他所有请求 - 需要认证
                .anyRequest().authenticated()
            )
            
            // ============ 4. 表单登录配置 ============
            .formLogin(form -> form
                // 登录处理URL（POST请求）
                .loginProcessingUrl("/login")
                // 用户名参数名
                .usernameParameter("username")
                // 密码参数名
                .passwordParameter("password")
                // 登录成功处理器
                .successHandler(loginSuccessHandler)
                // 登录失败处理器
                .failureHandler(loginFailureHandler)
                // 允许所有用户访问登录相关URL
                .permitAll()
            )
            
            // ============ 5. 异常处理 ============
            .exceptionHandling(ex -> ex
                // 未认证时的处理（返回401 JSON响应）
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"code\":401,\"message\":\"未认证，请先登录\"}");
                })
            );
        
        // 构建并返回过滤链
        return http.build();
    }
}

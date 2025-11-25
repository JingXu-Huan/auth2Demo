package com.example.auth.config;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * Feign Sentinel 配置 - 已禁用
 * 
 * 注意: 自定义 Feign.Builder 会与 Spring Cloud OpenFeign 的自动配置冲突，
 * 导致 factoryBeanObjectType 错误。如需 Sentinel 支持，请使用
 * spring-cloud-alibaba-sentinel 的自动配置。
 */
// @Configuration - 已禁用，避免与 OpenFeign 冲突
public class FeignSentinelConfig {
    // 移除自定义 Feign.Builder，使用默认配置
}

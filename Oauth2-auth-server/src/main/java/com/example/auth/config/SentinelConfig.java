package com.example.auth.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * Sentinel 配置类
 * 配置流控规则和降级规则
 */
@Configuration
@Slf4j
public class SentinelConfig {
    
    /**
     * 注册 Sentinel 切面，用于支持 @SentinelResource 注解
     */
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }
    
    /**
     * 初始化流控规则和降级规则
     */
    @PostConstruct
    public void initRules() {
        initFlowRules();
        initDegradeRules();
        log.info("Sentinel 规则初始化完成");
    }
    
    /**
     * 初始化流控规则
     */
    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();
        
        // User-server 服务调用流控规则
        FlowRule userServiceRule = new FlowRule();
        userServiceRule.setResource("GET:http://user-server/api/users/details/email/{email}");
        userServiceRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        userServiceRule.setCount(100);  // QPS 阈值 100
        rules.add(userServiceRule);
        
        // 认证接口流控规则
        FlowRule authRule = new FlowRule();
        authRule.setResource("/api/auth/check-email");
        authRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        authRule.setCount(50);  // QPS 阈值 50
        rules.add(authRule);
        
        FlowRuleManager.loadRules(rules);
        log.info("流控规则加载完成，共 {} 条", rules.size());
    }
    
    /**
     * 初始化降级规则
     */
    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();
        
        // User-server 服务调用降级规则 - 异常比例策略
        DegradeRule userServiceExceptionRule = new DegradeRule();
        userServiceExceptionRule.setResource("GET:http://user-server/api/users/details/email/{email}");
        userServiceExceptionRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        userServiceExceptionRule.setCount(0.5);  // 异常比例阈值 50%
        userServiceExceptionRule.setTimeWindow(15);  // 熔断时长 15 秒
        userServiceExceptionRule.setMinRequestAmount(5);  // 最小请求数 5
        userServiceExceptionRule.setStatIntervalMs(1000);  // 统计时长 1 秒
        rules.add(userServiceExceptionRule);
        
        // User-server 服务调用降级规则 - 慢调用比例策略
        DegradeRule userServiceRtRule = new DegradeRule();
        userServiceRtRule.setResource("GET:http://user-server/api/users/details/email/{email}");
        userServiceRtRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        userServiceRtRule.setCount(1000);  // 响应时间阈值 1000ms
        userServiceRtRule.setTimeWindow(10);  // 熔断时长 10 秒
        userServiceRtRule.setMinRequestAmount(5);  // 最小请求数 5
        userServiceRtRule.setStatIntervalMs(1000);  // 统计时长 1 秒
        rules.add(userServiceRtRule);
        
        DegradeRuleManager.loadRules(rules);
        log.info("降级规则加载完成，共 {} 条", rules.size());
    }
}

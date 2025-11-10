package com.example.auth.handler;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * Sentinel 异常处理器
 * 自定义被限流、降级、系统保护拦截时的响应
 */
@Component
@Slf4j
public class SentinelExceptionHandler implements BlockExceptionHandler {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, BlockException e) throws Exception {
        log.warn("Sentinel 拦截请求: URI={}, Exception={}", request.getRequestURI(), e.getClass().getSimpleName());
        
        Map<String, Object> result = new HashMap<>();
        
        // 根据不同的异常类型返回不同的响应
        if (e instanceof FlowException) {
            // 流控异常
            result.put("code", 429);
            result.put("message", "请求过于频繁，请稍后再试");
            result.put("type", "flow_control");
        } else if (e instanceof DegradeException) {
            // 降级异常
            result.put("code", 503);
            result.put("message", "服务暂时不可用，请稍后重试");
            result.put("type", "degrade");
        } else if (e instanceof ParamFlowException) {
            // 热点参数限流异常
            result.put("code", 429);
            result.put("message", "热点参数限流");
            result.put("type", "param_flow");
        } else if (e instanceof AuthorityException) {
            // 授权异常
            result.put("code", 403);
            result.put("message", "访问被拒绝");
            result.put("type", "authority");
        } else {
            // 系统保护异常
            result.put("code", 503);
            result.put("message", "系统保护，请稍后重试");
            result.put("type", "system");
        }
        
        result.put("timestamp", System.currentTimeMillis());
        result.put("path", request.getRequestURI());
        
        // 设置响应
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}

# 📝 登录日志服务使用指南

## 🎯 概述

`LoginLogService` 是一个异步的登录日志记录服务，用于记录所有登录相关的操作，包括成功、失败和锁定。

---

## ✅ 已完成的配置

### 1. 启用异步支持

已在 `UserServerApplication.java` 中添加 `@EnableAsync` 注解：

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync  // ✅ 已启用
@ComponentScan(basePackages = {"com.example.user", "com.example.common"})
public class UserServerApplication {
    // ...
}
```

### 2. 数据库表

`login_logs` 表已在 `schema_postgresql.sql` 中定义：

```sql
CREATE TABLE login_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    email VARCHAR(255),
    ip_address VARCHAR(50) NOT NULL,
    user_agent TEXT,
    login_status VARCHAR(20) NOT NULL,  -- SUCCESS, FAILED, BLOCKED
    failure_reason VARCHAR(255),
    location VARCHAR(255),
    device_type VARCHAR(50),            -- MOBILE, TABLET, DESKTOP
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🔧 使用方法

### 方式 1: 在 Controller 中使用

#### 示例：用户登录接口

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private LoginLogService loginLogService;
    
    @PostMapping("/login")
    public ResponseEntity<Result<TokenVO>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // 获取 IP 和 User-Agent
            String ipAddress = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            // 执行登录逻辑
            TokenVO token = authService.login(request.getEmail(), request.getPassword());
            
            // ✅ 记录登录成功
            loginLogService.logLoginSuccess(
                token.getUserId(), 
                request.getEmail(), 
                ipAddress, 
                userAgent
            );
            
            return ResponseEntity.ok(Result.success(token));
            
        } catch (BadCredentialsException e) {
            // ✅ 记录登录失败
            loginLogService.logLoginFailure(
                request.getEmail(), 
                getClientIp(httpRequest), 
                httpRequest.getHeader("User-Agent"),
                "密码错误"
            );
            
            return ResponseEntity.ok(Result.error(401, "用户名或密码错误"));
        }
    }
    
    // 获取客户端真实 IP
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
```

---

### 方式 2: 在 Spring Security Handler 中使用

#### 在 LoginSuccessHandler 中记录

由于 `LoginLogService` 在 User-server 中，而 `LoginSuccessHandler` 在 Oauth2-auth-server 中，我们需要通过 Feign 调用。

**步骤 1**: 在 User-server 创建 Controller 接口

```java
@RestController
@RequestMapping("/internal/login-logs")
public class LoginLogController {
    
    @Autowired
    private LoginLogService loginLogService;
    
    /**
     * 记录登录成功（内部接口）
     */
    @PostMapping("/success")
    public Result<Void> logSuccess(@RequestBody LoginLogRequest request) {
        loginLogService.logLoginSuccess(
            request.getUserId(),
            request.getEmail(),
            request.getIpAddress(),
            request.getUserAgent()
        );
        return Result.success(null);
    }
    
    /**
     * 记录登录失败（内部接口）
     */
    @PostMapping("/failure")
    public Result<Void> logFailure(@RequestBody LoginLogRequest request) {
        loginLogService.logLoginFailure(
            request.getEmail(),
            request.getIpAddress(),
            request.getUserAgent(),
            request.getFailureReason()
        );
        return Result.success(null);
    }
    
    /**
     * 记录账户锁定（内部接口）
     */
    @PostMapping("/blocked")
    public Result<Void> logBlocked(@RequestBody LoginLogRequest request) {
        loginLogService.logAccountBlocked(
            request.getEmail(),
            request.getIpAddress(),
            request.getUserAgent()
        );
        return Result.success(null);
    }
}
```

**步骤 2**: 创建请求 DTO

```java
@Data
public class LoginLogRequest {
    private Long userId;
    private String email;
    private String ipAddress;
    private String userAgent;
    private String failureReason;
}
```

**步骤 3**: 在 Oauth2-auth-server 创建 Feign 客户端

```java
@FeignClient(
    name = "user-server",
    fallback = LoginLogClientFallback.class
)
public interface LoginLogClient {
    
    @PostMapping("/internal/login-logs/success")
    Result<Void> logSuccess(@RequestBody LoginLogRequest request);
    
    @PostMapping("/internal/login-logs/failure")
    Result<Void> logFailure(@RequestBody LoginLogRequest request);
    
    @PostMapping("/internal/login-logs/blocked")
    Result<Void> logBlocked(@RequestBody LoginLogRequest request);
}
```

**步骤 4**: 在 LoginSuccessHandler 中调用

```java
@Component
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    
    @Autowired
    private LoginLogClient loginLogClient;
    
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, 
            HttpServletResponse response, 
            Authentication authentication) throws ServletException, IOException {
        
        try {
            String email = authentication.getName();
            
            // ✅ 记录登录成功
            LoginLogRequest logRequest = new LoginLogRequest();
            logRequest.setEmail(email);
            logRequest.setIpAddress(getClientIp(request));
            logRequest.setUserAgent(request.getHeader("User-Agent"));
            
            loginLogClient.logSuccess(logRequest);
            
            log.info("登录成功: email={}", email);
            
        } catch (Exception e) {
            log.error("记录登录日志失败", e);
        }
        
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
```

---

### 方式 3: 直接在 Service 中使用

```java
@Service
public class UserService {
    
    @Autowired
    private LoginLogService loginLogService;
    
    public void someMethod(HttpServletRequest request) {
        String email = "user@example.com";
        Long userId = 1L;
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        
        // ✅ 记录登录成功
        loginLogService.logLoginSuccess(userId, email, ipAddress, userAgent);
    }
}
```

---

## 📊 API 说明

### 1. logLoginSuccess - 记录登录成功

```java
/**
 * 记录登录成功日志
 * @param userId 用户ID
 * @param email 邮箱
 * @param ipAddress IP地址
 * @param userAgent 用户代理（浏览器信息）
 */
@Async
public void logLoginSuccess(Long userId, String email, String ipAddress, String userAgent)
```

**示例**:
```java
loginLogService.logLoginSuccess(
    1L,                          // 用户ID
    "user@example.com",          // 邮箱
    "192.168.1.100",            // IP地址
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64)..."  // User-Agent
);
```

---

### 2. logLoginFailure - 记录登录失败

```java
/**
 * 记录登录失败日志
 * @param email 邮箱
 * @param ipAddress IP地址
 * @param userAgent 用户代理
 * @param failureReason 失败原因
 */
@Async
public void logLoginFailure(String email, String ipAddress, String userAgent, String failureReason)
```

**示例**:
```java
loginLogService.logLoginFailure(
    "user@example.com",          // 邮箱
    "192.168.1.100",            // IP地址
    "Mozilla/5.0...",           // User-Agent
    "密码错误"                   // 失败原因
);
```

**常见失败原因**:
- `"密码错误"`
- `"用户不存在"`
- `"邮箱未验证"`
- `"账户已禁用"`

---

### 3. logAccountBlocked - 记录账户锁定

```java
/**
 * 记录账户锁定日志
 * @param email 邮箱
 * @param ipAddress IP地址
 * @param userAgent 用户代理
 */
@Async
public void logAccountBlocked(String email, String ipAddress, String userAgent)
```

**示例**:
```java
loginLogService.logAccountBlocked(
    "user@example.com",          // 邮箱
    "192.168.1.100",            // IP地址
    "Mozilla/5.0..."            // User-Agent
);
```

---

## 🔍 查询登录日志

### SQL 查询示例

```sql
-- 查询某用户的所有登录记录
SELECT * FROM login_logs 
WHERE email = 'user@example.com' 
ORDER BY created_at DESC;

-- 查询登录失败记录
SELECT * FROM login_logs 
WHERE login_status = 'FAILED' 
ORDER BY created_at DESC 
LIMIT 10;

-- 查询某IP的登录记录
SELECT * FROM login_logs 
WHERE ip_address = '192.168.1.100' 
ORDER BY created_at DESC;

-- 统计登录成功率
SELECT 
    login_status,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 2) as percentage
FROM login_logs
GROUP BY login_status;

-- 查询最近24小时的登录记录
SELECT * FROM login_logs 
WHERE created_at >= NOW() - INTERVAL '24 hours'
ORDER BY created_at DESC;
```

---

## 📱 设备类型识别

`LoginLogService` 会自动识别设备类型：

| User-Agent 包含 | 设备类型 |
|-----------------|---------|
| mobile, android, iphone | MOBILE |
| tablet, ipad | TABLET |
| 其他 | DESKTOP |

**示例**:
```
User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)
→ device_type = MOBILE

User-Agent: Mozilla/5.0 (iPad; CPU OS 14_0 like Mac OS X)
→ device_type = TABLET

User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)
→ device_type = DESKTOP
```

---

## ⚡ 性能说明

### 异步执行

所有日志记录方法都使用 `@Async` 注解，**不会阻塞主线程**：

```java
@Async  // 异步执行，不影响登录性能
public void logLoginSuccess(...) {
    // 日志记录逻辑
}
```

**优势**:
- ✅ 不影响登录响应时间
- ✅ 即使日志记录失败也不影响登录
- ✅ 提高系统吞吐量

---

## 🛡️ 安全建议

### 1. 保护内部接口

如果通过 HTTP 接口暴露日志记录功能，建议添加内部认证：

```java
@RestController
@RequestMapping("/internal/login-logs")
public class LoginLogController {
    
    @PostMapping("/success")
    public Result<Void> logSuccess(
            @RequestHeader("X-Internal-Token") String token,
            @RequestBody LoginLogRequest request) {
        
        // 验证内部调用 Token
        if (!isValidInternalToken(token)) {
            return Result.error(403, "Forbidden");
        }
        
        loginLogService.logLoginSuccess(...);
        return Result.success(null);
    }
}
```

### 2. 数据清理

定期清理旧的登录日志：

```sql
-- 删除 90 天前的日志
DELETE FROM login_logs 
WHERE created_at < NOW() - INTERVAL '90 days';
```

可以使用定时任务：

```java
@Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点执行
public void cleanOldLogs() {
    loginLogMapper.deleteOldLogs(90);  // 删除90天前的日志
}
```

---

## 📊 监控和告警

### 1. 异常登录检测

```sql
-- 检测同一IP短时间内多次失败
SELECT 
    ip_address,
    COUNT(*) as failure_count
FROM login_logs
WHERE login_status = 'FAILED'
  AND created_at >= NOW() - INTERVAL '1 hour'
GROUP BY ip_address
HAVING COUNT(*) >= 5;
```

### 2. 新设备登录提醒

```sql
-- 检测用户从新设备登录
SELECT DISTINCT
    user_id,
    email,
    device_type,
    ip_address
FROM login_logs
WHERE login_status = 'SUCCESS'
  AND created_at >= NOW() - INTERVAL '1 day'
  AND (user_id, device_type) NOT IN (
      SELECT user_id, device_type
      FROM login_logs
      WHERE created_at < NOW() - INTERVAL '1 day'
  );
```

---

## 🎯 完整示例

### 完整的登录流程示例

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private LoginLogService loginLogService;
    
    @Autowired
    private LoginAttemptService loginAttemptService;
    
    @PostMapping("/login")
    public ResponseEntity<Result<TokenVO>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        String email = request.getEmail();
        String password = request.getPassword();
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        try {
            // 1. 检查是否被锁定
            if (loginAttemptService.isBlocked(email)) {
                // ✅ 记录锁定日志
                loginLogService.logAccountBlocked(email, ipAddress, userAgent);
                
                long remainingTime = loginAttemptService.getBlockRemainingTime(email);
                return ResponseEntity.ok(
                    Result.error(403, "账户已被锁定，请在 " + remainingTime + " 秒后重试")
                );
            }
            
            // 2. 执行登录
            TokenVO token = authService.login(email, password);
            
            // 3. 清除失败记录
            loginAttemptService.loginSucceeded(email);
            
            // 4. ✅ 记录登录成功
            loginLogService.logLoginSuccess(
                token.getUserId(),
                email,
                ipAddress,
                userAgent
            );
            
            return ResponseEntity.ok(Result.success(token));
            
        } catch (BadCredentialsException e) {
            // 5. 记录失败次数
            loginAttemptService.loginFailed(email);
            
            // 6. ✅ 记录登录失败
            loginLogService.logLoginFailure(
                email,
                ipAddress,
                userAgent,
                "密码错误"
            );
            
            int remainingAttempts = loginAttemptService.getRemainingAttempts(email);
            return ResponseEntity.ok(
                Result.error(401, "密码错误，还剩 " + remainingAttempts + " 次尝试机会")
            );
            
        } catch (Exception e) {
            // 7. ✅ 记录其他失败原因
            loginLogService.logLoginFailure(
                email,
                ipAddress,
                userAgent,
                e.getMessage()
            );
            
            return ResponseEntity.ok(Result.error(500, "登录失败: " + e.getMessage()));
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个代理的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
```

---

## ✅ 总结

### 使用步骤

1. ✅ 启用异步支持（已完成）
2. ✅ 注入 `LoginLogService`
3. ✅ 在登录成功/失败/锁定时调用相应方法
4. ✅ 传入必要参数（userId, email, IP, User-Agent）

### 注意事项

- ✅ 所有方法都是异步的，不会阻塞主线程
- ✅ 日志记录失败不会影响业务流程
- ✅ 建议定期清理旧日志
- ✅ 可以基于日志实现异常检测和告警

---

**登录日志服务已就绪，可以开始使用！** 🎉

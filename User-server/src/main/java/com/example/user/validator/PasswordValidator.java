package com.example.user.validator;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-10
 * 密码强度验证器
 * 要求：至少8位，必须包含大小写字母、数字和特殊字符
 */
@Component
public class PasswordValidator {
    
    // 强密码：至少8位，包含大小写字母、数字和特殊字符
    private static final Pattern STRONG_PASSWORD = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );
    
    // 中等密码：至少6位，包含字母和数字
    private static final Pattern MEDIUM_PASSWORD = Pattern.compile(
        "^(?=.*[a-zA-Z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{6,}$"
    );
    
    // 常见弱密码列表
    private static final String[] COMMON_PASSWORDS = {
        "password", "123456", "12345678", "qwerty", "abc123", 
        "monkey", "1234567", "letmein", "trustno1", "dragon",
        "baseball", "111111", "iloveyou", "master", "sunshine",
        "ashley", "bailey", "passw0rd", "shadow", "123123"
    };
    
    /**
     * 验证密码强度（注册时使用，要求强密码）
     * @param password 密码
     * @return 错误信息，null表示验证通过
     */
    public String validate(String password) {
        if (password == null || password.isEmpty()) {
            return "密码不能为空";
        }
        
        if (password.length() < 8) {
            return "密码长度至少8位";
        }
        
        if (password.length() > 50) {
            return "密码长度不能超过50位";
        }
        
        // 检查是否包含空格
        if (password.contains(" ")) {
            return "密码不能包含空格";
        }
        
        // 检查是否是常见弱密码
        String lowerPassword = password.toLowerCase();
        for (String commonPassword : COMMON_PASSWORDS) {
            if (lowerPassword.equals(commonPassword)) {
                return "密码过于简单，请使用更复杂的密码";
            }
        }
        
        // 检查是否包含小写字母
        if (!password.matches(".*[a-z].*")) {
            return "密码必须包含小写字母";
        }
        
        // 检查是否包含大写字母
        if (!password.matches(".*[A-Z].*")) {
            return "密码必须包含大写字母";
        }
        
        // 检查是否包含数字
        if (!password.matches(".*\\d.*")) {
            return "密码必须包含数字";
        }
        
        // 检查是否包含特殊字符
        if (!password.matches(".*[@$!%*?&].*")) {
            return "密码必须包含特殊字符 (@$!%*?&)";
        }
        return null;
    }
    
    /**
     * 检查密码强度等级
     * @param password 密码
     * @return 强度等级
     */
    public PasswordStrength getStrength(String password) {
        if (password == null || password.isEmpty()) {
            return PasswordStrength.WEAK;
        }
        
        if (STRONG_PASSWORD.matcher(password).matches()) {
            return PasswordStrength.STRONG;
        } else if (MEDIUM_PASSWORD.matcher(password).matches()) {
            return PasswordStrength.MEDIUM;
        } else {
            return PasswordStrength.WEAK;
        }
    }
    
    /**
     * 获取密码强度描述
     * @param password 密码
     * @return 强度描述
     */
    public String getStrengthDescription(String password) {
        PasswordStrength strength = getStrength(password);
        switch (strength) {
            case STRONG:
                return "强密码";
            case MEDIUM:
                return "中等密码";
            case WEAK:
                return "弱密码";
            default:
                return "未知";
        }
    }
    
    /**
     * 密码强度枚举
     */
    public enum PasswordStrength {
        WEAK,
        MEDIUM,
        STRONG
    }
}

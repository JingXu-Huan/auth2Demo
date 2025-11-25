package com.example.domain.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册DTO
 * 包含输入验证规则
 * 
 * @author Security Team
 * @version 1.0.0
 */
@Data
public class UserRegisterDTO {
    
    /**
     * 用户名
     * - 必填
     * - 长度3-50字符
     * - 只能包含字母、数字、下划线
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;
    
    /**
     * 邮箱
     * - 必填
     * - 必须是有效的邮箱格式
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;
    
    /**
     * 密码
     * - 必填
     * - 长度8-100字符
     * - 必须包含大小写字母、数字
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 100, message = "密码长度必须在8-100个字符之间")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
        message = "密码必须包含大写字母、小写字母和数字"
    )
    private String password;
}

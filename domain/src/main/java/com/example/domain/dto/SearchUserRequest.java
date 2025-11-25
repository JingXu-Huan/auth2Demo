package com.example.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 搜索用户请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchUserRequest {
    
    /**
     * 搜索类型：email, phone
     */
    @NotNull(message = "搜索类型不能为空")
    private String searchType;
    
    /**
     * 搜索关键词
     */
    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;
}

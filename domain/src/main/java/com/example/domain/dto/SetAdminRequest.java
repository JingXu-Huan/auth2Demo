package com.example.domain.dto;

import lombok.Data;

@Data
public class SetAdminRequest {

    private Long userId;

    /**
     * 操作：ADD 或 REMOVE
     */
    private String action;
}

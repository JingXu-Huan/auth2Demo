package com.example.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 组织信息 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String orgId;

    private String name;

    private String code;

    private String avatar;

    private String description;

    private Boolean enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

package com.example.domain.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDateTime;

@Node("Organization")
@Data
public class OrganizationNode {

    @Id
    @GeneratedValue
    private Long id;

    // 业务侧的组织ID（例如租户ID），对外使用
    private String orgId;

    // 组织名称（公司名称）
    private String name;

    // 组织编码，可用于搜索或集成第三方
    private String code;

    // LOGO / 图标
    private String avatar;

    private String description;

    private Boolean enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

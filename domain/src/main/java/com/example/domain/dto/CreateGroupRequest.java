package com.example.domain.dto;

import lombok.Data;
import com.example.domain.model.Group.JoinType;

import java.util.List;

@Data
public class CreateGroupRequest {

    private String name;

    private String description;

    private String avatar;

    private Integer maxMembers;

    private JoinType joinType;

    private List<Long> memberIds;

    // 当前实现中通过该字段指定群主ID，文档示例中未显式给出，可在前端从登录用户ID填充
    private Long ownerId;
}

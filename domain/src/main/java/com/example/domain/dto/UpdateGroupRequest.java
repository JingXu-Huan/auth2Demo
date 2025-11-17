package com.example.domain.dto;

import lombok.Data;
import com.example.domain.model.Group.JoinType;

@Data
public class UpdateGroupRequest {

    private String name;

    private String description;

    private String avatar;

    private String announcement;

    private JoinType joinType;
}

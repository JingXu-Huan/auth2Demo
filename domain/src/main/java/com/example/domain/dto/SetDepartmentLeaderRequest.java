package com.example.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设置部门负责人请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetDepartmentLeaderRequest {

    /**
     * 新的负责人用户ID
     */
    private Long leaderUserId;
}

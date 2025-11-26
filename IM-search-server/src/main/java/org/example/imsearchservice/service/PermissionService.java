package org.example.imsearchservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 权限服务：根据 userId 获取其可访问的 channelIds 和 deptIds。
 *
 * 当前为演示版本，返回固定的权限数据。
 * 生产环境应调用 im-service 或 user-service 获取真实权限。
 */
@Service
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);

    public List<Long> getUserChannelIds(Long userId) {
        // TODO: 调用 IM-service 或 Redis 查询用户加入的所有 channel
        log.debug("[permission] 查询用户 {} 的 channelIds（演示版本返回固定数据）", userId);
        return Arrays.asList(100L, 102L, 999L);
    }

    public List<Long> getUserDeptIds(Long userId) {
        // TODO: 调用 user-service 获取用户所属的部门路径
        log.debug("[permission] 查询用户 {} 的 deptIds（演示版本返回固定数据）", userId);
        return Arrays.asList(10L, 50L);
    }
}

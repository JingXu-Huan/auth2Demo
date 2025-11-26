package com.example.imgateway.session;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 会话管理器
 * 维护本地 UserId <-> Channel 映射，并在 Redis 中注册全局位置
 */
@Slf4j
@Component
public class SessionManager {

    // 本地会话: userId -> Channel
    private final ConcurrentHashMap<Long, Channel> userChannelMap = new ConcurrentHashMap<>();

    // 反向映射: channelId -> userId
    private final ConcurrentHashMap<String, Long> channelUserMap = new ConcurrentHashMap<>();

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String REDIS_KEY_PREFIX = "im:location:";
    private static final long SESSION_TTL_SECONDS = 7200; // 2小时

    /**
     * 添加会话
     */
    public void addSession(Long userId, Channel channel) {
        if (userId == null || channel == null) {
            return;
        }

        userChannelMap.put(userId, channel);
        channelUserMap.put(channel.id().asLongText(), userId);

        String key = REDIS_KEY_PREFIX + userId;
        String value = getLocalAddress();
        try {
            redisTemplate.opsForValue().set(key, value, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("写入Redis会话信息失败: key={}, value={}", key, value, e);
        }

        log.info("会话已添加: userId={}, channelId={}, remote={}",
                userId, channel.id().asShortText(), channel.remoteAddress());
    }

    /**
     * 移除会话（通常在连接断开时调用）
     */
    public void removeSession(Channel channel) {
        if (channel == null) {
            return;
        }

        String channelId = channel.id().asLongText();
        Long userId = channelUserMap.remove(channelId);
        if (userId != null) {
            userChannelMap.remove(userId);
            String key = REDIS_KEY_PREFIX + userId;
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                log.error("删除Redis会话信息失败: key={}", key, e);
            }

            log.info("会话已移除: userId={}, channelId={}", userId, channel.id().asShortText());
        }
    }

    /**
     * 根据userId获取Channel
     */
    public Channel getChannel(Long userId) {
        return userChannelMap.get(userId);
    }

    /**
     * 用户是否在线
     */
    public boolean isOnline(Long userId) {
        Channel channel = userChannelMap.get(userId);
        return channel != null && channel.isActive();
    }

    /**
     * 当前节点在线用户数
     */
    public int getOnlineCount() {
        return userChannelMap.size();
    }

    /**
     * 获取当前网关节点地址（ip:port）
     * 这里只做简单实现，如需多实例/容器部署可改为从配置或环境变量获取
     */
    private String getLocalAddress() {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            // 端口可以通过环境变量/配置注入，这里简单写死9090，需与netty.port一致
            return ip + ":" + 9090;
        } catch (UnknownHostException e) {
            log.warn("获取本机IP失败，使用localhost", e);
            return "localhost:9090";
        }
    }
}

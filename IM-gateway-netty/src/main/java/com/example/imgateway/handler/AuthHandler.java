package com.example.imgateway.handler;

import com.example.common.util.JwtUtil;
import com.example.im.protocol.IMProtocol;
import com.example.imgateway.session.SessionManager;
import io.jsonwebtoken.Claims;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 鉴权处理器
 * 
 * WebSocket连接建立后，客户端需发送首个TextFrame作为JWT Token
 * 鉴权通过后：
 *   1. 解析userId
 *   2. 绑定Session
 *   3. 从Pipeline中移除自身
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class AuthHandler extends SimpleChannelInboundHandler<IMProtocol.IMPacket> {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IMProtocol.IMPacket packet) throws Exception {
        IMProtocol.Header header = packet.getHeader();
        int command = header.getCommand();

        // 只处理AUTH指令，其它指令交给后续Handler
        if (command != IMProtocol.CommandType.AUTH_VALUE) {
            ctx.fireChannelRead(packet);
            return;
        }

        // 解析AuthRequest
        IMProtocol.AuthRequest authRequest = IMProtocol.AuthRequest.parseFrom(packet.getBody());
        String token = authRequest.getToken();
        String deviceId = authRequest.getDeviceId();
        
        log.info("收到认证请求: token={}, deviceId={}, deviceType={}", 
                token != null ? token.substring(0, Math.min(10, token.length())) + "..." : "null",
                deviceId, 
                authRequest.getDeviceType());

        Long userId = null;
        
        // 开发模式：如果 token 为空但 deviceId 以 "DEV_" 开头，直接使用 deviceId 中的 userId
        if ((token == null || token.isEmpty()) && deviceId != null && deviceId.startsWith("DEV_")) {
            try {
                // deviceId 格式: DEV_{userId}_{timestamp}
                String[] parts = deviceId.split("_");
                if (parts.length >= 2) {
                    userId = Long.parseLong(parts[1]);
                    log.info("开发模式认证: deviceId={}, userId={}", deviceId, userId);
                }
            } catch (NumberFormatException e) {
                log.warn("开发模式解析失败: deviceId={}", deviceId);
            }
        }
        
        // 正常模式：验证 token
        if (userId == null && token != null && !token.isEmpty()) {
            // 1. 校验Token
            if (!jwtUtil.validateToken(token)) {
                log.warn("Token验证失败, remote={}, tokenPrefix={}",
                        ctx.channel().remoteAddress(), safePrefix(token));
                writeAuthResponseAndClose(ctx, header, false, "TOKEN_INVALID", null);
                return;
            }

            // 2. 解析userId
            userId = jwtUtil.getUserIdFromToken(token);
        }
        
        if (userId == null) {
            log.warn("无法获取userId, remote={}", ctx.channel().remoteAddress());
            writeAuthResponseAndClose(ctx, header, false, "USER_NOT_FOUND", null);
            return;
        }

        // 3. 从Token和请求中解析deviceId，并做风控检查
        Claims claims = token != null && !token.isEmpty() ? jwtUtil.getClaimsFromToken(token) : null;
        String deviceIdFromToken = claims != null ? (String) claims.get("device_id") : null;
        String finalDeviceId = deviceIdFromToken != null && !deviceIdFromToken.isEmpty()
                ? deviceIdFromToken
                : deviceId;

        // 3.1 封禁检查：risk:ban:user:{userId}
        String banKey = "risk:ban:user:" + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(banKey))) {
            log.warn("用户被封禁, 拒绝连接: userId={}, remote={}", userId, ctx.channel().remoteAddress());
            writeAuthResponseAndClose(ctx, header, false, "BANNED", userId);
            return;
        }

        // 3.2 设备踢下线检查：auth:kick:{userId}:{deviceId}
        if (finalDeviceId != null && !finalDeviceId.isEmpty()) {
            String kickKey = "auth:kick:" + userId + ":" + finalDeviceId;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(kickKey))) {
                log.warn("设备已被踢下线, 拒绝连接: userId={}, deviceId={}, remote={}",
                        userId, finalDeviceId, ctx.channel().remoteAddress());
                writeAuthResponseAndClose(ctx, header, false, "KICKED", userId);
                return;
            }
        }

        // 4. 绑定会话
        sessionManager.addSession(userId, ctx.channel());

        // 5. 移除自身，后续消息不再经过AuthHandler
        ctx.pipeline().remove(this);

        // 6. 返回鉴权成功
        writeAuthResponse(ctx, header, true, "OK", userId);

        log.info("鉴权成功: userId={}, channelId={}", userId, ctx.channel().id().asShortText());
    }

    private String safePrefix(String token) {
        if (token == null) {
            return "null";
        }
        if (token.length() <= 10) {
            return token;
        }
        return token.substring(0, 10) + "...";
    }

    /**
     * 写入鉴权响应并关闭连接
     */
    private void writeAuthResponseAndClose(ChannelHandlerContext ctx,
                                           IMProtocol.Header header,
                                           boolean success,
                                           String message,
                                           Long userId) {
        writeAuthResponse(ctx, header, success, message, userId);
        ctx.close();
    }

    /**
     * 写入鉴权响应（不强制关闭连接）
     */
    private void writeAuthResponse(ChannelHandlerContext ctx,
                                   IMProtocol.Header header,
                                   boolean success,
                                   String message,
                                   Long userId) {
        IMProtocol.AuthResponse.Builder respBuilder = IMProtocol.AuthResponse.newBuilder()
                .setSuccess(success)
                .setMessage(message);
        if (userId != null) {
            respBuilder.setUserId(userId);
        }

        IMProtocol.AuthResponse authResponse = respBuilder.build();

        IMProtocol.Header respHeader = header.toBuilder()
                .setCommand(IMProtocol.CommandType.AUTH_VALUE)
                .build();

        IMProtocol.IMPacket respPacket = IMProtocol.IMPacket.newBuilder()
                .setHeader(respHeader)
                .setBody(authResponse.toByteString())
                .build();

        ctx.writeAndFlush(respPacket);
    }
}

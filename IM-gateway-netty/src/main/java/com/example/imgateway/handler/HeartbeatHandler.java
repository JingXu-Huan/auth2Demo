package com.example.imgateway.handler;

import com.example.im.protocol.IMProtocol;
import com.example.imgateway.session.SessionManager;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 心跳处理器
 *
 * - 接收客户端发送的 "PING" 文本心跳，回复 "PONG"
 * - 处理IdleStateEvent，在读空闲超时时关闭连接
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class HeartbeatHandler extends SimpleChannelInboundHandler<IMProtocol.IMPacket> {

    @Autowired
    private SessionManager sessionManager;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IMProtocol.IMPacket packet) throws Exception {
        IMProtocol.Header header = packet.getHeader();
        int command = header.getCommand();

        if (command != IMProtocol.CommandType.HEARTBEAT_VALUE) {
            // 非心跳消息，交给后续Handler处理
            ctx.fireChannelRead(packet);
            return;
        }

        IMProtocol.HeartbeatRequest heartbeatRequest = IMProtocol.HeartbeatRequest.parseFrom(packet.getBody());

        if (log.isDebugEnabled()) {
            log.debug("收到心跳: channelId={}, ts={}",
                    ctx.channel().id().asShortText(), heartbeatRequest.getTimestamp());
        }

        // 构造心跳响应
        IMProtocol.HeartbeatResponse heartbeatResponse = IMProtocol.HeartbeatResponse.newBuilder()
                .setTimestamp(System.currentTimeMillis())
                .setMessage("PONG")
                .build();

        IMProtocol.Header respHeader = header.toBuilder()
                .setCommand(IMProtocol.CommandType.HEARTBEAT_VALUE)
                .build();

        IMProtocol.IMPacket respPacket = IMProtocol.IMPacket.newBuilder()
                .setHeader(respHeader)
                .setBody(heartbeatResponse.toByteString())
                .build();

        ctx.writeAndFlush(respPacket);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                log.warn("连接读空闲超时，即将关闭: channelId={}, remote={}",
                        ctx.channel().id().asShortText(), ctx.channel().remoteAddress());
                // 从SessionManager中移除会话
                sessionManager.removeSession(ctx.channel());
                ctx.close();
                return;
            }
        }
        super.userEventTriggered(ctx, evt);
    }
}

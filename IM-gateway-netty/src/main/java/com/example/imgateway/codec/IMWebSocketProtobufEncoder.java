package com.example.imgateway.codec;

import com.example.im.protocol.IMProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;

/**
 * 将 IMProtocol.IMPacket 编码为 WebSocket 二进制帧
 */
public class IMWebSocketProtobufEncoder extends MessageToMessageEncoder<IMProtocol.IMPacket> {

    @Override
    protected void encode(ChannelHandlerContext ctx, IMProtocol.IMPacket msg, List<Object> out) throws Exception {
        byte[] bytes = msg.toByteArray();
        ByteBufAllocator alloc = ctx.alloc();
        ByteBuf buffer = alloc.buffer(bytes.length);
        buffer.writeBytes(bytes);
        out.add(new BinaryWebSocketFrame(buffer));
    }
}

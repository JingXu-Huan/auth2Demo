package com.example.imgateway.codec;

import com.example.im.protocol.IMProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;

/**
 * 将 WebSocket 二进制帧解码为 IMProtocol.IMPacket
 */
public class IMWebSocketProtobufDecoder extends MessageToMessageDecoder<BinaryWebSocketFrame> {

    @Override
    protected void decode(ChannelHandlerContext ctx, BinaryWebSocketFrame frame, List<Object> out) throws Exception {
        ByteBuf content = frame.content();
        byte[] bytes = new byte[content.readableBytes()];
        content.getBytes(content.readerIndex(), bytes);
        IMProtocol.IMPacket packet = IMProtocol.IMPacket.parseFrom(bytes);
        out.add(packet);
    }
}

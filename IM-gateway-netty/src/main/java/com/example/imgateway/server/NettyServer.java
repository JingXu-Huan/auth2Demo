package com.example.imgateway.server;

import com.example.imgateway.codec.IMWebSocketProtobufDecoder;
import com.example.imgateway.codec.IMWebSocketProtobufEncoder;
import com.example.imgateway.config.NettyConfig;
import com.example.imgateway.handler.AuthHandler;
import com.example.imgateway.handler.HeartbeatHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * Netty Server 启动类
 * 负责初始化Boss/Worker线程组、ChannelPipeline等
 */
@Slf4j
@Component
public class NettyServer {

    @Autowired
    private NettyConfig nettyConfig;

    @Autowired
    private AuthHandler authHandler;

    @Autowired
    private HeartbeatHandler heartbeatHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    /**
     * 启动Netty服务器（非阻塞）
     */
    public void start() {
        bossGroup = new NioEventLoopGroup(nettyConfig.getBossThreads());
        workerGroup = new NioEventLoopGroup(nettyConfig.getWorkerThreads());

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // 操作系统参数调优
                    .option(ChannelOption.SO_BACKLOG, nettyConfig.getBacklog())
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();

                            // 1. 空闲检测（读空闲180秒）
                            p.addLast(new IdleStateHandler(
                                    nettyConfig.getReaderIdleTime(),
                                    nettyConfig.getWriterIdleTime(),
                                    nettyConfig.getAllIdleTime(),
                                    TimeUnit.SECONDS));

                            // 2. HTTP编解码 & 聚合
                            p.addLast(new HttpServerCodec());
                            p.addLast(new HttpObjectAggregator(nettyConfig.getMaxFrameLength()));
                            p.addLast(new ChunkedWriteHandler());

                            // 3. WebSocket协议处理（路径固定为 /ws）
                            p.addLast(new WebSocketServerProtocolHandler("/ws", null, true));

                            // 4. Protobuf编解码：BinaryWebSocketFrame <-> IMProtocol.IMPacket
                            p.addLast(new IMWebSocketProtobufDecoder());
                            p.addLast(new IMWebSocketProtobufEncoder());

                            // 5. 业务Handler（处理IMPacket）
                            p.addLast(authHandler);       // 鉴权
                            p.addLast(heartbeatHandler);  // 心跳处理 & 空闲关闭

                            log.debug("Channel pipeline initialized: {}", ch.id().asShortText());
                        }
                    });

            ChannelFuture future = bootstrap.bind(nettyConfig.getPort()).sync();
            serverChannel = future.channel();

            log.info("Netty Server 启动成功，监听端口: {}，bossThreads={}，workerThreads={}",
                    nettyConfig.getPort(), nettyConfig.getBossThreads(), nettyConfig.getWorkerThreads());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Netty Server 启动被中断", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("Netty Server 启动失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 优雅关闭Netty服务器
     */
    @PreDestroy
    public void shutdown() {
        log.info("Netty Server 正在关闭...");
        try {
            if (serverChannel != null) {
                serverChannel.close().syncUninterruptibly();
            }
        } finally {
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
        }
        log.info("Netty Server 已关闭");
    }
}

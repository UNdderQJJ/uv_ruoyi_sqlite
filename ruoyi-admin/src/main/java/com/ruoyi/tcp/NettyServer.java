package com.ruoyi.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Netty TCP 服务器
 */
@Component
public class NettyServer {

    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

    @Value("${netty.port:8030}")
    private int port;

    @Value("${netty.max-connections:1000}")
    private int maxConnections;

    @Value("${netty.connection-timeout:30}")
    private int connectionTimeout;

    @Autowired
    private NettyServerHandler nettyServerHandler;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private ChannelFuture channelFuture;

    @PostConstruct
    public void start() {
        // 使用新线程启动，避免阻塞主线程
        new Thread(() -> {
            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, maxConnections)
                        .option(ChannelOption.SO_REUSEADDR, true)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .childOption(ChannelOption.SO_RCVBUF, 1024 * 1024)
                        .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline()
                                        // 空闲状态检测
                                        .addLast(new IdleStateHandler(connectionTimeout, 0, 0, TimeUnit.SECONDS))
                                        // 基于分隔符的解码器，使用换行符作为消息分隔符
                                        .addLast(new DelimiterBasedFrameDecoder(10 * 1024 * 1024, 
                                                Unpooled.wrappedBuffer("\n".getBytes())))
                                        // 字符串编解码器
                                        .addLast(new StringDecoder(CharsetUtil.UTF_8))
                                        .addLast(new StringEncoder(CharsetUtil.UTF_8))
                                        // 自定义的业务处理器
                                        .addLast(nettyServerHandler);
                            }
                        });

                log.info("[Netty-Server] 服务器启动中...");
                channelFuture = bootstrap.bind(port).sync();
                log.info("[Netty-Server] 服务器启动成功，正在监听端口: {}", port);

                channelFuture.channel().closeFuture().sync();

            } catch (InterruptedException e) {
                log.error("[Netty-Server] 服务器启动时发生异常!", e);
                Thread.currentThread().interrupt();
            } finally {
                log.info("[Netty-Server] 服务器正在关闭...");
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
                log.info("[Netty-Server] 服务器已关闭。");
            }
        }, "Netty-Server-Thread").start();
    }

    @PreDestroy
    public void destroy() {
        if (channelFuture != null) {
            channelFuture.channel().close();
        }
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}

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
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Netty TCP 服务器 (标准版)
 */
@Component
public class NettyServer {

    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

    @Value("${netty.port:9999}")
    private int port;

    @Autowired
    private NettyServerHandler nettyServerHandler;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1); // 推荐 Boss 线程组为 1
    private final EventLoopGroup workerGroup = new NioEventLoopGroup(); // Worker 线程组默认为 CPU 核心数 * 2
    private ChannelFuture channelFuture;

    @PostConstruct
    public void start() {
        // 使用新线程启动，避免阻塞主线程
        new Thread(() -> {
            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {

                                // 1. 【核心变更】添加基于分隔符的解码器
                                //    我们选择换行符 \n 作为消息的分隔符
                                //    Unpooled.wrappedBuffer("\n".getBytes()) 就是把换行符包装成 Netty 的 Buffer 对象
                                //    10 * 1024 * 1024 是指单条消息的最大长度，防止内存溢出，这个保护依然需要。
                                ch.pipeline().addLast(new DelimiterBasedFrameDecoder(10 * 1024 * 1024, Unpooled.wrappedBuffer("\n".getBytes())));

                                // 2. 【移除】不再需要长度字段的编解码器，将下面这两行注释或删除
                                // ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                                // ch.pipeline().addLast(new LengthFieldPrepender(4));

                                // 3. 字符串编解码器依然需要，它们的位置不变
                                ch.pipeline().addLast(new StringDecoder(CharsetUtil.UTF_8));
                                ch.pipeline().addLast(new StringEncoder(CharsetUtil.UTF_8));

                                // 4. 自定义的业务处理器也不变
                                ch.pipeline().addLast(nettyServerHandler);
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

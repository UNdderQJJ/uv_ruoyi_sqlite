package com.ruoyi.business.service.notification;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.business.events.DataPoolStateChangedEvent;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

/**
 * WebSocket通知服务
 * 基于Netty实现，用于向前端推送数据池状态变更消息
 */
@Service
public class WebSocketNotificationService {
    
    private static final Logger log = LoggerFactory.getLogger(WebSocketNotificationService.class);
    
    @Value("${websocket.notification.port:8761}")
    private int port;
    
    @Value("${websocket.notification.path:/ws/notification}")
    private String path;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    
    // 存储所有连接的WebSocket会话
    private final Map<String, Channel> sessions = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void start() throws Exception {
        log.info("启动WebSocket通知服务: port={}, path={}", port, path);
        
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // 增加详细的日志记录
                            pipeline.addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
                                    log.debug("通道注册: remoteAddress={}", ctx.channel().remoteAddress());
                                    super.channelRegistered(ctx);
                                }

                                @Override
                                public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
                                    log.debug("通道注销: remoteAddress={}", ctx.channel().remoteAddress());
                                    super.channelUnregistered(ctx);
                                }
                            });
                            
                            // HTTP编解码器
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new ChunkedWriteHandler());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            
                            // WebSocket协议处理器
                            pipeline.addLast(new WebSocketServerProtocolHandler(path, null, true));
                            
                            // 自定义处理器
                            pipeline.addLast(new WebSocketNotificationHandler());
                        }
                    });
            
            // 绑定到所有网络接口
            ChannelFuture future = bootstrap.bind(new InetSocketAddress("0.0.0.0", port)).sync();
            serverChannel = future.channel();
            log.info("WebSocket通知服务启动成功: port={}, path={}, localAddress={}", 
                     port, path, serverChannel.localAddress());
            
        } catch (Exception e) {
            log.error("启动WebSocket通知服务失败: port={}, path={}", port, path, e);
            throw e;
        }
    }
    
    @PreDestroy
    public void stop() {
        log.info("停止WebSocket通知服务");
        
        // 关闭所有连接
        sessions.values().forEach(channel -> {
            if (channel.isActive()) {
                channel.close();
            }
        });
        sessions.clear();
        
        // 关闭服务器
        if (serverChannel != null) {
            serverChannel.close();
        }
        
        // 关闭线程组
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
    
    /**
     * 向所有连接的客户端推送状态变更消息
     */
    public void notifyStateChanged(DataPoolStateChangedEvent event) {
        if (event == null || !event.hasStateChanged()) {
            return;
        }
        
        String message = createStateChangeMessage(event);
        log.info("推送状态变更消息: poolId={}, message={}", event.getPoolId(), message);
        
        // 向所有连接的客户端推送消息
        sessions.values().forEach(channel -> {
            if (channel.isActive()) {
                channel.writeAndFlush(new TextWebSocketFrame(message));
            }
        });
    }
    
    /**
     * 创建状态变更消息
     */
    private String createStateChangeMessage(DataPoolStateChangedEvent event) {
        // 使用 HashMap 替代 Map.of，避免 null 值导致的问题
        Map<String, Object> message = new HashMap<>();
        message.put("type", "stateChange");
        message.put("poolId", event.getPoolId());
        message.put("poolName", event.getPoolName() != null ? event.getPoolName() : "未知数据池");
        message.put("sourceType", event.getSourceType() != null ? event.getSourceType() : "未知类型");

        // 连接状态处理
        Map<String, Object> connectionStateMap = new HashMap<>();
        connectionStateMap.put("old", event.getOldConnectionState() != null ? event.getOldConnectionState().getCode() : null);
        connectionStateMap.put("new", event.getNewConnectionState() != null ? event.getNewConnectionState().getCode() : null);
        connectionStateMap.put("oldInfo", event.getOldConnectionState() != null ? event.getOldConnectionState().getInfo() : "未知状态");
        connectionStateMap.put("newInfo", event.getNewConnectionState() != null ? event.getNewConnectionState().getInfo() : "未知状态");
        message.put("connectionState", connectionStateMap);

        // 运行状态处理
        Map<String, Object> poolStatusMap = new HashMap<>();
        poolStatusMap.put("old", event.getOldPoolStatus() != null ? event.getOldPoolStatus().getCode() : null);
        poolStatusMap.put("new", event.getNewPoolStatus() != null ? event.getNewPoolStatus().getCode() : null);
        poolStatusMap.put("oldInfo", event.getOldPoolStatus() != null ? event.getOldPoolStatus().getInfo() : "未知状态");
        poolStatusMap.put("newInfo", event.getNewPoolStatus() != null ? event.getNewPoolStatus().getInfo() : "未知状态");
        message.put("poolStatus", poolStatusMap);

        message.put("timestamp", event.getTimestamp());

        return JSON.toJSONString(message);
    }
    
    /**
     * 获取当前连接的客户端数量
     */
    public int getConnectedClientCount() {
        return sessions.size();
    }
    
    /**
     * 获取所有会话ID
     */
    public java.util.Set<String> getAllSessionIds() {
        return sessions.keySet();
    }
    
    /**
     * WebSocket通知处理器
     */
    private class WebSocketNotificationHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
            if (frame instanceof TextWebSocketFrame) {
                String message = ((TextWebSocketFrame) frame).text();
                log.debug("收到WebSocket消息: channelId={}, message={}", ctx.channel().id(), message);
                
                // 这里可以处理前端发送的消息，比如订阅特定数据池的状态变更
                // 目前简单回复确认消息
                String response = createConnectionMessage(message);
                ctx.channel().writeAndFlush(new TextWebSocketFrame(response));
            }
        }
        
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();
            String channelId = channel.id().asLongText();
            sessions.put(channelId, channel);
            log.info("WebSocket连接建立: channelId={}, remoteAddress={}", 
                    channelId, channel.remoteAddress());
            
            // 发送连接成功消息
            String message = createConnectionMessage("连接成功");
            channel.writeAndFlush(new TextWebSocketFrame(message));
        }
        
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();
            String channelId = channel.id().asLongText();
            sessions.remove(channelId);
            log.info("WebSocket连接关闭: channelId={}", channelId);
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("WebSocket处理异常: channelId={}", ctx.channel().id(), cause);
            ctx.close();
        }
        
        /**
         * 创建连接消息
         */
        private String createConnectionMessage(String content) {
            Map<String, Object> message = Map.of(
                    "type", "connection",
                    "content", content,
                    "timestamp", System.currentTimeMillis()
            );
            return JSON.toJSONString(message);
        }
    }
}

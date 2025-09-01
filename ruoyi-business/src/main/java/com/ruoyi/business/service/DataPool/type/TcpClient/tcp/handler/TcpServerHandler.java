package com.ruoyi.business.service.DataPool.type.TcpClient.tcp.handler;

import com.ruoyi.business.service.DataPool.type.TcpClient.tcp.TcpServerProvider;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP 服务端处理器
 * 处理客户端连接和数据接收
 */
public class TcpServerHandler extends SimpleChannelInboundHandler<String> {
    
    private static final Logger log = LoggerFactory.getLogger(TcpServerHandler.class);
    
    private final TcpServerProvider provider;
    
    public TcpServerHandler(TcpServerProvider provider) {
        this.provider = provider;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("客户端连接建立: {}", ctx.channel().remoteAddress());
        provider.onClientConnected(ctx.channel());
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("客户端连接断开: {}", ctx.channel().remoteAddress());
        provider.onClientDisconnected();
        super.channelInactive(ctx);
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (msg != null && !msg.trim().isEmpty()) {
            // 去除可能的换行符
            String cleanMsg = msg.trim();
            provider.onDataReceived(cleanMsg);
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("处理客户端数据时发生异常: {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}

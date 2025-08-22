package com.ruoyi.business.tcp.handler;

import com.ruoyi.business.tcp.TcpClientProvider;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty 客户端入站处理器
 */
public class TcpClientHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger log = LoggerFactory.getLogger(TcpClientHandler.class);

    private final TcpClientProvider provider;

    public TcpClientHandler(TcpClientProvider provider) {
        this.provider = provider;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        provider.onMessage(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        provider.onChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("[TcpClientHandler] 通道异常: {}", cause.getMessage(), cause);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idle) {
            switch (idle.state()) {
                case READER_IDLE -> {
                    log.warn("[TcpClientHandler] 读空闲超时，关闭通道以触发重连");
                    ctx.close();
                    return;
                }
                default -> { /* ignore */ }
            }
        }
        super.userEventTriggered(ctx, evt);
    }
}



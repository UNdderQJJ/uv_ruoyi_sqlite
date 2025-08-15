package com.ruoyi.tcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.core.domain.model.TcpRequest;
import com.ruoyi.common.utils.StringUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Netty TCP 业务处理器
 * 负责路由和分发TCP请求到相应的业务处理器
 */
@Component
@ChannelHandler.Sharable // 标注该 Handler 可以被多个 Channel 安全地共享，因为它是单例的
public class NettyServerHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger log = LoggerFactory.getLogger(NettyServerHandler.class);

    // 注入 Jackson 的 ObjectMapper，用于 JSON 序列化和反序列化
    @Autowired
    private ObjectMapper objectMapper;

    // 注入用户管理处理器
    @Autowired
    private UserManagementHandler userManagementHandler;

    // 注入角色管理处理器
    @Autowired
    private RoleManagementHandler roleManagementHandler;

    // 注入菜单管理处理器
    @Autowired
    private MenuManagementHandler menuManagementHandler;

    // 注入权限控制处理器
    @Autowired
    private AuthManagementHandler authManagementHandler;

    /**
     * 当从客户端接收到消息时被调用
     *
     * @param ctx Channel 的上下文，可以用来发送消息
     * @param msg 经过 StringDecoder 解码后的消息字符串 (JSON)
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        // 增加对空消息的判断，让服务器更健壮
        if (msg == null || msg.trim().isEmpty()) {
            log.warn("[Netty-Handler] 收到来自客户端 [{}] 的空消息或空白消息，已忽略。", ctx.channel().remoteAddress());
            return; // 如果是空消息，直接忽略并返回，不执行后续逻辑
        }

        String clientAddress = ctx.channel().remoteAddress().toString();
        log.debug("[Netty-Handler] 收到来自客户端 [{}] 的消息: {}", clientAddress, msg);

        TcpResponse response;
        String requestId = null;
        try {
            // 1. 解析请求
            TcpRequest request = objectMapper.readValue(msg, TcpRequest.class);
            String path = request.getPath();
            String body = request.getBody();
            requestId = request.getId();

            if (StringUtils.isEmpty(path)) {
                throw new IllegalArgumentException("请求路径 [path] 不能为空");
            }

            // 2. 根据 Path 路由到不同的业务逻辑
            if (path.startsWith("/system/user/")) {
                // 用户管理相关请求
                response = userManagementHandler.handleUserRequest(path, body);
            } else if (path.startsWith("/system/role/")) {
                // 角色管理相关请求
                response = roleManagementHandler.handleRoleRequest(path, body);
            } else if (path.startsWith("/system/menu/")) {
                // 菜单管理相关请求
                response = menuManagementHandler.handleMenuRequest(path, body);
            } else if (path.startsWith("/auth/")) {
                // 权限控制相关请求
                response = authManagementHandler.handleAuthRequest(path, body);
            } else {
                log.warn("[Netty-Handler] 客户端 [{}] 请求了未知的路径: {}", clientAddress, path);
                response = TcpResponse.error("请求的路径不存在: " + path);
            }

        } catch (Exception e) {
            log.error("[Netty-Handler] 处理客户端 [{}] 请求时发生异常", clientAddress, e);
            response = TcpResponse.error("服务器内部错误: " + e.getMessage());
        }

        // 3. 将响应写回客户端时，在末尾加上换行符
        try {
            // 回显 requestId 到响应顶层
            if (requestId != null && response != null && response.getResult() != null) {
                response.getResult().put("id", requestId);
            }
            String responseJson = objectMapper.writeValueAsString(response.getResult());
            log.debug("[Netty-Handler] 发送响应给客户端 [{}]: {}", clientAddress, responseJson);
            // 在 JSON 字符串末尾拼接一个换行符再发送
            ctx.writeAndFlush(responseJson + "\n");
        } catch (JsonProcessingException e) {
            log.error("[Netty-Handler] 序列化响应失败", e);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("[Netty-Handler] 客户端 [{}] 连接成功", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.warn("[Netty-Handler] 客户端 [{}] 断开连接", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("[Netty-Handler] 客户端 [{}] 连接出现异常", ctx.channel().remoteAddress(), cause);
        // 发生异常时，关闭连接
        ctx.close();
    }
}
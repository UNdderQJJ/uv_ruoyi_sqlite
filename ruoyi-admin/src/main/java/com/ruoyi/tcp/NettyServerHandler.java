package com.ruoyi.tcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.core.domain.model.TcpRequest;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.tcp.business.Task.TaskDeviceLinkManagementHandler;
import com.ruoyi.tcp.system.AuthManagementHandler;
import com.ruoyi.tcp.system.MenuManagementHandler;
import com.ruoyi.tcp.system.RoleManagementHandler;
import com.ruoyi.tcp.system.UserManagementHandler;
import com.ruoyi.tcp.system.VersionManagementHandler;
import com.ruoyi.tcp.business.DataPool.DataPoolManagementHandler;
import com.ruoyi.tcp.business.DataPoolItem.DataPoolItemManagementHandler;
import com.ruoyi.tcp.business.ArchivedDataPool.ArchivedDataPoolItemManagementHandler;
import com.ruoyi.tcp.business.DataPoolTemplate.DataPoolTemplateManagementHandler;
import com.ruoyi.tcp.business.Device.DeviceManagementHandler;
import com.ruoyi.tcp.business.DeviceFileConfig.DeviceFileConfigManagementHandler;
import com.ruoyi.tcp.business.Task.TaskInfoManagementHandler;
import com.ruoyi.tcp.business.SystemLog.SystemLogManagementHandler;
import com.ruoyi.tcp.business.DataInspect.DataInspectManagementHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Netty TCP 业务处理器
 * 负责路由和分发TCP请求到相应的业务处理器
 */
@Component
@ChannelHandler.Sharable
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

    // 注入数据池管理处理器
    @Autowired
    private DataPoolManagementHandler dataPoolManagementHandler;

    // 注入数据池热数据管理处理器
    @Autowired
    private DataPoolItemManagementHandler dataPoolItemManagementHandler;

    // 注入归档数据池项目管理处理器
    @Autowired
    private ArchivedDataPoolItemManagementHandler archivedDataPoolItemManagementHandler;

    // 注入数据池模板管理处理器
    @Autowired
    private DataPoolTemplateManagementHandler dataPoolTemplateManagementHandler;

    // 注入设备管理处理器
    @Autowired
    private DeviceManagementHandler deviceManagementHandler;

    // 注入设备文件配置管理处理器
    @Autowired
    private DeviceFileConfigManagementHandler deviceFileConfigManagementHandler;

    // 注入任务中心处理器
    @Autowired
    private TaskInfoManagementHandler taskInfoManagementHandler;

    // 注入任务管理处理器
    @Autowired
    private TaskDeviceLinkManagementHandler taskDeviceLinkManagementHandler;

    // 注入系统日志处理器
    @Autowired
    private SystemLogManagementHandler systemLogManagementHandler;

    // 注入产品质检处理器
    @Autowired
    private DataInspectManagementHandler dataInspectManagementHandler;

    // 注入版本管理处理器
    @Autowired
    private VersionManagementHandler versionManagementHandler;

    // 注入Spring的TaskExecutor，用于异步处理业务逻辑
    @Resource
    private TaskExecutor taskExecutor;

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
            return;
        }

        String clientAddress = ctx.channel().remoteAddress().toString();
        log.debug("[Netty-Handler] 收到来自客户端 [{}] 的消息: {}", clientAddress, msg);

        try {
            // 1. 解析请求
            TcpRequest request = objectMapper.readValue(msg, TcpRequest.class);
            String path = request.getPath();
            String body = request.getBody();
            String requestId = request.getId();

            if (StringUtils.isEmpty(path)) {
                throw new IllegalArgumentException("请求路径 [path] 不能为空");
            }

            // 2. 异步处理业务逻辑，避免阻塞EventLoop线程
            CompletableFuture.supplyAsync(() -> {
                try {
                    return processBusinessRequest(path, body, requestId, clientAddress);
                } catch (Exception e) {
                    log.error("[Netty-Handler] 处理客户端 [{}] 请求时发生异常", clientAddress, e);
                    return TcpResponse.error("服务器内部错误: " + e.getMessage());
                }
            }, taskExecutor).thenAccept(response -> {
                // 在EventLoop线程中发送响应
                sendResponse(ctx, response, requestId, clientAddress);
            }).exceptionally(throwable -> {
                log.error("[Netty-Handler] 异步处理请求时发生异常", throwable);
                TcpResponse errorResponse = TcpResponse.error("服务器内部错误: " + throwable.getMessage());
                sendResponse(ctx, errorResponse, requestId, clientAddress);
                return null;
            });

        } catch (Exception e) {
            log.error("[Netty-Handler] 解析客户端 [{}] 请求时发生异常", clientAddress, e);
            TcpResponse errorResponse = TcpResponse.error("请求格式错误: " + e.getMessage());
            sendResponse(ctx, errorResponse, null, clientAddress);
        }
    }

    /**
     * 处理业务请求（在业务线程池中执行）
     */
    private TcpResponse processBusinessRequest(String path, String body, String requestId, String clientAddress) throws Exception {
        TcpResponse response;
        
        // 根据 Path 路由到不同的业务逻辑
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
        } else if (path.startsWith("/business/dataPool/")) {
            // 数据池管理相关请求
            response = dataPoolManagementHandler.handleDataPoolRequest(path, body);
        } else if (path.startsWith("/business/dataPoolItem/")) {
            // 数据池热数据管理相关请求
            response = dataPoolItemManagementHandler.handleDataPoolItemRequest(path, body);
        } else if (path.startsWith("/business/archivedDataPoolItem/")) {
            // 归档数据池项目管理相关请求
            response = archivedDataPoolItemManagementHandler.handleArchivedDataPoolItemRequest(path, body);
        } else if (path.startsWith("/business/dataPoolTemplate/")) {
            // 数据池模板管理相关请求
            response = dataPoolTemplateManagementHandler.handleDataPoolTemplateRequest(path, body);
        } else if (path.startsWith("/business/device/")) {
            // 设备管理相关请求
            response = deviceManagementHandler.handleDeviceRequest(path, body);
        } else if (path.startsWith("/business/deviceFileConfig/")) {
            // 设备文件配置管理相关请求
            response = deviceFileConfigManagementHandler.handleDeviceFileConfigRequest(path, body);
        }else if (path.startsWith("/business/taskInfo/link/")) {
            // 任务设备关联表相关请求
            response = taskDeviceLinkManagementHandler.handleTaskDeviceLinkRequest(path, body);
        } else if (path.startsWith("/business/taskInfo/")) {
            // 任务中心相关请求
            response = taskInfoManagementHandler.handleTaskInfoRequest(path, body);
        } else if (path.startsWith("/business/systemLog/")) {
            // 系统日志相关请求
            response = systemLogManagementHandler.handleSystemLogRequest(path, body);
        } else if (path.startsWith("/business/dataInspect/")) {
            // 产品质检相关请求
            response = dataInspectManagementHandler.handleRequest(path, body);
        } else if (path.startsWith("/version/")) {
            // 版本管理相关请求
            response = versionManagementHandler.handleVersionRequest(path, body);
        } else {
            log.warn("[Netty-Handler] 客户端 [{}] 请求了未知的路径: {}", clientAddress, path);
            response = TcpResponse.error("请求的路径不存在: " + path);
        }
        
        return response;
    }

    /**
     * 发送响应（在EventLoop线程中执行）
     */
    private void sendResponse(ChannelHandlerContext ctx, TcpResponse response, String requestId, String clientAddress) {
        try {
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            if (ObjectUtils.isNotEmpty(response.getCode())) {
                responseData.put("code", response.getCode());
            }else {
                responseData.put("code", 200);
            }
            responseData.put("message", response.getMessage());
            responseData.put("data", response.getResult());
            
            // 回显 requestId 到响应顶层
            if (requestId != null) {
                responseData.put("id", requestId);
            }
            
            String responseJson = objectMapper.writeValueAsString(responseData);
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
package com.ruoyi.tcp.stdio;

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
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 * STDIO 通信服务
 * 当 comm-mode 为 stdio 时启动，监听 System.in 并将响应写入 System.out
 */
@Service
@ConditionalOnProperty(name = "app.comm-mode", havingValue = "stdio")
public class StdioCommunicator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StdioCommunicator.class);

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

    @Override
    public void run(String... args) throws Exception {
        log.info("STDIO通信模式已启动，等待来自Electron的数据...");

        // 启动一个新线程来读取System.in，防止阻塞主线程
        new Thread(() -> {
            try (Scanner scanner = new Scanner(System.in, "UTF-8")) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    try {
                        // 处理 STDIO 请求
                        handleStdioRequest(line);
                    } catch (Exception e) {
                        log.error("处理STDIO数据异常", e);
                    }
                }
            } catch (Exception e) {
                log.error("STDIO读取线程异常", e);
            }
        }, "STDIO-Reader-Thread").start();
    }

    /**
     * 处理 STDIO 请求
     */
    private void handleStdioRequest(String msg) {
        // 增加对空消息的判断，让服务器更健壮
        if (msg == null || msg.trim().isEmpty()) {
            log.warn("[STDIO-Handler] 收到空消息或空白消息，已忽略。");
            return;
        }

        log.debug("[STDIO-Handler] 收到消息: {}", msg);

        try {
            // 1. 解析请求
            TcpRequest request = objectMapper.readValue(msg, TcpRequest.class);
            String path = request.getPath();
            String body = request.getBody();
            String requestId = request.getId();

            if (StringUtils.isEmpty(path)) {
                throw new IllegalArgumentException("请求路径 [path] 不能为空");
            }

            // 2. 异步处理业务逻辑，避免阻塞主线程
            CompletableFuture.supplyAsync(() -> {
                try {
                    return processBusinessRequest(path, body, requestId);
                } catch (Exception e) {
                    log.error("[STDIO-Handler] 处理请求时发生异常", e);
                    return TcpResponse.error("服务器内部错误: " + e.getMessage());
                }
            }, taskExecutor).thenAccept(response -> {
                // 发送响应到标准输出
                sendStdioResponse(response, requestId);
            }).exceptionally(throwable -> {
                log.error("[STDIO-Handler] 异步处理请求时发生异常", throwable);
                TcpResponse errorResponse = TcpResponse.error("服务器内部错误: " + throwable.getMessage());
                sendStdioResponse(errorResponse, requestId);
                return null;
            });

        } catch (Exception e) {
            log.error("[STDIO-Handler] 解析请求时发生异常", e);
            TcpResponse errorResponse = TcpResponse.error("请求格式错误: " + e.getMessage());
            sendStdioResponse(errorResponse, null);
        }
    }

    /**
     * 处理业务请求（在业务线程池中执行）
     */
    private TcpResponse processBusinessRequest(String path, String body, String requestId) throws Exception {
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
        } else if (path.startsWith("/business/taskInfo/link/")) {
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
            log.warn("[STDIO-Handler] 请求了未知的路径: {}", path);
            response = TcpResponse.error("请求的路径不存在: " + path);
        }
        
        return response;
    }

    /**
     * 发送响应到标准输出
     */
    private void sendStdioResponse(TcpResponse response, String requestId) {
        try {
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("code", response.getCode());
            responseData.put("message", response.getMessage());
            responseData.put("data", response.getResult());
            
            // 回显 requestId 到响应顶层
            if (requestId != null) {
                responseData.put("id", requestId);
            }
            
            String responseJson = objectMapper.writeValueAsString(responseData);
            log.debug("[STDIO-Handler] 发送响应: {}", responseJson);
            
            // 在 JSON 字符串末尾拼接一个换行符再发送到标准输出
            System.out.println(responseJson);
            System.out.flush(); // 确保数据立即输出
        } catch (JsonProcessingException e) {
            log.error("[STDIO-Handler] 序列化响应失败", e);
        }
    }

    /**
     * 提供一个方法给业务逻辑层，用于将数据发送回Electron
     * 
     * @param data 要发送的数据
     */
    public void sendToFrontend(String data) {
        // 直接写入标准输出
        System.out.println(data);
        System.out.flush(); // 确保数据立即输出
    }
}

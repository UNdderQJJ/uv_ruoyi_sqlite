package com.ruoyi.tcp.system;

import com.ruoyi.common.core.TcpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 版本管理TCP处理器
 * 专门处理版本信息相关的TCP请求
 */
@Component
public class VersionManagementHandler {

    private static final Logger log = LoggerFactory.getLogger(VersionManagementHandler.class);

    @Autowired
    private VersionService versionService;

    /**
     * 处理版本管理相关的TCP请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @return TCP响应
     */
    public TcpResponse handleVersionRequest(String path, String body) {
        try {
            switch (path) {
                case "/version/info":
                    return getVersionInfo();
                case "/version/simple":
                    return getSimpleVersion();
                case "/version/service":
                    return getServiceInfo();
                default:
                    log.warn("[VersionManagement] 未知的版本管理路径: {}", path);
                    return TcpResponse.error("未知的版本管理操作: " + path);
            }
        } catch (Exception e) {
            log.error("[VersionManagement] 处理版本管理请求时发生异常: {}", path, e);
            return TcpResponse.error("版本管理操作失败: " + e.getMessage());
        }
    }

    /**
     * 获取完整版本信息
     *
     * @return TCP响应
     */
    private TcpResponse getVersionInfo() {
        try {
            log.info("[VersionManagement] 获取完整版本信息");
            
            Map<String, Object> versionInfo = versionService.getVersionInfo();
            
            return TcpResponse.success(versionInfo);
        } catch (Exception e) {
            log.error("获取版本信息失败: {}", e.getMessage());
            return TcpResponse.error("获取版本信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取简单版本号
     *
     * @return TCP响应
     */
    private TcpResponse getSimpleVersion() {
        try {
            log.info("[VersionManagement] 获取简单版本号");
            
            String version = versionService.getVersion();
            
            var result = new java.util.HashMap<String, Object>();
            result.put("version", version);
            result.put("serviceName", versionService.getServiceName());
            
            return TcpResponse.success(result);
        } catch (Exception e) {
            log.error("获取简单版本号失败: {}", e.getMessage());
            return TcpResponse.error("获取简单版本号失败: " + e.getMessage());
        }
    }

    /**
     * 获取服务信息
     *
     * @return TCP响应
     */
    private TcpResponse getServiceInfo() {
        try {
            log.info("[VersionManagement] 获取服务信息");
            
            var result = new java.util.HashMap<String, Object>();
            result.put("serviceName", versionService.getServiceName());
            result.put("version", versionService.getVersion());
            result.put("buildTime", versionService.getBuildTime());
            result.put("status", "running");
            result.put("uptime", System.currentTimeMillis());
            
            return TcpResponse.success(result);
        } catch (Exception e) {
            log.error("获取服务信息失败: {}", e.getMessage());
            return TcpResponse.error("获取服务信息失败: " + e.getMessage());
        }
    }
}

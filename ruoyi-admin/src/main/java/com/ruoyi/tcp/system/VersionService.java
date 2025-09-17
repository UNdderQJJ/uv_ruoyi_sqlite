package com.ruoyi.tcp.system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 版本信息服务类
 * 提供后端服务版本信息
 */
@Service
public class VersionService {

    @Value("${backend.version:1.0.0}")
    private String version;

    @Value("${backend.buildTime:2025-07-27}")
    private String buildTime;

    @Value("${backend.name:UV-Redio-Server}")
    private String serviceName;

    @Value("${ruoyi.name:UV}")
    private String projectName;

    @Value("${ruoyi.version:3.8.9}")
    private String projectVersion;

    @Value("${ruoyi.copyrightYear:2025}")
    private String copyrightYear;

    /**
     * 获取完整的版本信息
     *
     * @return 版本信息Map
     */
    public Map<String, Object> getVersionInfo() {
        Map<String, Object> versionInfo = new HashMap<>();
        
        // 后端服务信息
        versionInfo.put("serviceName", serviceName);// 服务名称
        versionInfo.put("version", version);// 版本号
        versionInfo.put("buildTime", buildTime);// 构建时间
        
        // 项目信息
        versionInfo.put("projectName", projectName);// 项目名称
        versionInfo.put("projectVersion", projectVersion);// 项目版本
        versionInfo.put("copyrightYear", copyrightYear);// 版权年份
        
        // 系统信息
        versionInfo.put("javaVersion", System.getProperty("java.version"));// Java版本
        versionInfo.put("osName", System.getProperty("os.name"));// 操作系统名称
        versionInfo.put("osVersion", System.getProperty("os.version"));// 操作系统版本
        versionInfo.put("serverTime", System.currentTimeMillis());// 服务器时间
        
        return versionInfo;
    }

    /**
     * 获取简单版本号
     *
     * @return 版本号字符串
     */
    public String getVersion() {
        return version;
    }

    /**
     * 获取服务名称
     *
     * @return 服务名称
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * 获取构建时间
     *
     * @return 构建时间
     */
    public String getBuildTime() {
        return buildTime;
    }
}

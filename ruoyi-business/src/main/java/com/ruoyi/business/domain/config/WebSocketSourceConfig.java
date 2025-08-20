package com.ruoyi.business.domain.config;

/**
 * WebSocket配置
 * 
 * @author ruoyi
 */
public class WebSocketSourceConfig extends SourceConfig {
    
    /** 服务器URL */
    private String serverUrl;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
}

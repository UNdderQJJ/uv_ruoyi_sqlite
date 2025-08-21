package com.ruoyi.business.domain.config;

import lombok.Data;

/**
 * WebSocket配置
 * 
 * @author ruoyi
 */
@Data
public class WebSocketSourceConfig extends SourceConfig {
    
    /** 服务器URL */
    private String serverUrl;

}

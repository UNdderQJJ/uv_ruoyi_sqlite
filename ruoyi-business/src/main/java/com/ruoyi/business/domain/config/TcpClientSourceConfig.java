package com.ruoyi.business.domain.config;

import lombok.Data;

/**
 * TCP客户端配置
 * 
 * @author ruoyi
 */
@Data
public class TcpClientSourceConfig extends SourceConfig {
    
    /** 监听端口 */
    private Integer listenPort;

}

package com.ruoyi.business.domain.config;

import lombok.Data;

/**
 * TCP服务端配置
 * 
 * @author ruoyi
 */
@Data
public class TcpServerSourceConfig extends SourceConfig {
    
    /** 服务器IP地址 */
    private String ipAddress;
    
    /** 服务器端口 */
    private Integer port;

}

package com.ruoyi.business.domain.config;

import lombok.Data;

/**
 * MQTT配置
 * 
 * @author ruoyi
 */
@Data
public class MqttSourceConfig extends SourceConfig {
    
    /** MQTT代理地址 */
    private String brokerAddress;
    
    /** MQTT端口 */
    private Integer port;
    
    /** 用户名 */
    private String username;
    
    /** 密码 */
    private String password;
    
    /** 客户端ID */
    private String clientId;

}

package com.ruoyi.business.domain.config;

/**
 * MQTT配置
 * 
 * @author ruoyi
 */
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

    public String getBrokerAddress() {
        return brokerAddress;
    }

    public void setBrokerAddress(String brokerAddress) {
        this.brokerAddress = brokerAddress;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}

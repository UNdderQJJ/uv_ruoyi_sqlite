package com.ruoyi.business.domain.config;

/**
 * TCP服务端配置
 * 
 * @author ruoyi
 */
public class TcpServerSourceConfig extends SourceConfig {
    
    /** 服务器IP地址 */
    private String ipAddress;
    
    /** 服务器端口 */
    private Integer port;

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}

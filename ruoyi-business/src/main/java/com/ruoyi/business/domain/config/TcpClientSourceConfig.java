package com.ruoyi.business.domain.config;

/**
 * TCP客户端配置
 * 
 * @author ruoyi
 */
public class TcpClientSourceConfig extends SourceConfig {
    
    /** 监听端口 */
    private Integer listenPort;

    public Integer getListenPort() {
        return listenPort;
    }

    public void setListenPort(Integer listenPort) {
        this.listenPort = listenPort;
    }
}

package com.ruoyi.business.domain.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 数据源配置基类
 * 
 * @author ruoyi
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = UDiskSourceConfig.class, name = "U_DISK"),
    @JsonSubTypes.Type(value = TcpServerSourceConfig.class, name = "TCP_SERVER"),
    @JsonSubTypes.Type(value = TcpClientSourceConfig.class, name = "TCP_CLIENT"),
    @JsonSubTypes.Type(value = HttpSourceConfig.class, name = "HTTP"),
    @JsonSubTypes.Type(value = MqttSourceConfig.class, name = "MQTT"),
    @JsonSubTypes.Type(value = WebSocketSourceConfig.class, name = "WEBSOCKET")
})
public abstract class SourceConfig {
    // 基类可以包含通用配置
}

package com.ruoyi.business.service.DataPool;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.business.domain.config.*;
import org.springframework.stereotype.Component;

/**
 * 数据池配置工厂类
 * 用于创建和解析不同类型的数据源配置
 * 
 * @author ruoyi
 */
@Component
public class DataPoolConfigFactory {

    /**
     * 创建U盘导入配置
     */
    public UDiskSourceConfig createUDiskConfig(String filePath, String sheetNameOrIndex, 
                                              Integer startRow, Integer dataColumn, Boolean autoMonitor) {
        UDiskSourceConfig config = new UDiskSourceConfig();
        config.setFilePath(filePath);
        config.setSheetNameOrIndex(sheetNameOrIndex);
        config.setStartRow(startRow);
        config.setDataColumn(dataColumn);
        config.setAutoMonitor(autoMonitor);
        return config;
    }

    /**
     * 创建TCP服务端配置
     */
    public TcpServerSourceConfig createTcpServerConfig(String ipAddress, Integer port) {
        TcpServerSourceConfig config = new TcpServerSourceConfig();
        config.setIpAddress(ipAddress);
        config.setPort(port);
        return config;
    }

    /**
     * 创建TCP客户端配置
     */
    public TcpClientSourceConfig createTcpClientConfig(Integer listenPort) {
        TcpClientSourceConfig config = new TcpClientSourceConfig();
        config.setListenPort(listenPort);
        return config;
    }

    /**
     * 创建HTTP配置
     */
    public HttpSourceConfig createHttpConfig(String url, String method, 
                                           java.util.List<HttpSourceConfig.HttpHeader> headers, String body) {
        HttpSourceConfig config = new HttpSourceConfig();
        config.setUrl(url);
        config.setMethod(method);
        config.setHeaders(headers);
        config.setBody(body);
        return config;
    }

    /**
     * 创建MQTT配置
     */
    public MqttSourceConfig createMqttConfig(String brokerAddress, Integer port, 
                                           String username, String password, String clientId) {
        MqttSourceConfig config = new MqttSourceConfig();
        config.setBrokerAddress(brokerAddress);
        config.setPort(port);
        config.setUsername(username);
        config.setPassword(password);
        config.setClientId(clientId);
        return config;
    }

    /**
     * 创建WebSocket配置
     */
    public WebSocketSourceConfig createWebSocketConfig(String serverUrl) {
        WebSocketSourceConfig config = new WebSocketSourceConfig();
        config.setServerUrl(serverUrl);
        return config;
    }

    /**
     * 创建解析规则配置
     */
    public ParsingRuleConfig createParsingRuleConfig(String ruleType, String expression) {
        ParsingRuleConfig config = new ParsingRuleConfig();
        config.setRuleType(ruleType);
        config.setExpression(expression);
        return config;
    }

    /**
     * 创建触发条件配置
     */
    public TriggerConfig createTriggerConfig(String triggerType, Integer threshold, String requestCommand) {
        TriggerConfig config = new TriggerConfig();
        config.setTriggerType(triggerType);
        config.setThreshold(threshold);
        config.setRequestCommand(requestCommand);
        return config;
    }

    /**
     * 创建MQTT触发条件配置
     */
    public TriggerConfig createMqttTriggerConfig(String triggerType, Integer threshold, 
                                               String subscribeTopic, String publishTopic, String requestPayload) {
        TriggerConfig config = new TriggerConfig();
        config.setTriggerType(triggerType);
        config.setThreshold(threshold);
        config.setSubscribeTopic(subscribeTopic);
        config.setPublishTopic(publishTopic);
        config.setRequestPayload(requestPayload);
        return config;
    }

    /**
     * 根据数据源类型解析配置JSON
     */
    public SourceConfig parseSourceConfig(String sourceType, String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return null;
        }

        switch (sourceType) {
            case "U_DISK":
                return JSON.parseObject(configJson, UDiskSourceConfig.class);
            case "TCP_SERVER":
                return JSON.parseObject(configJson, TcpServerSourceConfig.class);
            case "TCP_CLIENT":
                return JSON.parseObject(configJson, TcpClientSourceConfig.class);
            case "HTTP":
                return JSON.parseObject(configJson, HttpSourceConfig.class);
            case "MQTT":
                return JSON.parseObject(configJson, MqttSourceConfig.class);
            case "WEBSOCKET":
                return JSON.parseObject(configJson, WebSocketSourceConfig.class);
            default:
                throw new IllegalArgumentException("不支持的数据源类型: " + sourceType);
        }
    }

    /**
     * 解析解析规则配置JSON
     */
    public ParsingRuleConfig parseParsingRuleConfig(String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return null;
        }
        return JSON.parseObject(configJson, ParsingRuleConfig.class);
    }

    /**
     * 解析触发条件配置JSON
     */
    public TriggerConfig parseTriggerConfig(String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return null;
        }
        return JSON.parseObject(configJson, TriggerConfig.class);
    }

    /**
     * 验证配置是否有效
     */
    public boolean validateSourceConfig(String sourceType, SourceConfig config) {
        if (config == null) {
            return false;
        }

        switch (sourceType) {
            case "U_DISK":
                UDiskSourceConfig uDiskConfig = (UDiskSourceConfig) config;
                return uDiskConfig.getFilePath() != null && !uDiskConfig.getFilePath().trim().isEmpty();
            case "TCP_SERVER":
                TcpServerSourceConfig tcpServerConfig = (TcpServerSourceConfig) config;
                return tcpServerConfig.getIpAddress() != null && tcpServerConfig.getPort() != null;
            case "TCP_CLIENT":
                TcpClientSourceConfig tcpClientConfig = (TcpClientSourceConfig) config;
                return tcpClientConfig.getListenPort() != null;
            case "HTTP":
                HttpSourceConfig httpConfig = (HttpSourceConfig) config;
                return httpConfig.getUrl() != null && !httpConfig.getUrl().trim().isEmpty();
            case "MQTT":
                MqttSourceConfig mqttConfig = (MqttSourceConfig) config;
                return mqttConfig.getBrokerAddress() != null && mqttConfig.getPort() != null;
            case "WEBSOCKET":
                WebSocketSourceConfig wsConfig = (WebSocketSourceConfig) config;
                return wsConfig.getServerUrl() != null && !wsConfig.getServerUrl().trim().isEmpty();
            default:
                return false;
        }
    }
}

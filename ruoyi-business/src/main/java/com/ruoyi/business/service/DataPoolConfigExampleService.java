package com.ruoyi.business.service;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.business.domain.config.*;
import com.ruoyi.business.enums.RuleType;
import com.ruoyi.business.enums.TriggerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据池配置示例服务
 * 提供各种配置的示例JSON
 * 
 * @author ruoyi
 */
@Service
public class DataPoolConfigExampleService {

    @Autowired
    private DataPoolConfigFactory configFactory;

    /**
     * 获取U盘导入配置示例
     */
    public String getUDiskConfigExample() {
        UDiskSourceConfig config = configFactory.createUDiskConfig(
            "D:/data/products.xlsx",
            "Sheet1",
            2,
            1,
            true
        );
        return JSON.toJSONString(config, String.valueOf(true));
    }

    /**
     * 获取TCP服务端配置示例
     */
    public String getTcpServerConfigExample() {
        TcpServerSourceConfig config = configFactory.createTcpServerConfig(
            "192.168.1.10",
            8899
        );
        return JSON.toJSONString(config, String.valueOf(true));
    }

    /**
     * 获取TCP客户端配置示例
     */
    public String getTcpClientConfigExample() {
        TcpClientSourceConfig config = configFactory.createTcpClientConfig(8899);
        return JSON.toJSONString(config, String.valueOf(true));
    }

    /**
     * 获取HTTP配置示例
     */
    public String getHttpConfigExample() {
        HttpSourceConfig.HttpHeader header1 = new HttpSourceConfig.HttpHeader();
        header1.setKey("Content-Type");
        header1.setValue("application/json");

        HttpSourceConfig.HttpHeader header2 = new HttpSourceConfig.HttpHeader();
        header2.setKey("Authorization");
        header2.setValue("Bearer your_token");

        HttpSourceConfig config = configFactory.createHttpConfig(
            "https://api.example.com/data",
            "POST",
            Arrays.asList(header1, header2),
            "{\"page\": 1, \"size\": 100}"
        );
        return JSON.toJSONString(config, String.valueOf(true));
    }

    /**
     * 获取MQTT配置示例
     */
    public String getMqttConfigExample() {
        MqttSourceConfig config = configFactory.createMqttConfig(
            "mqtt.eclipse.org",
            1883,
            "user",
            "password",
            "uv_printer_system_1"
        );
        return JSON.toJSONString(config, String.valueOf(true));
    }

    /**
     * 获取WebSocket配置示例
     */
    public String getWebSocketConfigExample() {
        WebSocketSourceConfig config = configFactory.createWebSocketConfig(
            "ws://192.168.1.12:9000/ws"
        );
        return JSON.toJSONString(config, String.valueOf(true));
    }

    /**
     * 获取解析规则配置示例
     */
    public String getParsingRuleConfigExample() {
        ParsingRuleConfig config = configFactory.createParsingRuleConfig(
            RuleType.JSON_PATH.getCode(),
            "$.data.code"
        );
        return JSON.toJSONString(config, String.valueOf(true));
    }

    /**
     * 获取触发条件配置示例
     */
    public String getTriggerConfigExample() {
        TriggerConfig config = configFactory.createTriggerConfig(
            TriggerType.BELOW_THRESHOLD.getCode(),
            100,
            "GET_DATA_NOW"
        );
        return JSON.toJSONString(config, String.valueOf(true));
    }

    /**
     * 获取MQTT触发条件配置示例
     */
    public String getMqttTriggerConfigExample() {
        TriggerConfig config = configFactory.createMqttTriggerConfig(
            TriggerType.BELOW_THRESHOLD.getCode(),
            100,
            "printer/data/response",
            "printer/data/request",
            "GET_DATA_NOW"
        );
        return JSON.toJSONString(config, String.valueOf(true));
    }

    /**
     * 获取完整的数据池配置示例
     */
    public Map<String, Object> getCompleteDataPoolExamples() {
        Map<String, Object> examples = new HashMap<>();

        // U盘导入示例
        Map<String, Object> uDiskExample = new HashMap<>();
        uDiskExample.put("poolName", "U盘数据池");
        uDiskExample.put("sourceType", "U_DISK");
        uDiskExample.put("sourceConfigJson", getUDiskConfigExample());
        uDiskExample.put("parsingRuleJson", null);
        uDiskExample.put("triggerConfigJson", null);
        examples.put("U_DISK", uDiskExample);

        // TCP服务端示例
        Map<String, Object> tcpServerExample = new HashMap<>();
        tcpServerExample.put("poolName", "TCP服务端数据池");
        tcpServerExample.put("sourceType", "TCP_SERVER");
        tcpServerExample.put("sourceConfigJson", getTcpServerConfigExample());
        tcpServerExample.put("parsingRuleJson", getParsingRuleConfigExample());
        tcpServerExample.put("triggerConfigJson", getTriggerConfigExample());
        examples.put("TCP_SERVER", tcpServerExample);

        // TCP客户端示例
        Map<String, Object> tcpClientExample = new HashMap<>();
        tcpClientExample.put("poolName", "TCP客户端数据池");
        tcpClientExample.put("sourceType", "TCP_CLIENT");
        tcpClientExample.put("sourceConfigJson", getTcpClientConfigExample());
        tcpClientExample.put("parsingRuleJson", getParsingRuleConfigExample());
        tcpClientExample.put("triggerConfigJson", getTriggerConfigExample());
        examples.put("TCP_CLIENT", tcpClientExample);

        // HTTP示例
        Map<String, Object> httpExample = new HashMap<>();
        httpExample.put("poolName", "HTTP数据池");
        httpExample.put("sourceType", "HTTP");
        httpExample.put("sourceConfigJson", getHttpConfigExample());
        httpExample.put("parsingRuleJson", getParsingRuleConfigExample());
        httpExample.put("triggerConfigJson", getTriggerConfigExample());
        examples.put("HTTP", httpExample);

        // MQTT示例
        Map<String, Object> mqttExample = new HashMap<>();
        mqttExample.put("poolName", "MQTT数据池");
        mqttExample.put("sourceType", "MQTT");
        mqttExample.put("sourceConfigJson", getMqttConfigExample());
        mqttExample.put("parsingRuleJson", getParsingRuleConfigExample());
        mqttExample.put("triggerConfigJson", getMqttTriggerConfigExample());
        examples.put("MQTT", mqttExample);

        // WebSocket示例
        Map<String, Object> webSocketExample = new HashMap<>();
        webSocketExample.put("poolName", "WebSocket数据池");
        webSocketExample.put("sourceType", "WEBSOCKET");
        webSocketExample.put("sourceConfigJson", getWebSocketConfigExample());
        webSocketExample.put("parsingRuleJson", getParsingRuleConfigExample());
        webSocketExample.put("triggerConfigJson", getTriggerConfigExample());
        examples.put("WEBSOCKET", webSocketExample);

        return examples;
    }

    /**
     * 获取解析规则类型示例
     */
    public Map<String, Object> getParsingRuleExamples() {
        Map<String, Object> examples = new HashMap<>();

        // JSON路径解析
        ParsingRuleConfig jsonPathConfig = configFactory.createParsingRuleConfig(
            RuleType.JSON_PATH.getCode(),
            "$.data.code"
        );
        examples.put("JSON_PATH", JSON.toJSONString(jsonPathConfig, String.valueOf(true)));

        // 子字符串解析
        ParsingRuleConfig substringConfig = configFactory.createParsingRuleConfig(
            RuleType.SUBSTRING.getCode(),
            "start:10,end:20"
        );
        examples.put("SUBSTRING", JSON.toJSONString(substringConfig, String.valueOf(true)));

        // 分隔符解析
        ParsingRuleConfig delimiterConfig = configFactory.createParsingRuleConfig(
            RuleType.DELIMITER.getCode(),
            "|,2"
        );
        examples.put("DELIMITER", JSON.toJSONString(delimiterConfig, String.valueOf(true)));

        // JavaScript表达式解析
        ParsingRuleConfig jsConfig = configFactory.createParsingRuleConfig(
            RuleType.JS_EXPRESSION.getCode(),
            "data.split(',').map(item => item.trim())"
        );
        examples.put("JS_EXPRESSION", JSON.toJSONString(jsConfig, String.valueOf(true)));

        return examples;
    }

    /**
     * 获取触发类型示例
     */
    public Map<String, Object> getTriggerTypeExamples() {
        Map<String, Object> examples = new HashMap<>();

        // 低于阈值触发
        TriggerConfig belowThresholdConfig = configFactory.createTriggerConfig(
            TriggerType.BELOW_THRESHOLD.getCode(),
            100,
            "GET_DATA_NOW"
        );
        examples.put("BELOW_THRESHOLD", JSON.toJSONString(belowThresholdConfig, String.valueOf(true)));

        // 定时触发
        TriggerConfig scheduledConfig = configFactory.createTriggerConfig(
            TriggerType.SCHEDULED.getCode(),
            null,
            "GET_DATA_NOW"
        );
        examples.put("SCHEDULED", JSON.toJSONString(scheduledConfig, String.valueOf(true)));

        return examples;
    }
}

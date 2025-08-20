package com.ruoyi.business.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据池配置工具类
 * 提供配置示例和验证方法
 * 
 * @author ruoyi
 */
public class DataPoolConfigUtils {

    /**
     * 获取U盘导入配置示例
     */
    public static String getUDiskConfigExample() {
        Map<String, Object> config = new HashMap<>();
        config.put("filePath", "D:/data/products.xlsx");
        config.put("sheetNameOrIndex", "Sheet1");
        config.put("startRow", 2);
        config.put("dataColumn", 1);
        config.put("autoMonitor", true);
        return JSON.toJSONString(config, String.valueOf(true));
    }

    /**
     * 获取TCP服务端配置示例
     */
    public static String getTcpServerConfigExample() {
        Map<String, Object> config = new HashMap<>();
        config.put("ipAddress", "192.168.1.10");
        config.put("port", 8899);
        return JSON.toJSONString(config, String.valueOf(true));
    }

    /**
     * 获取TCP客户端配置示例
     */
    public static String getTcpClientConfigExample() {
        Map<String, Object> config = new HashMap<>();
        config.put("listenPort", 8899);
        return JSON.toJSONString(config, String.valueOf(true));
    }

    /**
     * 获取HTTP配置示例
     */
    public static String getHttpConfigExample() {
        Map<String, Object> config = new HashMap<>();
        config.put("url", "https://api.example.com/data");
        config.put("method", "POST");
        
        Map<String, String> header1 = new HashMap<>();
        header1.put("key", "Content-Type");
        header1.put("value", "application/json");
        
        Map<String, String> header2 = new HashMap<>();
        header2.put("key", "Authorization");
        header2.put("value", "Bearer your_token");
        
        config.put("headers", new Object[]{header1, header2});
        config.put("body", "{\"page\": 1, \"size\": 100}");
        
        return JSON.toJSONString(config, String.valueOf(true));
    }

    /**
     * 获取MQTT配置示例
     */
    public static String getMqttConfigExample() {
        Map<String, Object> config = new HashMap<>();
        config.put("brokerAddress", "mqtt.eclipse.org");
        config.put("port", 1883);
        config.put("username", "user");
        config.put("password", "password");
        config.put("clientId", "uv_printer_system_1");
        return JSON.toJSONString(config, String.valueOf(true));
    }

    /**
     * 获取WebSocket配置示例
     */
    public static String getWebSocketConfigExample() {
        Map<String, Object> config = new HashMap<>();
        config.put("serverUrl", "ws://192.168.1.12:9000/ws");
        return JSON.toJSONString(config, String.valueOf(true));
    }

    /**
     * 获取解析规则配置示例
     */
    public static String getParsingRuleConfigExample() {
        Map<String, Object> config = new HashMap<>();
        config.put("ruleType", "JSON_PATH");
        config.put("expression", "$.data.code");
        return JSON.toJSONString(config, String.valueOf(true));
    }

    /**
     * 获取触发条件配置示例
     */
    public static String getTriggerConfigExample() {
        Map<String, Object> config = new HashMap<>();
        config.put("triggerType", "BELOW_THRESHOLD");
        config.put("threshold", 100);
        config.put("requestCommand", "GET_DATA_NOW");
        return JSON.toJSONString(config, String.valueOf(true));
    }

    /**
     * 获取MQTT触发条件配置示例
     */
    public static String getMqttTriggerConfigExample() {
        Map<String, Object> config = new HashMap<>();
        config.put("triggerType", "BELOW_THRESHOLD");
        config.put("threshold", 100);
        config.put("subscribeTopic", "printer/data/response");
        config.put("publishTopic", "printer/data/request");
        config.put("requestPayload", "GET_DATA_NOW");
        return JSON.toJSONString(config, String.valueOf(true));
    }

    /**
     * 验证数据源配置JSON格式
     */
    public static boolean validateSourceConfigJson(String sourceType, String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return false;
        }

        try {
            JSONObject config = JSON.parseObject(configJson);
            
            switch (sourceType) {
                case "U_DISK":
                    return validateUDiskConfig(config);
                case "TCP_SERVER":
                    return validateTcpServerConfig(config);
                case "TCP_CLIENT":
                    return validateTcpClientConfig(config);
                case "HTTP":
                    return validateHttpConfig(config);
                case "MQTT":
                    return validateMqttConfig(config);
                case "WEBSOCKET":
                    return validateWebSocketConfig(config);
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证解析规则配置JSON格式
     */
    public static boolean validateParsingRuleConfigJson(String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return false;
        }

        try {
            JSONObject config = JSON.parseObject(configJson);
            String ruleType = config.getString("ruleType");
            String expression = config.getString("expression");
            
            return ruleType != null && !ruleType.trim().isEmpty() &&
                   expression != null && !expression.trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证触发条件配置JSON格式
     */
    public static boolean validateTriggerConfigJson(String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return false;
        }

        try {
            JSONObject config = JSON.parseObject(configJson);
            String triggerType = config.getString("triggerType");
            
            if (triggerType == null || triggerType.trim().isEmpty()) {
                return false;
            }

            // 验证阈值触发类型的必要字段
            if ("BELOW_THRESHOLD".equals(triggerType)) {
                Integer threshold = config.getInteger("threshold");
                return threshold != null && threshold > 0;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean validateUDiskConfig(JSONObject config) {
        String filePath = config.getString("filePath");
        String sheetNameOrIndex = config.getString("sheetNameOrIndex");
        Integer startRow = config.getInteger("startRow");
        Integer dataColumn = config.getInteger("dataColumn");
        
        return filePath != null && !filePath.trim().isEmpty() &&
               sheetNameOrIndex != null && !sheetNameOrIndex.trim().isEmpty() &&
               startRow != null && startRow > 0 &&
               dataColumn != null && dataColumn > 0;
    }

    private static boolean validateTcpServerConfig(JSONObject config) {
        String ipAddress = config.getString("ipAddress");
        Integer port = config.getInteger("port");
        
        return ipAddress != null && !ipAddress.trim().isEmpty() &&
               port != null && port > 0 && port <= 65535;
    }

    private static boolean validateTcpClientConfig(JSONObject config) {
        Integer listenPort = config.getInteger("listenPort");
        return listenPort != null && listenPort > 0 && listenPort <= 65535;
    }

    private static boolean validateHttpConfig(JSONObject config) {
        String url = config.getString("url");
        String method = config.getString("method");
        
        return url != null && !url.trim().isEmpty() &&
               method != null && !method.trim().isEmpty();
    }

    private static boolean validateMqttConfig(JSONObject config) {
        String brokerAddress = config.getString("brokerAddress");
        Integer port = config.getInteger("port");
        String clientId = config.getString("clientId");
        
        return brokerAddress != null && !brokerAddress.trim().isEmpty() &&
               port != null && port > 0 && port <= 65535 &&
               clientId != null && !clientId.trim().isEmpty();
    }

    private static boolean validateWebSocketConfig(JSONObject config) {
        String serverUrl = config.getString("serverUrl");
        return serverUrl != null && !serverUrl.trim().isEmpty();
    }

    /**
     * 获取所有配置示例
     */
    public static Map<String, Object> getAllConfigExamples() {
        Map<String, Object> examples = new HashMap<>();
        
        examples.put("U_DISK_SOURCE", getUDiskConfigExample());
        examples.put("TCP_SERVER_SOURCE", getTcpServerConfigExample());
        examples.put("TCP_CLIENT_SOURCE", getTcpClientConfigExample());
        examples.put("HTTP_SOURCE", getHttpConfigExample());
        examples.put("MQTT_SOURCE", getMqttConfigExample());
        examples.put("WEBSOCKET_SOURCE", getWebSocketConfigExample());
        examples.put("PARSING_RULE", getParsingRuleConfigExample());
        examples.put("TRIGGER", getTriggerConfigExample());
        examples.put("MQTT_TRIGGER", getMqttTriggerConfigExample());
        
        return examples;
    }
}

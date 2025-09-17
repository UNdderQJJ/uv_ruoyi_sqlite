package com.ruoyi.business.service.DataPool;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.business.domain.config.*;
import com.ruoyi.business.enums.RuleType;
import com.ruoyi.business.enums.SourceType;
import com.ruoyi.business.enums.TriggerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据池配置验证服务
 * 用于验证各种配置的有效性
 * 
 * @author ruoyi
 */
@Service
public class DataPoolConfigValidationService {

    @Autowired
    private DataPoolConfigFactory configFactory;

    /**
     * 验证数据池配置
     */
    public ValidationResult validateDataPoolConfig(String sourceType, String sourceConfigJson,
                                                 String parsingRuleJson, String triggerConfigJson) {
        return validateDataPoolConfig(sourceType, sourceConfigJson, parsingRuleJson, triggerConfigJson, null);
    }

    /**
     * 验证数据池配置（包含时间间隔）
     */
    public ValidationResult validateDataPoolConfig(String sourceType, String sourceConfigJson,
                                                 String parsingRuleJson, String triggerConfigJson, Long dataFetchInterval) {
        ValidationResult result = new ValidationResult();
        
        try {
            // 验证数据源类型
            if (!isValidSourceType(sourceType)) {
                result.addError("不支持的数据源类型: " + sourceType);
                return result;
            }

            // 验证数据源配置
            if (sourceConfigJson != null && !sourceConfigJson.trim().isEmpty()) {
                SourceConfig sourceConfig = configFactory.parseSourceConfig(sourceType, sourceConfigJson);
                if (!configFactory.validateSourceConfig(sourceType, sourceConfig)) {
                    result.addError("数据源配置无效");
                }
            } else {
                result.addError("数据源配置不能为空");
            }

            // 验证解析规则配置
            if (parsingRuleJson != null && !parsingRuleJson.trim().isEmpty()) {
                if (!validateParsingRuleConfig(parsingRuleJson)) {
                    result.addError("解析规则配置无效");
                }
            } else if (needsParsingRule(sourceType)) {
                result.addError("该数据源类型需要解析规则配置");
            }

            // 验证触发条件配置
            if (triggerConfigJson != null && !triggerConfigJson.trim().isEmpty()) {
                if (!validateTriggerConfig(triggerConfigJson)) {
                    result.addError("触发条件配置无效");
                }
            } else if (needsTriggerConfig(sourceType)) {
                result.addError("该数据源类型需要触发条件配置");
            }

            // 验证时间间隔配置
            if (dataFetchInterval != null) {
                if (dataFetchInterval < 100) {
                    result.addError("数据获取间隔时间不能少于100毫秒（0.1秒）");
                } else if (dataFetchInterval > 3600000) {
                    result.addError("数据获取间隔时间不能超过3600000毫秒（1小时）");
                }
            }

        } catch (Exception e) {
            result.addError("配置验证失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 验证数据源类型是否有效
     */
    private boolean isValidSourceType(String sourceType) {
        if (sourceType == null || sourceType.trim().isEmpty()) {
            return false;
        }
        
        for (SourceType type : SourceType.values()) {
            if (type.getCode().equals(sourceType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证解析规则配置
     */
    private boolean validateParsingRuleConfig(String configJson) {
        try {
            ParsingRuleConfig config = JSON.parseObject(configJson, ParsingRuleConfig.class);
            
            if (config.getRuleType() == null || config.getRuleType().trim().isEmpty()) {
                return false;
            }

            // 验证规则类型
            boolean validRuleType = false;
            for (RuleType type : RuleType.values()) {
                if (type.getCode().equals(config.getRuleType())) {
                    validRuleType = true;
                    break;
                }
            }
            
            if (!validRuleType) {
                return false;
            }

            // 验证表达式
            if (config.getExpression() == null || config.getExpression().trim().isEmpty()) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证触发条件配置
     */
    private boolean validateTriggerConfig(String configJson) {
        try {
            TriggerConfig config = JSON.parseObject(configJson, TriggerConfig.class);
            
            if (config.getTriggerType() == null || config.getTriggerType().trim().isEmpty()) {
                return false;
            }

            // 验证触发类型
            boolean validTriggerType = false;
            for (TriggerType type : TriggerType.values()) {
                if (type.getCode().equals(config.getTriggerType())) {
                    validTriggerType = true;
                    break;
                }
            }
            
            if (!validTriggerType) {
                return false;
            }

            // 验证阈值触发类型的必要字段
            if (TriggerType.THRESHOLD.getCode().equals(config.getTriggerType())) {
                if (config.getThreshold() == null || config.getThreshold() <= 0) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否需要解析规则配置
     */
    private boolean needsParsingRule(String sourceType) {
        // U盘导入不需要解析规则
        return !SourceType.U_DISK.getCode().equals(sourceType);
    }

    /**
     * 判断是否需要触发条件配置
     */
    private boolean needsTriggerConfig(String sourceType) {
        // U盘导入不需要触发条件
        return !SourceType.U_DISK.getCode().equals(sourceType);
    }

    /**
     * 验证特定数据源类型的配置
     */
    public ValidationResult validateSpecificSourceConfig(String sourceType, String configJson) {
        ValidationResult result = new ValidationResult();
        
        try {
            SourceConfig config = configFactory.parseSourceConfig(sourceType, configJson);
            
            switch (sourceType) {
                case "U_DISK":
                    validateUDiskConfig((UDiskSourceConfig) config, result);
                    break;
                case "TCP_SERVER":
                    validateTcpServerConfig((TcpServerSourceConfig) config, result);
                    break;
                case "TCP_CLIENT":
                    validateTcpClientConfig((TcpClientSourceConfig) config, result);
                    break;
                case "HTTP":
                    validateHttpConfig((HttpSourceConfig) config, result);
                    break;
                case "MQTT":
                    validateMqttConfig((MqttSourceConfig) config, result);
                    break;
                case "WEBSOCKET":
                    validateWebSocketConfig((WebSocketSourceConfig) config, result);
                    break;
            }
        } catch (Exception e) {
            result.addError("配置验证失败: " + e.getMessage());
        }
        
        return result;
    }

    private void validateUDiskConfig(UDiskSourceConfig config, ValidationResult result) {
        if (config.getFilePath() == null || config.getFilePath().trim().isEmpty()) {
            result.addError("文件路径不能为空");
        }
        if (config.getSheetNameOrIndex() == null || config.getSheetNameOrIndex().trim().isEmpty()) {
            result.addError("工作表名称或索引不能为空");
        }
        if (config.getStartRow() == null || config.getStartRow() < 1) {
            result.addError("开始行号必须大于0");
        }
        if (config.getDataColumn() == null || config.getDataColumn() < 1) {
            result.addError("数据列号必须大于0");
        }
    }

    private void validateTcpServerConfig(TcpServerSourceConfig config, ValidationResult result) {
        if (config.getIpAddress() == null || config.getIpAddress().trim().isEmpty()) {
            result.addError("IP地址不能为空");
        }
        if (config.getPort() == null || config.getPort() < 1 || config.getPort() > 65535) {
            result.addError("端口号必须在1-65535之间");
        }
    }

    private void validateTcpClientConfig(TcpClientSourceConfig config, ValidationResult result) {
        if (config.getListenPort() == null || config.getListenPort() < 1 || config.getListenPort() > 65535) {
            result.addError("监听端口必须在1-65535之间");
        }
    }

    private void validateHttpConfig(HttpSourceConfig config, ValidationResult result) {
        if (config.getUrl() == null || config.getUrl().trim().isEmpty()) {
            result.addError("URL不能为空");
        }
        if (config.getMethod() == null || config.getMethod().trim().isEmpty()) {
            result.addError("请求方法不能为空");
        }
    }

    private void validateMqttConfig(MqttSourceConfig config, ValidationResult result) {
        if (config.getBrokerAddress() == null || config.getBrokerAddress().trim().isEmpty()) {
            result.addError("MQTT代理地址不能为空");
        }
        if (config.getPort() == null || config.getPort() < 1 || config.getPort() > 65535) {
            result.addError("端口号必须在1-65535之间");
        }
        if (config.getClientId() == null || config.getClientId().trim().isEmpty()) {
            result.addError("客户端ID不能为空");
        }
    }

    private void validateWebSocketConfig(WebSocketSourceConfig config, ValidationResult result) {
        if (config.getServerUrl() == null || config.getServerUrl().trim().isEmpty()) {
            result.addError("服务器URL不能为空");
        }
    }

    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private boolean valid = true;
        private List<String> errors = new java.util.ArrayList<>();

        public boolean isValid() {
            return valid && errors.isEmpty();
        }

        public void addError(String error) {
            this.valid = false;
            this.errors.add(error);
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}

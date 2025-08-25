package com.ruoyi.business.service.common;

import com.jayway.jsonpath.JsonPath;
import com.ruoyi.business.domain.config.ParsingRuleConfig;
import com.ruoyi.business.enums.RuleType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用解析规则引擎服务
 * 支持多种数据解析规则，供所有数据源类型使用
 */
@Service
public class ParsingRuleEngineService {

    private static final Logger log = LoggerFactory.getLogger(ParsingRuleEngineService.class);

    /**
     * 根据解析规则，从原始响应中提取数据项列表
     */
    public List<String> extractItems(String raw, ParsingRuleConfig ruleConfig) {
        List<String> result = new ArrayList<>();
        if (StringUtils.isBlank(raw) || ruleConfig == null || StringUtils.isBlank(ruleConfig.getRuleType())) {
            return result;
        }
        try {
            String type = ruleConfig.getRuleType();
            String expr = ruleConfig.getExpression();
            if (RuleType.JSON_PATH.getCode().equals(type)) {
                Object value = JsonPath.read(raw, expr);
                if (value instanceof List<?> list) {
                    for (Object item : list) {
                        result.add(String.valueOf(item));
                    }
                } else if (value != null) {
                    result.add(String.valueOf(value));
                }
            } else if (RuleType.DELIMITER.getCode().equals(type)) {
                String delimiter = expr == null || expr.isEmpty() ? "\n" : expr;
                for (String part : raw.split(delimiter)) {
                    if (StringUtils.isNotBlank(part)) {
                        result.add(part.trim());
                    }
                }
            } else if (RuleType.SUBSTRING.getCode().equals(type)) {
                // 表达式: start,end
                String[] se = (expr == null ? "" : expr).split(",");
                int start = se.length > 0 ? Integer.parseInt(se[0].trim()) : 0;
                int end = se.length > 1 ? Integer.parseInt(se[1].trim()) : raw.length();
                if (start >= 0 && end <= raw.length() && start < end) {
                    result.add(raw.substring(start, end));
                }
            } else {
                // 兜底：整条作为一个数据项
                result.add(raw);
            }
        } catch (Exception e) {
            log.error("解析规则执行失败: {}", e.getMessage(), e);
        }
        return result;
    }
    
    /**
     * 根据解析规则字符串，从原始响应中提取数据项列表
     * 用于兼容旧的配置方式
     */
    public List<String> extractItems(String raw, String parsingRule) {
        if (StringUtils.isBlank(raw) || StringUtils.isBlank(parsingRule)) {
            return new ArrayList<>();
        }
        
        try {
            // 这里可以添加 JSON 解析逻辑来解析 parsingRule
            // 暂时使用默认规则：整条作为一个数据项
            List<String> result = new ArrayList<>();
            result.add(raw);
            return result;
        } catch (Exception e) {
            log.error("解析规则执行失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}

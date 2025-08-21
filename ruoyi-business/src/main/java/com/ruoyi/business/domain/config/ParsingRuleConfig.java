package com.ruoyi.business.domain.config;

import lombok.Data;

/**
 * 解析规则配置
 * 
 * @author ruoyi
 */
@Data
public class ParsingRuleConfig {
    
    /** 规则类型 */
    private String ruleType;
    
    /** 解析表达式 */
    private String expression;

}

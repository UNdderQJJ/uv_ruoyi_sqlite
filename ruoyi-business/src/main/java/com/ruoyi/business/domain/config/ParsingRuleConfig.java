package com.ruoyi.business.domain.config;

/**
 * 解析规则配置
 * 
 * @author ruoyi
 */
public class ParsingRuleConfig {
    
    /** 规则类型 */
    private String ruleType;
    
    /** 解析表达式 */
    private String expression;

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }
}

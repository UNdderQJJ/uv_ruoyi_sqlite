package com.ruoyi.business.enums;

/**
 * 解析规则类型枚举
 * 
 * @author ruoyi
 */
public enum RuleType
{
    /** JSON路径解析 */
    JSON_PATH("JSON_PATH", "JSON路径解析"),
    
    /** 子字符串解析 */
    SUBSTRING("SUBSTRING", "子字符串解析"),
    
    /** 分隔符解析 */
    DELIMITER("DELIMITER", "分隔符解析"),
    
    /** JavaScript表达式解析 */
    JS_EXPRESSION("JS_EXPRESSION", "JavaScript表达式解析");

    private final String code;
    private final String info;

    RuleType(String code, String info)
    {
        this.code = code;
        this.info = info;
    }

    public String getCode()
    {
        return code;
    }

    public String getInfo()
    {
        return info;
    }
}

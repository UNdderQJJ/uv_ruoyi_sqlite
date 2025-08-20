package com.ruoyi.business.enums;

/**
 * 触发类型枚举
 * 
 * @author ruoyi
 */
public enum TriggerType
{
    /** 低于阈值触发 */
    BELOW_THRESHOLD("BELOW_THRESHOLD", "低于阈值触发"),
    
    /** 定时触发 */
    SCHEDULED("SCHEDULED", "定时触发");

    private final String code;
    private final String info;

    TriggerType(String code, String info)
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

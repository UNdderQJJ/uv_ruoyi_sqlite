package com.ruoyi.business.enums;

/**
 * 数据池状态枚举
 * 
 * @author ruoyi
 */
public enum PoolStatus
{
    /** 闲置 */
    IDLE("IDLE", "闲置"),
    
    /** 运行 */
    RUNNING("RUNNING", "运行"),
    
    /** 警告 */
    WARNING("WARNING", "警告"),
    
    /** 错误 */
    ERROR("ERROR", "错误"),

    /**已完成*/
    WINING("WINING", "已完成");

    private final String code;
    private final String info;

    PoolStatus(String code, String info)
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

    /**
     * 根据状态码获取枚举值
     */
    public static PoolStatus fromCode(String code) {
        for (PoolStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}

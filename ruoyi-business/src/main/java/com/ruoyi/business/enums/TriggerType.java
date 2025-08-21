package com.ruoyi.business.enums;

/**
 * 触发类型枚举
 * 
 * @author ruoyi
 */
public enum TriggerType {
    
    /** 阈值触发 */
    THRESHOLD("THRESHOLD", "阈值触发"),
    
    /** 定时触发 */
    INTERVAL("INTERVAL", "定时触发"),
    
    /** 手动触发 */
    MANUAL("MANUAL", "手动触发");
    
    private final String code;
    private final String info;
    
    TriggerType(String code, String info) {
        this.code = code;
        this.info = info;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getInfo() {
        return info;
    }
    
    /**
     * 根据代码获取枚举
     */
    public static TriggerType getByCode(String code) {
        for (TriggerType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * 判断是否为有效类型
     */
    public static boolean isValidType(String code) {
        return getByCode(code) != null;
    }
}
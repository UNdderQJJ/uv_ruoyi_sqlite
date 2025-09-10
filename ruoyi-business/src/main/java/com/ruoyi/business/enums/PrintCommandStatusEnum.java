package com.ruoyi.business.enums;

/**
 * 打印指令状态枚举
 */
public enum PrintCommandStatusEnum {
    
    /** 待发送 */
    PENDING("PENDING", "待发送"),
    
    /** 已发送 */
    SENT("SENT", "已发送"),
    
    /** 已完成 */
    COMPLETED("COMPLETED", "已完成"),
    
    /** 失败 */
    FAILED("FAILED", "失败"),
    
    /** 重试中 */
    RETRYING("RETRYING", "重试中");
    
    private final String code;
    private final String info;
    
    PrintCommandStatusEnum(String code, String info) {
        this.code = code;
        this.info = info;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getInfo() {
        return info;
    }
    
    public static PrintCommandStatusEnum fromCode(String code) {
        for (PrintCommandStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的打印指令状态: " + code);
    }
}

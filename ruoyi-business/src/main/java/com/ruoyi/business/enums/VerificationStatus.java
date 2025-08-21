package com.ruoyi.business.enums;

/**
 * 校验状态枚举
 * 用于归档数据表中的校验结果状态
 * 
 * @author ruoyi
 */
public enum VerificationStatus {
    
    /**
     * 校验成功
     */
    SUCCESS("SUCCESS", "校验成功"),
    
    /**
     * 校验失败
     */
    FAIL("FAIL", "校验失败"),
    
    /**
     * 校验中
     */
    PROCESSING("PROCESSING", "校验中"),
    
    /**
     * 校验超时
     */
    TIMEOUT("TIMEOUT", "校验超时"),
    
    /**
     * 无需校验
     */
    NOT_REQUIRED("NOT_REQUIRED", "无需校验");
    
    private final String code;
    private final String info;
    
    VerificationStatus(String code, String info) {
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
     * 根据代码获取枚举值
     */
    public static VerificationStatus getByCode(String code) {
        for (VerificationStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}

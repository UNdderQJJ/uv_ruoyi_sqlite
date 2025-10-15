package com.ruoyi.business.enums;

/**
 * 设备类型枚举
 * 
 * @author ruoyi
 */
public enum DeviceType {
    
    /** 打印机 */
    PRINTER("PRINTER", "打印机"),
    
    /** 喷码机 */
    CODER("CODER", "喷码机"),
    
    /** 读码器 */
    SCANNER("SCANNER", "读码器");

    private final String code;
    private final String info;

    DeviceType(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

    public static DeviceType fromCode(String code) {
        for (DeviceType type : DeviceType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的设备类型: " + code);
    }
}

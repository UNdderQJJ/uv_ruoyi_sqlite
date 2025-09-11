package com.ruoyi.business.enums;

/**
 * 系统日志类型
 * 对应字段: system_log.log_type
 */
public enum SystemLogType {

    /** 打印 */
    PRINT("PRINT", "打印"),

    /** 扫码 */
    SCAN("SCAN", "扫码"),

    /** 通讯 */
    COMMUNICATION("common", "通讯");

    private final String code;
    private final String info;

    SystemLogType(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

    public static SystemLogType fromCode(String code) {
        for (SystemLogType type : SystemLogType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的系统日志类型: " + code);
    }
}



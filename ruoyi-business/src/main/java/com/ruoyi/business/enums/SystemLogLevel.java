package com.ruoyi.business.enums;

/**
 * 系统日志级别
 * 对应字段: system_log.log_level
 */
public enum SystemLogLevel {

    INFO("INFO", "信息"),
    ERROR("ERROR", "错误"),
    DEBUG("DEBUG", "调试"),
    WARN("WARN", "告警");

    private final String code;
    private final String info;

    SystemLogLevel(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

    public static SystemLogLevel fromCode(String code) {
        for (SystemLogLevel level : SystemLogLevel.values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("未知的系统日志级别: " + code);
    }
}



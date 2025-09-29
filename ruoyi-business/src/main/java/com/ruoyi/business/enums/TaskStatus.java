package com.ruoyi.business.enums;

/**
 * 任务状态枚举
 * 与设备状态枚举写法保持一致，提供 code 与 info 字段，以及 fromCode 工具方法
 */
public enum TaskStatus {

    /** 待开始 */
    PENDING("PENDING", "待开始"),

    /** 运行中 */
    RUNNING("RUNNING", "运行中"),

    /** 已暂停 */
    PAUSED("PAUSED", "已暂停"),

    /** 已停止 */
    STOPPED("STOPPED", "已停止"),

    /** 已完成 */
    COMPLETED("COMPLETED", "已完成"),

    /**已报废  */
    SCRAP("SCRAP", "已报废"),

    /** 故障 */
    ERROR("ERROR", "故障");

    private final String code;
    private final String info;

    TaskStatus(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

    public static TaskStatus fromCode(String code) {
        for (TaskStatus status : TaskStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的任务状态: " + code);
    }
}



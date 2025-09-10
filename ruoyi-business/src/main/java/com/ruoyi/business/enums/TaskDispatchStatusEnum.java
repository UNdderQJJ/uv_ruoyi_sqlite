package com.ruoyi.business.enums;

/**
 * 任务调度状态枚举
 */
public enum TaskDispatchStatusEnum {
    
    /** 初始化中 */
    INITIALIZING("INITIALIZING", "初始化中"),
    
    /** 运行中 */
    RUNNING("RUNNING", "运行中"),
    
    /** 已暂停 */
    PAUSED("PAUSED", "已暂停"),
    
    /** 已完成 */
    COMPLETED("COMPLETED", "已完成"),
    
    /** 失败 */
    FAILED("FAILED", "失败"),
    
    /** 已停止 */
    STOPPED("STOPPED", "已停止");
    
    private final String code;
    private final String info;
    
    TaskDispatchStatusEnum(String code, String info) {
        this.code = code;
        this.info = info;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getInfo() {
        return info;
    }
    
    public static TaskDispatchStatusEnum fromCode(String code) {
        for (TaskDispatchStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的任务调度状态: " + code);
    }
}

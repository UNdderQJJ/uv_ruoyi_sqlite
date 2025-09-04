package com.ruoyi.business.enums;

/**
 * 任务-设备关联状态
 * WAITING(等待), SENDING(下发数据中), PRINTING(打印中), COMPLETED(完成), ERROR(故障)
 */
public enum TaskDeviceStatus {

    /** 等待 */
    WAITING("WAITING", "等待"),

    /** 下发数据中 */
    SENDING("SENDING", "下发数据中"),

    /** 待打印 */
    WAITING_PRINT("WAITING_PRINT", "待打印"),

    /** 打印中 */
    PRINTING("PRINTING", "打印中"),

    /** 完成 */
    COMPLETED("COMPLETED", "完成"),

    /** 故障 */
    ERROR("ERROR", "故障");

    private final String code;
    private final String info;

    TaskDeviceStatus(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

    public static TaskDeviceStatus fromCode(String code) {
        for (TaskDeviceStatus status : TaskDeviceStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的任务设备状态: " + code);
    }
}



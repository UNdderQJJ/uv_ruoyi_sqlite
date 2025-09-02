package com.ruoyi.business.enums;

/**
 * 设备状态枚举
 * 
 * @author ruoyi
 */
public enum DeviceStatus {
    
    /** 离线 */
    OFFLINE("OFFLINE", "离线"),
    
    /** 在线空闲 */
    ONLINE_IDLE("ONLINE_IDLE", "在线空闲"),
    
    /** 在线打印 */
    ONLINE_PRINTING("ONLINE_PRINTING", "在线打印"),
    
    /** 在线扫描 */
    ONLINE_SCANNING("ONLINE_SCANNING", "在线扫描"),
    
    /** 故障 */
    ERROR("ERROR", "故障"),
    
    /** 维护 */
    MAINTENANCE("MAINTENANCE", "维护");

    private final String code;
    private final String info;

    DeviceStatus(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

    public static DeviceStatus fromCode(String code) {
        for (DeviceStatus status : DeviceStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的设备状态: " + code);
    }
}

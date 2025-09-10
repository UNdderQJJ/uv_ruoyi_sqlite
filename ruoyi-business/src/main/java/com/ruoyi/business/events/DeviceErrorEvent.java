package com.ruoyi.business.events;

/**
 * 设备错误事件
 */
public class DeviceErrorEvent extends TaskDispatchEvent {
    
    private final String deviceId;
    private final String errorMessage;
    
    public DeviceErrorEvent(Object source, Long taskId, String deviceId, String errorMessage) {
        super(source, taskId, "DEVICE_ERROR");
        this.deviceId = deviceId;
        this.errorMessage = errorMessage;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
}

package com.ruoyi.business.events;

/**
 * 指令完成事件
 */
public class CommandCompletedEvent extends TaskDispatchEvent {
    
    private final String deviceId;
    
    public CommandCompletedEvent(Object source, Long taskId, String deviceId) {
        super(source, taskId, "COMMAND_COMPLETED");
        this.deviceId = deviceId;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
}

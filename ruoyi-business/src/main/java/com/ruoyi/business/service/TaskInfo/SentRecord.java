package com.ruoyi.business.service.TaskInfo;

/**
 * 轻量已发送记录，携带任务ID、数据项ID与设备ID
 */
public class SentRecord {
    private Long taskId;
    private Long dataPoolItemId;
    private String deviceId;

    public SentRecord() {}

    public SentRecord(Long taskId, Long dataPoolItemId, String deviceId) {
        this.taskId = taskId;
        this.dataPoolItemId = dataPoolItemId;
        this.deviceId = deviceId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getDataPoolItemId() {
        return dataPoolItemId;
    }

    public void setDataPoolItemId(Long dataPoolItemId) {
        this.dataPoolItemId = dataPoolItemId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}



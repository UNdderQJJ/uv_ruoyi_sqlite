package com.ruoyi.business.service.TaskInfo;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 轻量已发送记录，携带任务ID、数据项ID与设备ID
 */
@Data
public class SentRecord {
    private Long taskId;
    private Long dataPoolItemId;
    private String dataPoolItemData;
    private String deviceId;
    private Long poolId;
    private String printTime;


    public SentRecord(Long taskId, Long dataPoolItemId,String dataPoolItemData, String deviceId,Long poolId) {
        this.taskId = taskId;
        this.dataPoolItemId = dataPoolItemId;
        this.dataPoolItemData = dataPoolItemData;
        this.deviceId = deviceId;
        this.poolId = poolId;
    }

    //打印时间为空时，给一个默认时间 yyyy-MM-dd HH:mm:ss
    public String getPrintTime() {
        if (printTime == null || printTime.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            printTime = LocalDateTime.now().format(formatter);
        }
        return printTime;
    }
}



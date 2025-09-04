package com.ruoyi.business.domain.TaskInfo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

/**
 * 任务-设备关联实体
 * 对应表: task_device_link
 */
@Data
public class TaskDeviceLink extends BaseEntity {

    /** 主键ID */
    private Long id;

    /** 所属任务ID */
    private Long taskId;

    /** 关联的设备ID */
    private Long deviceId;

    /** 设备名称（冗余） */
    private String deviceName;

    /** 指定的打印文件配置ID */
    private Long deviceFileConfigId;

    /** 数据池模板ID */
    private Long poolTemplateId;

    /** 任务中的设备状态（字符串保存） */
    private String status;

    /** 分配给设备的打印数量 */
    private Integer assignedQuantity;

    /** 设备已完成数量 */
    private Integer completedQuantity;

    /** 删除标记 0-正常 2-删除 */
    private Integer delFlag;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 最后更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}



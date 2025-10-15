package com.ruoyi.business.domain.TaskInfo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

/**
 * 任务中心 - 任务信息实体
 * 对应表: task_info
 */
@Data
public class TaskInfo extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务状态: PENDING, RUNNING, PAUSED, COMPLETED, ERROR（存库为字符串）
     */
    private String status;

    /**‘
     * 关联的设备名称
     */
    private String deviceName;

    /**
     * 关联的设备id
     */
    private String deviceId;

    /**
     * 设备类型 （打印机，读码器）
     */
    private String deviceType;

    /**
     * 关联的数据池ID
     */
    private Long poolId;

    /**
     * 数据池模版ID
     */
    private Long poolTemplateId;

    /**
     * 数据池名称（冗余便于展示）
     */
    private String poolName;

    /**
     * 计划打印总数 (-1 表示持续打印)
     */
    private Integer plannedQuantity;

    /**
     * 已完成打印总数
     */
    private Integer completedQuantity;

    /**
     * 打印总接收数
     */
    private Integer receivedQuantity;

    /**
     * 动态内容打印时, 提前下发的数据条数 缓存池大小
     */
    private Integer preloadDataCount;

    /**
     * 任务备注信息
     */
    private String description;

    /**
     * 删除标记 0-正常 2-删除
     */
    private Integer delFlag;

    /**
     * 是否启用质检
     */
    private Boolean enableCheck;
}



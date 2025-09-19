package com.ruoyi.business.domain.DataInspect;

import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Data;

/**
 * 产品质检记录实体，对应表 data_inspect
 */
@Data
public class DataInspect extends BaseEntity {

    /**
     * 主键
     */
    private Long id;

    /**
     * 打印数据id
     */
    private Long itemId;

    /**
     * 打印数据内容
     */
    private String itemData;

    /**
     * 数据池id
     */
    private Long poolId;

    /**
     * 数据池名称
     */
    private String poolName;

    /**
     * 任务id
     */
    private Long taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 打印设备id
     */
    private Long printDeviceId;

    /**
     * 打印设备名称
     */
    private String printDeviceName;

    /**
     * 打印时间
     */
    private String printTime;

    /**
     * 检测状态
     */
    private String inspectStatus; // PENDING, SUCCESS, FAILED, RECYCLED

    /**
     * 扫描设备id
     */
    private Long scanDeviceId;

    /**
     * 扫描设备名称
     */
    private String scanDeviceName;

    /**
     * 扫描内容
     */
    private String scannedContent;

    /**
     * 扫描时间
     */
    private String scanTime;
}



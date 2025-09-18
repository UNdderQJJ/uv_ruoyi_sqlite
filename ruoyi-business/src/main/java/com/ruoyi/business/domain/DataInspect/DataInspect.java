package com.ruoyi.business.domain.DataInspect;

import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Data;

/**
 * 产品质检记录实体，对应表 data_inspect
 */
@Data
public class DataInspect extends BaseEntity {

    private Long id;
    private Long itemId;
    private String itemData;
    private Long poolId;
    private Long taskId;
    private Long printDeviceId;
    private String printTime;
    private String inspectStatus; // PENDING, SUCCESS, FAILED, RECYCLED
    private Long scanDeviceId;
    private String scannedContent;
    private String scanTime;
}



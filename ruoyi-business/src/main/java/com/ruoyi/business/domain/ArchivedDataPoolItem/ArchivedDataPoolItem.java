package com.ruoyi.business.domain.ArchivedDataPoolItem;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

/**
 * 归档数据池项目对象 archived_data_pool_item
 * 
 * @author ruoyi
 */
@Data
public class ArchivedDataPoolItem extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID (直接使用原热表中的ID) */
    private Long id;

    /** 打印的数据 */
    @Excel(name = "打印数据")
    private String itemData;

    /** 最终状态 (PRINTED 或 FAILED) */
    @Excel(name = "最终状态")
    private String finalStatus;

    /** 最终打印次数 */
    @Excel(name = "打印次数")
    private Integer printCount;

    /** 所属数据池ID */
    @Excel(name = "数据池ID")
    private Long poolId;

    /** 数据池名称 (冗余字段) */
    @Excel(name = "数据池名称")
    private String poolName;

    /** 执行打印的设备ID或名称 */
    @Excel(name = "设备ID")
    private String deviceId;

    /** 数据进入系统的时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "接收时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date receivedTime;

    /** 打印完成的时间戳 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "打印时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date printedTime;

    /** 数据归档时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "归档时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date archivedTime;

    /** 扫描设备回传的原始数据 */
    @Excel(name = "校验数据")
    private String verificationData;

    /** 校验结果 */
    @Excel(name = "校验状态")
    private String verificationStatus;

    /** 是否删除 */
    private String delFlag;
    @Override
    public String toString() {
        return "ArchivedDataPoolItem{" +
                "id=" + id +
                ", itemData='" + itemData + '\'' +
                ", finalStatus='" + finalStatus + '\'' +
                ", printCount=" + printCount +
                ", poolId=" + poolId +
                ", poolName='" + poolName + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", receivedTime=" + receivedTime +
                ", printedTime=" + printedTime +
                ", archivedTime=" + archivedTime +
                ", verificationData='" + verificationData + '\'' +
                ", verificationStatus='" + verificationStatus + '\'' +
                ", delFlag='" + delFlag + '\'' +
                '}';
    }
}

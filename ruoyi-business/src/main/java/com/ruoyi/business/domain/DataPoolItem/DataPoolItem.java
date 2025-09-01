package com.ruoyi.business.domain.DataPoolItem;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 数据池热数据表 data_pool_item
 * 
 * @author ruoyi
 */
@Data
public class DataPoolItem extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    /** 主键 */
    private Long id;
    
    /** 数据池ID（外键） */
    @Excel(name = "数据池ID")
    private Long poolId;
    
    /** 待打印数据 */
    @Excel(name = "待打印数据")
    private String itemData;
    
    /** 数据状态 */
    @Excel(name = "数据状态", readConverterExp = "PENDING=待打印,PRINTING=正在打印,PRINTED=打印成功,FAILED=打印失败")
    private String status;
    
    /** 打印次数（用于失败重试） */
    @Excel(name = "打印次数")
    private Integer printCount;
    
    /** 设备ID（哪个设备正在处理） */
    @Excel(name = "设备ID")
    private String deviceId;
    
    /** 数据接收时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "数据接收时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date receivedTime;
    
    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    
    @Override
    public String toString() {
        return new StringBuilder()
            .append("DataPoolItem{")
            .append("id=").append(id)
            .append(", poolId=").append(poolId)
            .append(", itemData='").append(itemData).append('\'')
            .append(", status='").append(status).append('\'')
            .append(", printCount=").append(printCount)
            .append(", deviceId='").append(deviceId).append('\'')
            .append(", receivedTime=").append(receivedTime)
            .append(", delFlag='").append(delFlag).append('\'')
            .append(", createTime=").append(getCreateTime())
            .append(", updateTime=").append(getUpdateTime())
            .append('}')
            .toString();
    }
}

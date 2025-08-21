package com.ruoyi.business.domain;

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
    
    /** 锁定ID（哪个设备或线程正在处理） */
    @Excel(name = "锁定ID")
    private String lockId;
    
    /** 数据接收时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "数据接收时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date receivedTime;
    
    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setPoolId(Long poolId) {
        this.poolId = poolId;
    }
    
    public Long getPoolId() {
        return poolId;
    }
    
    public void setItemData(String itemData) {
        this.itemData = itemData;
    }
    
    public String getItemData() {
        return itemData;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setPrintCount(Integer printCount) {
        this.printCount = printCount;
    }
    
    public Integer getPrintCount() {
        return printCount;
    }
    
    public void setLockId(String lockId) {
        this.lockId = lockId;
    }
    
    public String getLockId() {
        return lockId;
    }
    
    public void setReceivedTime(Date receivedTime) {
        this.receivedTime = receivedTime;
    }
    
    public Date getReceivedTime() {
        return receivedTime;
    }
    
    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }
    
    public String getDelFlag() {
        return delFlag;
    }
    
    @Override
    public String toString() {
        return new StringBuilder()
            .append("DataPoolItem{")
            .append("id=").append(id)
            .append(", poolId=").append(poolId)
            .append(", itemData='").append(itemData).append('\'')
            .append(", status='").append(status).append('\'')
            .append(", printCount=").append(printCount)
            .append(", lockId='").append(lockId).append('\'')
            .append(", receivedTime=").append(receivedTime)
            .append(", delFlag='").append(delFlag).append('\'')
            .append(", createTime=").append(getCreateTime())
            .append(", updateTime=").append(getUpdateTime())
            .append('}')
            .toString();
    }
}

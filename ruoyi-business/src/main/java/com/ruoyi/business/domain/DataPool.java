package com.ruoyi.business.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 数据池对象 data_pool
 * 
 * @author ruoyi
 */
@Data
public class DataPool extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 数据池名称 */
    private String poolName;

    /** 数据源类型（U_DISK, TCP_SERVER, TCP_CLIENT, HTTP, MQTT, WEBSOCKET） */
    private String sourceType;

    /** 总数据量 */
    private Long totalCount;

    /** 待打印数量 */
    private Long pendingCount;

    /** 运行状态（IDLE, RUNNING, WARNING, ERROR） */
    private String status;

    /** 连接状态（DISCONNECTED, CONNECTING, CONNECTED） */
    private String connectionState;

    /** 详细配置 */
    private String sourceConfigJson;

    /** 解析规则 */
    private String parsingRuleJson;

    /** 触发条件配置 */
    private String triggerConfigJson;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 文件读取完成标志（0代表未完成 1代表已完成） */
    private String fileReadCompleted;

    /** 数据获取间隔时间（毫秒） */
    private Long dataFetchInterval;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("poolName", getPoolName())
            .append("sourceType", getSourceType())
            .append("totalCount", getTotalCount())
            .append("pendingCount", getPendingCount())
            .append("status", getStatus())
            .append("connectionState", getConnectionState())
            .append("sourceConfigJson", getSourceConfigJson())
            .append("parsingRuleJson", getParsingRuleJson())
            .append("triggerConfigJson", getTriggerConfigJson())
            .append("delFlag", getDelFlag())
            .append("fileReadCompleted", getFileReadCompleted())
            .append("dataFetchInterval", getDataFetchInterval())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}

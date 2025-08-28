package com.ruoyi.business.events;

/**
 * 数据池数量变更事件
 */
public class DataPoolCountChangedEvent {
    private final Long poolId;
    private final String poolName;
    private final String sourceType;

    private final Long oldTotalCount;
    private final Long newTotalCount;
    private final Long oldPendingCount;
    private final Long newPendingCount;

    private final long timestamp;

    public DataPoolCountChangedEvent(Long poolId, String poolName, String sourceType,
                                     Long oldTotalCount, Long newTotalCount,
                                     Long oldPendingCount, Long newPendingCount) {
        this.poolId = poolId;
        this.poolName = poolName;
        this.sourceType = sourceType;
        this.oldTotalCount = oldTotalCount;
        this.newTotalCount = newTotalCount;
        this.oldPendingCount = oldPendingCount;
        this.newPendingCount = newPendingCount;
        this.timestamp = System.currentTimeMillis();
    }

    public Long getPoolId() { return poolId; }
    public String getPoolName() { return poolName; }
    public String getSourceType() { return sourceType; }
    public Long getOldTotalCount() { return oldTotalCount; }
    public Long getNewTotalCount() { return newTotalCount; }
    public Long getOldPendingCount() { return oldPendingCount; }
    public Long getNewPendingCount() { return newPendingCount; }
    public long getTimestamp() { return timestamp; }
}



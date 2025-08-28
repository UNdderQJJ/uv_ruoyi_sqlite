package com.ruoyi.business.events;

import com.ruoyi.business.enums.ConnectionState;
import com.ruoyi.business.enums.PoolStatus;

/**
 * 数据池状态变更事件
 * 包含连接状态和运行状态的变更信息
 */
public class DataPoolStateChangedEvent {
    private final Long poolId;
    private final String poolName;
    private final String sourceType;
    
    // 连接状态变更
    private final ConnectionState oldConnectionState;
    private final ConnectionState newConnectionState;
    
    // 运行状态变更
    private final PoolStatus oldPoolStatus;
    private final PoolStatus newPoolStatus;
    
    // 变更时间
    private final long timestamp;

    public DataPoolStateChangedEvent(Long poolId, String poolName, String sourceType,
                                   ConnectionState oldConnectionState, ConnectionState newConnectionState,
                                   PoolStatus oldPoolStatus, PoolStatus newPoolStatus) {
        this.poolId = poolId;
        this.poolName = poolName;
        this.sourceType = sourceType;
        this.oldConnectionState = oldConnectionState;
        this.newConnectionState = newConnectionState;
        this.oldPoolStatus = oldPoolStatus;
        this.newPoolStatus = newPoolStatus;
        this.timestamp = System.currentTimeMillis();
    }

    public Long getPoolId() {
        return poolId;
    }

    public String getPoolName() {
        return poolName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public ConnectionState getOldConnectionState() {
        return oldConnectionState;
    }

    public ConnectionState getNewConnectionState() {
        return newConnectionState;
    }

    public PoolStatus getOldPoolStatus() {
        return oldPoolStatus;
    }

    public PoolStatus getNewPoolStatus() {
        return newPoolStatus;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 检查连接状态是否发生变化
     */
    public boolean isConnectionStateChanged() {
        return oldConnectionState != newConnectionState;
    }

    /**
     * 检查运行状态是否发生变化
     */
    public boolean isPoolStatusChanged() {
        return oldPoolStatus != newPoolStatus;
    }

    /**
     * 检查是否有任何状态发生变化
     */
    public boolean hasStateChanged() {
        return isConnectionStateChanged() || isPoolStatusChanged();
    }
}

package com.ruoyi.business.events;

import com.ruoyi.business.enums.ConnectionState;

/**
 * 连接状态变更事件
 */
public class ConnectionStateChangedEvent {
    private final Long poolId;
    private final ConnectionState connectionState;

    public ConnectionStateChangedEvent(Long poolId, ConnectionState connectionState) {
        this.poolId = poolId;
        this.connectionState = connectionState;
    }

    public Long getPoolId() {
        return poolId;
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }
}



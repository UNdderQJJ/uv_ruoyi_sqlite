package com.ruoyi.business.service.notification;

import com.ruoyi.business.domain.DataPool;
import com.ruoyi.business.events.DataPoolStateChangedEvent;
import com.ruoyi.business.enums.ConnectionState;
import com.ruoyi.business.enums.PoolStatus;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 数据池状态变更服务
 * 用于发布状态变更事件
 */
@Service
public class DataPoolStateChangeService implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(DataPoolStateChangeService.class);

    private final ApplicationEventPublisher eventPublisher;
    private ApplicationContext applicationContext;

    public DataPoolStateChangeService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 获取 IDataPoolService，避免直接依赖
     */
    private IDataPoolService getDataPoolService() {
        return applicationContext.getBean(IDataPoolService.class);
    }

    /**
     * 发布连接状态变更事件
     */
    public void publishConnectionStateChanged(Long poolId, String poolName, String sourceType,
                                           ConnectionState oldState, ConnectionState newState) {
        if (oldState == newState) {
            return; // 状态没有变化，不发布事件
        }

        try {
            // 从数据库获取当前完整的数据池信息
            IDataPoolService dataPoolService = getDataPoolService();
            DataPool dataPool = dataPoolService.selectDataPoolById(poolId);
            if (dataPool == null) {
                log.warn("未找到数据池: poolId={}", poolId);
                return;
            }

            // 获取当前运行状态
            PoolStatus currentPoolStatus = PoolStatus.fromCode(dataPool.getStatus());

            DataPoolStateChangedEvent event = new DataPoolStateChangedEvent(
                    poolId,
                    dataPool.getPoolName(),
                    dataPool.getSourceType(),
                    oldState, newState,
                    currentPoolStatus, currentPoolStatus
            );

            log.info("发布连接状态变更事件: poolId={}, {} -> {}",
                    poolId,
                    oldState != null ? oldState.getInfo() : "null",
                    newState != null ? newState.getInfo() : "null");

            eventPublisher.publishEvent(event);

        } catch (Exception e) {
            log.error("发布连接状态变更事件失败: poolId={}", poolId, e);
        }
    }

    /**
     * 发布运行状态变更事件
     */
    public void publishPoolStatusChanged(Long poolId, String poolName, String sourceType,
                                       PoolStatus oldStatus, PoolStatus newStatus) {
        if (oldStatus == newStatus) {
            return; // 状态没有变化，不发布事件
        }

        try {
            // 从数据库获取当前完整的数据池信息
            IDataPoolService dataPoolService = getDataPoolService();
            DataPool dataPool = dataPoolService.selectDataPoolById(poolId);
            if (dataPool == null) {
                log.warn("未找到数据池: poolId={}", poolId);
                return;
            }
            
            // 获取当前连接状态
            ConnectionState currentConnectionState = ConnectionState.fromCode(dataPool.getConnectionState());
            
            DataPoolStateChangedEvent event = new DataPoolStateChangedEvent(
                    poolId, 
                    dataPool.getPoolName(), 
                    dataPool.getSourceType(),
                    currentConnectionState, currentConnectionState,
                    oldStatus, newStatus
            );
            
            log.info("发布运行状态变更事件: poolId={}, {} -> {}", 
                    poolId, 
                    oldStatus != null ? oldStatus.getInfo() : "null",
                    newStatus != null ? newStatus.getInfo() : "null");
            
            eventPublisher.publishEvent(event);
            
        } catch (Exception e) {
            log.error("发布运行状态变更事件失败: poolId={}", poolId, e);
        }
    }
    
    /**
     * 发布完整状态变更事件
     */
    public void publishStateChanged(Long poolId, String poolName, String sourceType,
                                  ConnectionState oldConnectionState, ConnectionState newConnectionState,
                                  PoolStatus oldPoolStatus, PoolStatus newPoolStatus) {
        try {
            DataPoolStateChangedEvent event = new DataPoolStateChangedEvent(
                    poolId, poolName, sourceType,
                    oldConnectionState, newConnectionState,
                    oldPoolStatus, newPoolStatus
            );
            
            if (event.hasStateChanged()) {
                log.info("发布完整状态变更事件: poolId={}, poolName={}", poolId, poolName);
                eventPublisher.publishEvent(event);
            }
            
        } catch (Exception e) {
            log.error("发布完整状态变更事件失败: poolId={}", poolId, e);
        }
    }
    
    /**
     * 根据数据池对象发布状态变更事件
     */
    public void publishStateChanged(DataPool dataPool, ConnectionState oldConnectionState, 
                                  ConnectionState newConnectionState, PoolStatus oldPoolStatus, 
                                  PoolStatus newPoolStatus) {
        if (dataPool == null) {
            return;
        }
        
        publishStateChanged(
                dataPool.getId(),
                dataPool.getPoolName(),
                dataPool.getSourceType(),
                oldConnectionState,
                newConnectionState,
                oldPoolStatus,
                newPoolStatus
        );
    }
}

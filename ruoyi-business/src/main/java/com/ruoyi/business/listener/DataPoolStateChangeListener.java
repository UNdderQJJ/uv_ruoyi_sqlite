package com.ruoyi.business.listener;

import com.ruoyi.business.events.DataPoolStateChangedEvent;
import com.ruoyi.business.service.notification.WebSocketNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 数据池状态变更监听器
 * 监听状态变更事件并通知前端
 */
@Component
public class DataPoolStateChangeListener {
    
    private static final Logger log = LoggerFactory.getLogger(DataPoolStateChangeListener.class);
    
    @Autowired
    private WebSocketNotificationService webSocketNotificationService;
    
    /**
     * 监听数据池状态变更事件
     */
    @EventListener
    public void onDataPoolStateChanged(DataPoolStateChangedEvent event) {
        try {
            log.info("监听到数据池状态变更事件: poolId={}, poolName={}, sourceType={}", 
                    event.getPoolId(), event.getPoolName(), event.getSourceType());
            
            // 增加详细的空值检查日志
            log.debug("状态详情: oldConnectionState={}, newConnectionState={}, oldPoolStatus={}, newPoolStatus={}", 
                    event.getOldConnectionState(), 
                    event.getNewConnectionState(), 
                    event.getOldPoolStatus(), 
                    event.getNewPoolStatus());
            
            if (event.isConnectionStateChanged()) {
                log.info("连接状态变更: {} -> {}", 
                        event.getOldConnectionState() != null ? event.getOldConnectionState().getInfo() : "null",
                        event.getNewConnectionState() != null ? event.getNewConnectionState().getInfo() : "null");
            }
            
            if (event.isPoolStatusChanged()) {
                log.info("运行状态变更: {} -> {}", 
                        event.getOldPoolStatus() != null ? event.getOldPoolStatus().getInfo() : "null",
                        event.getNewPoolStatus() != null ? event.getNewPoolStatus().getInfo() : "null");
            }
            
            // 通知前端
            webSocketNotificationService.notifyStateChanged(event);
            
        } catch (Exception e) {
            log.error("处理数据池状态变更事件失败: poolId={}, 异常信息: {}", 
                    event.getPoolId(), e.getMessage(), e);
        }
    }
}

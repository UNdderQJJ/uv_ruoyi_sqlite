package com.ruoyi.business.service.DataPool.WebSocket;

import com.ruoyi.business.domain.DataPool;
import com.ruoyi.business.domain.config.TriggerConfig;
import com.ruoyi.business.enums.PoolStatus;
import com.ruoyi.business.enums.SourceType;
import com.ruoyi.business.service.DataPool.DataPoolConfigFactory;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * WebSocket数据调度服务
 * 定期检查WebSocket数据池状态，触发数据获取
 */
@Service
public class WebSocketDataSchedulerService {
    
    private static final Logger log = LoggerFactory.getLogger(WebSocketDataSchedulerService.class);
    
    @Autowired
    private IDataPoolService dataPoolService;
    
    @Autowired
    private WebSocketManager webSocketManager;
    
    /**
     * 定时检查WebSocket数据池
     * 每30秒执行一次
     */
    @Scheduled(fixedRate = 5000)
    public void scheduleWebSocketDataCheck() {
        try {
            
            // 查询所有运行中的WebSocket数据池
            DataPool queryParam = new DataPool();
            queryParam.setSourceType(SourceType.WEBSOCKET.getCode());
            queryParam.setStatus(PoolStatus.RUNNING.getCode());
            
            List<DataPool> webSocketPools = dataPoolService.selectDataPoolList(queryParam);
            
            if (webSocketPools.isEmpty()) {
//                log.debug("[WebSocketDataSchedulerService] 没有运行中的WebSocket数据池");
                return;
            }
            
            // 处理每个WebSocket数据池
            for (DataPool pool : webSocketPools) {
                try {
                    processPool(pool);
                } catch (Exception e) {
                    log.error("[WebSocketDataSchedulerService] 处理WebSocket数据池失败: poolId={}", pool.getId(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("[WebSocketDataSchedulerService] 检查WebSocket数据池时发生异常", e);
        }
    }
    
    /**
     * 处理单个WebSocket数据池
     */
    private void processPool(DataPool pool) {
        Long poolId = pool.getId();
        
        try {
            // 获取或创建WebSocketProvider
            WebSocketProvider provider = webSocketManager.getOrCreateProvider(poolId);
            
            // 检查连接状态
            if (!provider.isConnected()) {
                log.info("[WebSocketDataSchedulerService] WebSocket未连接，尝试连接: poolId={}", poolId);
                provider.connect();
                return;
            }
            
            // 检查是否需要发送指令
            if (shouldSendCommand(pool)) {
                log.debug("[WebSocketDataSchedulerService] 触发WebSocket指令发送: poolId={}", poolId);
                provider.sendMessage();
            }
            
        } catch (Exception e) {
            log.error("[WebSocketDataSchedulerService] 处理WebSocket数据池异常: poolId={}", poolId, e);
        }
    }
    
    /**
     * 判断是否需要发送指令
     * 这里可以根据业务逻辑来判断，比如：
     * 1. 定时发送心跳指令
     * 2. 根据数据池状态发送请求指令
     * 3. 根据配置的触发条件发送指令
     */
    private boolean shouldSendCommand(DataPool pool) {
        // 检查是否配置了请求指令
        if (pool.getTriggerConfigJson() == null) {
            return false;
        }
        
        try {
            // 解析触发配置
           DataPoolConfigFactory configFactory = new DataPoolConfigFactory();
            TriggerConfig triggerConfig = configFactory.parseTriggerConfig(pool.getTriggerConfigJson());
            
            // 如果有配置请求指令，则发送
            return triggerConfig != null && triggerConfig.getRequestCommand() != null;
            
        } catch (Exception e) {
            log.warn("[WebSocketDataSchedulerService] 解析触发配置失败: poolId={}", pool.getId(), e);
            return false;
        }
    }
    
    /**
     * 手动触发WebSocket数据获取
     * 供外部调用，比如通过TCP API触发
     */
    public void manualTriggerDataFetching(Long poolId) {
        try {
            log.info("[WebSocketDataSchedulerService] 手动触发WebSocket数据获取: poolId={}", poolId);
            
            DataPool pool = dataPoolService.selectDataPoolById(poolId);
            if (pool == null) {
                log.warn("[WebSocketDataSchedulerService] 数据池不存在: poolId={}", poolId);
                return;
            }
            
            if (!SourceType.WEBSOCKET.getCode().equals(pool.getSourceType())) {
                log.warn("[WebSocketDataSchedulerService] 数据池类型不是WebSocket: poolId={}, type={}", 
                        poolId, pool.getSourceType());
                return;
            }
            
            if (!PoolStatus.RUNNING.getCode().equals(pool.getStatus())) {
                log.warn("[WebSocketDataSchedulerService] 数据池未运行: poolId={}, status={}", 
                        poolId, pool.getStatus());
                return;
            }
            
            // 处理数据池
            processPool(pool);
            
        } catch (Exception e) {
            log.error("[WebSocketDataSchedulerService] 手动触发WebSocket数据获取失败: poolId={}", poolId, e);
        }
    }
}

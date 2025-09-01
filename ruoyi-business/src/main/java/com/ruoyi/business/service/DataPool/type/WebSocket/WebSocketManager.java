package com.ruoyi.business.service.DataPool.type.WebSocket;

import com.ruoyi.business.enums.ConnectionState;
import com.ruoyi.business.enums.PoolStatus;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.business.service.DataPool.DataPoolConfigFactory;
import com.ruoyi.business.service.common.DataIngestionService;
import com.ruoyi.business.service.common.ParsingRuleEngineService;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket管理器
 * 负责管理WebSocketProvider实例的生命周期
 */
@Component
public class WebSocketManager {
    
    private static final Logger log = LoggerFactory.getLogger(WebSocketManager.class);
    
    @Resource
    private IDataPoolService dataPoolService;
    
    @Resource
    private DataPoolConfigFactory configFactory;
    
    @Resource
    private DataIngestionService dataIngestionService;
    
    @Resource
    private ParsingRuleEngineService parsingRuleEngineService;
    @Resource
    private ApplicationEventPublisher eventPublisher;
    
    // 缓存Provider实例
    private final Map<Long, WebSocketProvider> providerCache = new ConcurrentHashMap<>();
    
    /**
     * 获取或创建WebSocketProvider实例
     */
    public WebSocketProvider getOrCreateProvider(Long poolId) {
        return providerCache.computeIfAbsent(poolId, id -> {
            log.info("创建新的 WebSocket 提供者，数据池ID: {}", id);
            WebSocketProvider provider = new WebSocketProvider(id, dataPoolService, configFactory, dataIngestionService, parsingRuleEngineService, eventPublisher);
            provider.connect();
            
            // 等待连接完成，最多等待1秒
            int maxWaitTime = 1000; // 1秒
            int waitInterval = 100; // 100毫秒检查一次
            int totalWaitTime = 0;
            
            while (!provider.isConnected() && totalWaitTime < maxWaitTime) {
                try {
                    Thread.sleep(waitInterval);
                    totalWaitTime += waitInterval;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            if (!provider.isConnected()) {
                dataPoolService.updateDataPoolStatus(id, PoolStatus.ERROR.getCode());
                dataPoolService.updateConnectionState(id, ConnectionState.ERROR.getCode());
                log.warn("[WebSocketManager] 初始化连接失败，不缓存Provider: poolId={}", id);
                throw new IllegalStateException("WebSocket连接失败，等待超时");
            }
            return provider;
        });
    }

    
    /**
     * 移除Provider实例
     */
    public void removeProvider(Long poolId) {
        WebSocketProvider provider = providerCache.remove(poolId);
        if (provider != null) {
            try {
                provider.close();
                log.info("[WebSocketManager] 已移除WebSocketProvider: poolId={}", poolId);
            } catch (Exception e) {
                log.warn("[WebSocketManager] 关闭WebSocketProvider时发生异常: poolId={}", poolId, e);
            }
        }
    }
    
    /**
     * 获取Provider实例（不创建新的）
     */
    public WebSocketProvider getProvider(Long poolId) {
        return providerCache.get(poolId);
    }
    
    /**
     * 检查Provider是否存在
     */
    public boolean hasProvider(Long poolId) {
        return providerCache.containsKey(poolId);
    }
    
    /**
     * 获取所有Provider的poolId
     */
    public java.util.Set<Long> getAllPoolIds() {
        return providerCache.keySet();
    }
    
    /**
     * 关闭所有Provider
     */
    public void closeAllProviders() {
        log.info("[WebSocketManager] 开始关闭所有WebSocketProvider，数量: {}", providerCache.size());
        
        providerCache.forEach((poolId, provider) -> {
            try {
                provider.close();
                log.debug("[WebSocketManager] 已关闭WebSocketProvider: poolId={}", poolId);
            } catch (Exception e) {
                log.warn("[WebSocketManager] 关闭WebSocketProvider时发生异常: poolId={}", poolId, e);
            }
        });
        
        providerCache.clear();
        log.info("[WebSocketManager] 所有WebSocketProvider已关闭");
    }
}

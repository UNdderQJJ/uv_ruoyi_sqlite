package com.ruoyi.business.service.DataPool.TcpServer.tcp;

import com.ruoyi.business.enums.ConnectionState;
import com.ruoyi.business.enums.PoolStatus;
import com.ruoyi.business.service.DataPool.DataPoolConfigFactory;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.business.service.common.DataIngestionService;
import com.ruoyi.business.service.common.ParsingRuleEngineService;
import org.springframework.context.ApplicationEventPublisher;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TcpClientProvider 管理器：生命周期管理 & 提供按池获取
 */
@Component
public class TcpClientManager {

    private static final Logger log = LoggerFactory.getLogger(TcpClientManager.class);

    @Resource
    private IDataPoolService dataPoolService;
    @Resource
    private DataPoolConfigFactory configFactory;
    @Resource
    private DataIngestionService ingestionService;
    @Resource
    private ParsingRuleEngineService parsingRuleEngineService;
    @Resource
    private ApplicationEventPublisher eventPublisher;

    private final Map<Long, TcpClientProvider> providers = new ConcurrentHashMap<>();

    public TcpClientProvider getOrCreateProvider(Long poolId) {
        return providers.computeIfAbsent(poolId, id -> {
            TcpClientProvider provider = new TcpClientProvider(id, dataPoolService, configFactory, ingestionService, parsingRuleEngineService, eventPublisher);
            provider.ensureConnected();
            
            // 等待连接完成，最多等待2秒
            int maxWaitTime = 2000; // 2秒
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
                // 未连接成功不缓存
                throw new IllegalStateException("TCP客户端连接失败，等待超时");
            }
            
            return provider;
        });
    }

    public void removeProvider(Long poolId) {
        TcpClientProvider provider = providers.remove(poolId);
        if (provider != null) {
            provider.close();
        }
    }

    /**
     * 根据数据池最新配置刷新 Provider
     */
    public void refreshProvider(Long poolId) {
        TcpClientProvider provider = providers.get(poolId);
        if (provider != null) {
            provider.reloadConfigs();
            provider.ensureConnected();
        }
    }
}



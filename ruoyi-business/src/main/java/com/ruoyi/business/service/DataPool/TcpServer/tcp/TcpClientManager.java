package com.ruoyi.business.service.DataPool.TcpServer.tcp;

import com.ruoyi.business.service.DataPool.DataPoolConfigFactory;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.business.service.common.DataIngestionService;
import com.ruoyi.business.service.common.ParsingRuleEngineService;
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

    private final Map<Long, TcpClientProvider> providers = new ConcurrentHashMap<>();

    public TcpClientProvider getOrCreateProvider(Long poolId) {
        return providers.computeIfAbsent(poolId, id -> {
            TcpClientProvider provider = new TcpClientProvider(id, dataPoolService, configFactory, ingestionService, parsingRuleEngineService);
            provider.ensureConnected();
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



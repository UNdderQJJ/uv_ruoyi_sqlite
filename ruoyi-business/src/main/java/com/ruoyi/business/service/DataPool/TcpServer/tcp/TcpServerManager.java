package com.ruoyi.business.service.DataPool.TcpServer.tcp;

import com.ruoyi.business.service.DataPool.DataPoolConfigFactory;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.business.service.DataPool.TcpServer.tcp.ingest.DataIngestionService;
import com.ruoyi.business.service.DataPool.TcpServer.tcp.parse.ParsingRuleEngineService;
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
public class TcpServerManager {

    private static final Logger log = LoggerFactory.getLogger(TcpServerManager.class);

    @Resource
    private IDataPoolService dataPoolService;
    @Resource
    private DataPoolConfigFactory configFactory;
    @Resource
    private DataIngestionService ingestionService;
    @Resource
    private ParsingRuleEngineService parsingRuleEngineService;

    private final Map<Long, TcpServerProvider> providers = new ConcurrentHashMap<>();

    public TcpServerProvider getOrCreateProvider(Long poolId) {
        return providers.computeIfAbsent(poolId, id -> {
            TcpServerProvider provider = new TcpServerProvider(id, dataPoolService, configFactory, ingestionService, parsingRuleEngineService);
            provider.ensureConnected();
            return provider;
        });
    }

    public void removeProvider(Long poolId) {
        TcpServerProvider provider = providers.remove(poolId);
        if (provider != null) {
            provider.close();
        }
    }

    /**
     * 根据数据池最新配置刷新 Provider
     */
    public void refreshProvider(Long poolId) {
        TcpServerProvider provider = providers.get(poolId);
        if (provider != null) {
            provider.reloadConfigs();
            provider.ensureConnected();
        }
    }
}



package com.ruoyi.business.service.DataPool.type.Mqtt;

import com.ruoyi.business.enums.ConnectionState;
import com.ruoyi.business.enums.PoolStatus;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.business.service.DataPool.DataPoolConfigFactory;
import com.ruoyi.business.service.common.ParsingRuleEngineService;
import com.ruoyi.business.service.common.DataIngestionService;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQTT 管理器
 * 管理所有 MQTT 提供者实例
 */
@Component
public class MqttManager {

    private static final Logger log = LoggerFactory.getLogger(MqttManager.class);

    @Resource
    private IDataPoolService dataPoolService;

    @Resource
    private DataPoolConfigFactory configFactory;

    @Resource
    private ParsingRuleEngineService parsingRuleEngineService;

    @Resource
    private DataIngestionService dataIngestionService;
    @Resource
    private ApplicationEventPublisher eventPublisher;

    // 缓存 MQTT 提供者实例
    private final Map<Long, MqttProvider> providers = new ConcurrentHashMap<>();

    /**
     * 获取或创建 MQTT 提供者
     */
    public MqttProvider getOrCreateProvider(Long poolId) {
        return providers.computeIfAbsent(poolId, new java.util.function.Function<Long, MqttProvider>() {
            @Override
            public MqttProvider apply(Long id) {
                log.info("创建新的 MQTT 提供者，数据池ID: {}", id);
                MqttProvider provider = new MqttProvider(id, dataPoolService, configFactory, dataIngestionService,
                        parsingRuleEngineService, eventPublisher);
                provider.connect();
                
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
                    // 未连接成功则不缓存，并抛出异常交由调用方处理
                    log.warn("[MqttManager] 初始化连接失败，不缓存Provider: poolId={}", id);
                    throw new IllegalStateException("MQTT连接失败，等待超时");
                }
                return provider;
            }
        });
    }

    /**
     * 移除 MQTT 提供者
     */
    public void removeProvider(Long poolId) {
        MqttProvider provider = providers.remove(poolId);
        if (provider != null) {
            log.info("移除 MQTT 提供者，数据池ID: {}", poolId);
            provider.close();
        }
    }

    /**
     * 获取指定数据池的 MQTT 提供者
     */
    public MqttProvider getProvider(Long poolId) {
        return providers.get(poolId);
    }

    /**
     * 检查指定数据池是否有活动的 MQTT 提供者
     */
    public boolean hasProvider(Long poolId) {
        return providers.containsKey(poolId);
    }

    /**
     * 获取所有数据池ID
     */
    public java.util.Set<Long> getAllPoolIds() {
        return providers.keySet();
    }

    /**
     * 根据数据池最新配置刷新 Provider
     */
    public void refreshProvider(Long poolId) {
        MqttProvider provider = providers.get(poolId);
        if (provider != null) {
            provider.reloadConfigs();
        }
    }

    /**
     * 关闭所有 MQTT 提供者
     */
    @PreDestroy
    public void closeAll() {
        log.info("开始关闭所有 MQTT 提供者，总数: {}", providers.size());

        providers.values().forEach(MqttProvider::close);
        providers.clear();

        log.info("所有 MQTT 提供者已关闭");
    }
}

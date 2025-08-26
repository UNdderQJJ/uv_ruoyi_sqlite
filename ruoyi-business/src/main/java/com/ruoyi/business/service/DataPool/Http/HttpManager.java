package com.ruoyi.business.service.DataPool.Http;

import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.business.service.DataPool.DataPoolConfigFactory;
import com.ruoyi.business.service.common.ParsingRuleEngineService;
import com.ruoyi.business.service.common.DataIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP 管理器
 * 管理所有 HTTP 提供者实例
 */
@Component
public class HttpManager {
    
    private static final Logger log = LoggerFactory.getLogger(HttpManager.class);
    
    @Resource
    private IDataPoolService dataPoolService;
    
    @Resource
    private DataPoolConfigFactory configFactory;
    
    @Resource
    private ParsingRuleEngineService parsingRuleEngineService;
    
    @Resource
    private DataIngestionService dataIngestionService;
    
    // 缓存 HTTP 提供者实例
    private final Map<Long, HttpProvider> providers = new ConcurrentHashMap<>();
    
    /**
     * 获取或创建 HTTP 提供者
     */
    public HttpProvider getOrCreateProvider(Long poolId) {
        return providers.computeIfAbsent(poolId, id -> {
            log.info("创建新的 HTTP 提供者，数据池ID: {}", id);
            HttpProvider provider = new HttpProvider(id, dataPoolService, configFactory, dataIngestionService, parsingRuleEngineService);
            return provider;
        });
    }
    
    /**
     * 移除 HTTP 提供者
     */
    public void removeProvider(Long poolId) {
        HttpProvider provider = providers.remove(poolId);
        if (provider != null) {
            log.info("移除 HTTP 提供者，数据池ID: {}", poolId);
            provider.close();
        }
    }
    
    /**
     * 获取指定数据池的 HTTP 提供者
     */
    public HttpProvider getProvider(Long poolId) {
        return providers.get(poolId);
    }
    
    /**
     * 检查指定数据池是否有活动的 HTTP 提供者
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
        HttpProvider provider = providers.get(poolId);
        if (provider != null) {
            provider.reloadConfigs();
        }
    }
    
    /**
     * 关闭所有 HTTP 提供者
     */
    @PreDestroy
    public void closeAll() {
        log.info("开始关闭所有 HTTP 提供者，总数: {}", providers.size());
        
        providers.values().forEach(HttpProvider::close);
        providers.clear();
        
        log.info("所有 HTTP 提供者已关闭");
    }
}

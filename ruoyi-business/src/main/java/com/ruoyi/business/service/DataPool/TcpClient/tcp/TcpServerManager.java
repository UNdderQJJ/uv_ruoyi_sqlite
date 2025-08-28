package com.ruoyi.business.service.DataPool.TcpClient.tcp;

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
 * TCP 服务端管理器
 * 管理所有 TCP 服务端提供者实例
 */
@Component
public class TcpServerManager {
    
    private static final Logger log = LoggerFactory.getLogger(TcpServerManager.class);
    
    @Resource
    private IDataPoolService dataPoolService;
    
    @Resource
    private ParsingRuleEngineService parsingRuleEngineService;
    
    @Resource
    private DataIngestionService dataIngestionService;
    
    @Resource
    private DataPoolConfigFactory configFactory;
    @Resource
    private ApplicationEventPublisher eventPublisher;
    
    // 缓存 TCP 服务端提供者实例
    private final Map<Long, TcpServerProvider> providers = new ConcurrentHashMap<>();
    
    /**
     * 获取或创建 TCP 服务端提供者
     */
    public TcpServerProvider getOrCreateProvider(Long poolId) {
        return providers.computeIfAbsent(poolId, id -> {
            log.info("创建新的 TCP 服务端提供者，数据池ID: {}", id);
            TcpServerProvider provider = new TcpServerProvider(id, dataPoolService, configFactory, dataIngestionService, parsingRuleEngineService, eventPublisher);
            provider.initialize();
            if (!provider.isConnected()) {
                // 未有客户端接入不缓存，让调用方决定重试时机
                throw new IllegalStateException("TCP服务端未有客户端接入");
            }
            return provider;
        });
    }
    
    /**
     * 移除 TCP 服务端提供者
     */
    public void removeProvider(Long poolId) {
        TcpServerProvider provider = providers.remove(poolId);
        if (provider != null) {
            log.info("移除 TCP 服务端提供者，数据池ID: {}", poolId);
            provider.close();
        }
    }
    
    /**
     * 获取指定数据池的 TCP 服务端提供者
     */
    public TcpServerProvider getProvider(Long poolId) {
        return providers.get(poolId);
    }
    
    /**
     * 检查指定数据池是否有活动的 TCP 服务端提供者
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
     * 关闭所有 TCP 服务端提供者
     */
    @PreDestroy
    public void closeAll() {
        log.info("开始关闭所有 TCP 服务端提供者，总数: {}", providers.size());
        
        providers.values().forEach(TcpServerProvider::close);
        providers.clear();
        
        log.info("所有 TCP 服务端提供者已关闭");
    }
}

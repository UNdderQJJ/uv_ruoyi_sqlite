package com.ruoyi.business.service.DataPool.type.WebSocket;

import com.ruoyi.business.domain.DataPool.DataPool;
import com.ruoyi.business.domain.config.WebSocketSourceConfig;
import com.ruoyi.business.domain.config.ParsingRuleConfig;
import com.ruoyi.business.domain.config.TriggerConfig;
import com.ruoyi.business.service.DataPool.DataPoolConfigFactory;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.business.service.common.DataIngestionService;
import com.ruoyi.business.service.common.ParsingRuleEngineService;
import org.springframework.context.ApplicationEventPublisher;
import com.ruoyi.business.events.ConnectionStateChangedEvent;
import com.ruoyi.business.enums.ConnectionState;
import com.ruoyi.business.enums.PoolStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * WebSocket 提供者
 * 负责连接WebSocket服务器、接收消息并处理
 */
public class WebSocketProvider {
    
    private static final Logger log = LoggerFactory.getLogger(WebSocketProvider.class);
    
    private final Long poolId;
    private final IDataPoolService dataPoolService;
    private final DataPoolConfigFactory configFactory;
    private final DataIngestionService dataIngestionService;
    private final ParsingRuleEngineService parsingRuleEngineService;
    private final ApplicationEventPublisher eventPublisher;
    
    private volatile WebSocketSourceConfig sourceConfig;
    private volatile TriggerConfig triggerConfig;
    private volatile ParsingRuleConfig parsingRuleConfig;
    
    // WebSocket客户端
    private final AtomicReference<WebSocketClient> webSocketClient = new AtomicReference<>();
    
    // 连接状态
    private volatile ConnectionState connectionState = ConnectionState.DISCONNECTED;
    
    // 防止重复连接
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    
    public WebSocketProvider(Long poolId,
                            IDataPoolService dataPoolService,
                            DataPoolConfigFactory configFactory,
                            DataIngestionService dataIngestionService,
                            ParsingRuleEngineService parsingRuleEngineService,
                            ApplicationEventPublisher eventPublisher) {
        this.poolId = poolId;
        this.dataPoolService = dataPoolService;
        this.configFactory = configFactory;
        this.dataIngestionService = dataIngestionService;
        this.parsingRuleEngineService = parsingRuleEngineService;
        this.eventPublisher = eventPublisher;
        
        reloadConfigs();
    }
    
    /**
     * 重新加载数据池配置（源、触发、解析）
     */
    public synchronized void reloadConfigs() {
        DataPool pool = dataPoolService.selectDataPoolById(poolId);
        if (pool == null) {
            log.warn("[WebSocketProvider] 数据池不存在: {}", poolId);
            return;
        }
        try {
            this.sourceConfig = (WebSocketSourceConfig) configFactory.parseSourceConfig("WEBSOCKET", pool.getSourceConfigJson());
        } catch (Exception e) {
            log.error("解析WebSocket配置失败: {}", e.getMessage(), e);
        }
        try {
            this.triggerConfig = configFactory.parseTriggerConfig(pool.getTriggerConfigJson());
        } catch (Exception e) {
            log.error("解析触发配置失败: {}", e.getMessage(), e);
        }
        try {
            this.parsingRuleConfig = configFactory.parseParsingRuleConfig(pool.getParsingRuleJson());
        } catch (Exception e) {
            log.error("解析规则配置失败: {}", e.getMessage(), e);
        }
        
        // 清除相关缓存，确保配置更新生效
        clearRelatedCache();
    }
    
    /**
     * 清除相关缓存
     */
    private void clearRelatedCache() {
        try {
            // 清除数据池配置缓存
            String cacheKey = "dataPool:config:" + poolId;
            // 这里可以通过SpringUtils获取RedisCache来清除缓存
            // 或者通过其他方式清除相关缓存
            log.debug("[WebSocketProvider] 已清除配置缓存: {}", cacheKey);
        } catch (Exception e) {
            log.warn("[WebSocketProvider] 清除缓存失败: {}", e.getMessage());
        }
    }
    
    /**
     * 连接WebSocket服务器
     */
    public void connect() {
        // 检查数据池状态
        DataPool latest = dataPoolService.selectDataPoolById(poolId);
        if (latest == null || !PoolStatus.RUNNING.getCode().equals(latest.getStatus())) {
            log.info("[WebSocketProvider] 数据池非运行状态，跳过连接: poolId={}", poolId);
            return;
        }
        
        // 验证配置
        if (sourceConfig == null || sourceConfig.getServerUrl() == null) {
            log.error("[WebSocketProvider] WebSocket配置不完整，无法连接: poolId={}", poolId);
            return;
        }
        
        // 防止重复连接
        if (!connecting.compareAndSet(false, true)) {
            log.debug("[WebSocketProvider] 正在连接中，跳过重复连接: poolId={}", poolId);
            return;
        }
        
        try {
            // 更新连接状态
            updateConnectionState(ConnectionState.CONNECTING);
            
            // 创建WebSocket客户端
            WebSocketClient client = new WebSocketClient(new URI(sourceConfig.getServerUrl()));
            
            // 设置消息处理器
            client.setMessageHandler(this::handleMessage);
            
            // 设置连接状态处理器
            client.setConnectionStateHandler(this::handleConnectionStateChange);
            
            // 连接到WebSocket服务器
            log.info("[WebSocketProvider] 连接WebSocket服务器: {}, poolId={}", sourceConfig.getServerUrl(), poolId);
            CompletableFuture<Void> connectFuture = client.connect();
            
            // 等待连接完成
            connectFuture.get();
            
            // 保存客户端引用
            webSocketClient.set(client);
            
            // 更新连接状态
            updateConnectionState(ConnectionState.CONNECTED);
            
            log.info("[WebSocketProvider] WebSocket连接成功: poolId={}", poolId);
            
        } catch (Exception e) {
            log.error("[WebSocketProvider] WebSocket连接失败: poolId={}", poolId, e);
            updateConnectionState(ConnectionState.ERROR);
        } finally {
            connecting.set(false);
        }
    }
    
    /**
     * 处理接收到的消息
     */
    private void handleMessage(String message) {
        try {
            log.debug("[WebSocketProvider] 收到WebSocket消息: poolId={}, message={}", poolId, message);
            
            // 处理接收到的消息
            processMessage(message);
            
        } catch (Exception e) {
            log.error("[WebSocketProvider] 处理WebSocket消息失败: poolId={}", poolId, e);
        }
    }
    
    /**
     * 处理连接状态变化
     */
    private void handleConnectionStateChange(ConnectionState newState) {
        log.info("[WebSocketProvider] WebSocket连接状态变化: poolId={}, newState={}", poolId, newState);
        this.connectionState = newState;
        dataPoolService.updateConnectionState(poolId, newState.getCode());
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new ConnectionStateChangedEvent(poolId, newState));
        }
        
        if (newState == ConnectionState.DISCONNECTED) {
            connecting.set(false);
        }
    }
    
    /**
     * 发送消息（由调度器触发）
     */
    public void sendMessage() {
        WebSocketClient client = webSocketClient.get();
        if (client == null || !client.isConnected()) {
            log.debug("[WebSocketProvider] WebSocket客户端未连接，跳过发送: poolId={}", poolId);
            return;
        }
        
        if (triggerConfig == null || triggerConfig.getRequestCommand() == null) {
            log.debug("[WebSocketProvider] 未配置请求消息，跳过发送: poolId={}", poolId);
            return;
        }
        
        try {
            String message = triggerConfig.getRequestCommand();
            
            CompletableFuture<Void> sendFuture = client.sendMessage(message);
            sendFuture.get(); // 等待发送完成
            
            log.debug("[WebSocketProvider] 发送消息成功: message={}, poolId={}", message, poolId);
            
        } catch (Exception e) {
            log.error("[WebSocketProvider] 发送消息失败: poolId={}", poolId, e);
        }
    }
    
    /**
     * 处理接收到的消息
     */
    private void processMessage(String messagePayload) {
        if (parsingRuleConfig == null) {
            log.error("[WebSocketProvider] 解析规则配置为空，无法处理消息: poolId={}", poolId);
            return;
        }
        
        try {
            // 解析数据
            List<String> items = parsingRuleEngineService.extractItems(messagePayload, parsingRuleConfig);
            if (items == null || items.isEmpty()) {
                log.debug("[WebSocketProvider] 解析结果为空: poolId={}", poolId);
                return;
            }
            
            // 数据入库
            dataIngestionService.ingestItems(poolId, items);
            
            log.info("[WebSocketProvider] 数据处理完成: poolId={}, 解析出 {} 条记录", poolId, items.size());
            
        } catch (Exception e) {
            log.error("[WebSocketProvider] 处理消息数据失败: poolId={}", poolId, e);
        }
    }
    
    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        WebSocketClient client = webSocketClient.get();
        return client != null && client.isConnected();
    }
    
    /**
     * 获取连接状态
     */
    public ConnectionState getConnectionState() {
        return connectionState;
    }
    
    /**
     * 关闭WebSocket连接
     */
    public void close() {
        try {
            WebSocketClient client = webSocketClient.getAndSet(null);
            if (client != null && client.isConnected()) {
                log.info("[WebSocketProvider] 关闭WebSocket连接: poolId={}", poolId);
                // 先取消连接，避免等待服务器响应
                client.cancel();
            }
        } catch (Exception e) {
            log.error("[WebSocketProvider] 关闭WebSocket连接失败: poolId={}", poolId, e);
        } finally {
            updateConnectionState(ConnectionState.DISCONNECTED);
            connecting.set(false);
        }
    }

    private void updateConnectionState(ConnectionState state) {
        this.connectionState = state;
        try {
            dataPoolService.updateConnectionState(poolId, state.getCode());
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new ConnectionStateChangedEvent(poolId, state));
            }
        } catch (Exception e) {
            log.error("[WebSocketProvider] 更新连接状态失败: poolId={}", poolId, e);
        }
    }
    
    /**
     * 获取数据池ID
     */
    public Long getPoolId() {
        return poolId;
    }
}

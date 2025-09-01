package com.ruoyi.business.service.DataPool.Mqtt;

import com.ruoyi.business.domain.DataPool;
import com.ruoyi.business.domain.config.MqttSourceConfig;
import com.ruoyi.business.domain.config.ParsingRuleConfig;
import com.ruoyi.business.domain.config.TriggerConfig;
import com.ruoyi.business.service.DataPool.DataPoolConfigFactory;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.business.service.common.DataIngestionService;
import com.ruoyi.business.service.common.ParsingRuleEngineService;
import com.ruoyi.business.events.ConnectionStateChangedEvent;
import com.ruoyi.business.enums.ConnectionState;
import com.ruoyi.business.enums.PoolStatus;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MQTT 提供者
 * 负责连接MQTT代理、订阅主题、接收消息并处理
 */
public class MqttProvider {
    
    private static final Logger log = LoggerFactory.getLogger(MqttProvider.class);
    
    private final Long poolId;
    private final IDataPoolService dataPoolService;
    private final DataPoolConfigFactory configFactory;
    private final DataIngestionService dataIngestionService;
    private final ParsingRuleEngineService parsingRuleEngineService;
    private final ApplicationEventPublisher eventPublisher;
    
    private volatile MqttSourceConfig sourceConfig;
    private volatile TriggerConfig triggerConfig;
    private volatile ParsingRuleConfig parsingRuleConfig;
    
    // MQTT客户端
    private Mqtt5AsyncClient mqttClient;
    
    // 连接状态
    private volatile ConnectionState connectionState = ConnectionState.DISCONNECTED;
    
    // 防止重复连接
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    
    public MqttProvider(Long poolId,
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
            log.warn("[MqttProvider] 数据池不存在: {}", poolId);
            return;
        }
        try {
            this.sourceConfig = (MqttSourceConfig) configFactory.parseSourceConfig("MQTT", pool.getSourceConfigJson());
        } catch (Exception e) {
            log.error("解析MQTT配置失败: {}", e.getMessage(), e);
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
    }
    
    /**
     * 连接MQTT代理
     */
    public void connect() {
        // 检查数据池状态
        DataPool latest = dataPoolService.selectDataPoolById(poolId);
        if (latest == null || !PoolStatus.RUNNING.getCode().equals(latest.getStatus())) {
            log.info("[MqttProvider] 数据池非运行状态，跳过连接: poolId={}", poolId);
            return;
        }
        
        // 验证配置
        if (sourceConfig == null || sourceConfig.getBrokerAddress() == null || sourceConfig.getPort() == null) {
            log.error("[MqttProvider] MQTT配置不完整，无法连接: poolId={}", poolId);
            return;
        }
        
        // 防止重复连接
        if (!connecting.compareAndSet(false, true)) {
            log.debug("[MqttProvider] 正在连接中，跳过重复连接: poolId={}", poolId);
            return;
        }
        
        try {
            // 更新连接状态
            updateConnectionState(ConnectionState.CONNECTING);
            
            // 构建连接地址
            String brokerUrl = sourceConfig.getBrokerAddress() + ":" + sourceConfig.getPort();
            
            // 创建MQTT客户端
            String clientId = sourceConfig.getClientId() != null ? sourceConfig.getClientId() : 
                             "ruoyi-mqtt-client-" + poolId + "-" + System.currentTimeMillis();
            
            String host = sourceConfig.getBrokerAddress().replace("mqtt://", "");
            
            mqttClient = Mqtt5Client.builder()
                    .identifier(clientId)
                    .serverHost(host)
                    .serverPort(sourceConfig.getPort())
                    .buildAsync();
            
            // 设置消息回调 - 使用更直接的方法
            mqttClient.toAsync().publishes(MqttGlobalPublishFilter.ALL, this::handleMessage);
            
            // 构建连接选项
            var connectBuilder = mqttClient.connectWith()
                    .cleanStart(true)
                    .keepAlive(60);
            
            // 设置认证信息
            if (sourceConfig.getUsername() != null && !sourceConfig.getUsername().trim().isEmpty()) {
                connectBuilder.simpleAuth()
                        .username(sourceConfig.getUsername())
                        .password(sourceConfig.getPassword() != null ? 
                                sourceConfig.getPassword().getBytes(StandardCharsets.UTF_8) : 
                                new byte[0])
                        .applySimpleAuth();
            }
            
            // 连接到MQTT代理
            log.info("[MqttProvider] 连接MQTT代理: {}, poolId={}", brokerUrl, poolId);
            CompletableFuture<Mqtt5ConnAck> connectFuture = connectBuilder.send();
            
            // 等待连接完成，设置12秒超时
            Mqtt5ConnAck connAck = null;
            try {
                connAck = connectFuture.get(12, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                if (e instanceof TimeoutException) {
                    log.error("[MqttProvider] MQTT连接超时: poolId={}", poolId);
                } else {
                    log.error("[MqttProvider] MQTT连接异常: poolId={}, error={}", poolId, e.getMessage());
                }
                throw new RuntimeException("MQTT连接失败: " + e.getMessage(), e);
            }
            if (connAck != null && connAck.getReasonCode().isError()) {
                throw new Exception("MQTT连接失败: " + connAck.getReasonCode());
            }

            // 订阅主题
            if (triggerConfig != null && triggerConfig.getSubscribeTopic() != null) {
                String topic = triggerConfig.getSubscribeTopic();
                CompletableFuture<Mqtt5SubAck> subscribeFuture = mqttClient.subscribeWith()
                        .topicFilter(topic)
                        .send();
                
                Mqtt5SubAck subAck = null;
                try {
                    subAck = subscribeFuture.get(5, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    if (e instanceof TimeoutException) {
                        log.warn("[MqttProvider] 订阅主题超时: {}, poolId={}", topic, poolId);
                    } else {
                        log.warn("[MqttProvider] 订阅主题异常: {}, poolId={}, error={}", topic, poolId, e.getMessage());
                    }
                    // 订阅失败不影响连接，继续执行
                }
                
                if (subAck != null && subAck.getReasonCodes().get(0).isError()) {
                    log.warn("[MqttProvider] 订阅主题失败: {}, poolId={}", topic, poolId);
                } else if (subAck != null) {
                    log.info("[MqttProvider] 订阅主题成功: {}, poolId={}", topic, poolId);
                }
            } else {
                log.warn("[MqttProvider] 未配置订阅主题: poolId={}", poolId);
            }
            
            // 更新连接状态
            updateConnectionState(ConnectionState.CONNECTED);
            
            log.info("[MqttProvider] MQTT连接成功: poolId={}", poolId);
            
        } catch (Exception e) {
            log.error("[MqttProvider] MQTT连接失败: poolId={}", poolId, e);
            updateConnectionState(ConnectionState.ERROR);
        } finally {
            connecting.set(false);
        }
    }
    
    /**
     * 处理接收到的消息
     */
    private void handleMessage(Mqtt5Publish publish) {
        try {
            String topic = publish.getTopic().toString();
            String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
            log.debug("[MqttProvider] 收到MQTT消息: poolId={}, topic={}, payload={}", 
                    poolId, topic, payload);
            
            // 处理接收到的消息
            processMessage(payload);
            
        } catch (Exception e) {
            log.error("[MqttProvider] 处理MQTT消息失败: poolId={}", poolId, e);
        }
    }
    
    /**
     * 发布消息（由调度器触发）
     */
    public void publishMessage() {
        if (mqttClient == null || !mqttClient.getState().isConnected()) {
            log.debug("[MqttProvider] MQTT客户端未连接，跳过发布: poolId={}", poolId);
            return;
        }
        
        if (triggerConfig == null || triggerConfig.getPublishTopic() == null) {
            log.debug("[MqttProvider] 未配置发布主题，跳过发布: poolId={}", poolId);
            return;
        }
        
        try {
            String topic = triggerConfig.getPublishTopic();
            String payload = triggerConfig.getRequestPayload() != null ? 
                           triggerConfig.getRequestPayload() : "";
            
            var publishFuture = mqttClient.publishWith()
                    .topic(topic)
                    .payload(payload.getBytes(StandardCharsets.UTF_8))
                    .send();
            
            // 设置5秒超时，避免无限等待
            publishFuture.get(5, TimeUnit.SECONDS);
            log.debug("[MqttProvider] 发布消息成功: topic={}, payload={}, poolId={}", 
                    topic, payload, poolId);
            
        } catch (Exception e) {
            if (e instanceof TimeoutException) {
                log.error("[MqttProvider] 发布消息超时: poolId={}", poolId);
            } else {
                log.error("[MqttProvider] 发布消息失败: poolId={}", poolId, e);
            }
        }
    }
    
    /**
     * 处理接收到的消息
     */
    private void processMessage(String messagePayload) {
        if (parsingRuleConfig == null) {
            log.error("[MqttProvider] 解析规则配置为空，无法处理消息: poolId={}", poolId);
            return;
        }
        
        try {
            // 解析数据
            List<String> items = parsingRuleEngineService.extractItems(messagePayload, parsingRuleConfig);
            if (items == null || items.isEmpty()) {
                log.debug("[MqttProvider] 解析结果为空: poolId={}", poolId);
                return;
            }
            
            // 数据入库
            dataIngestionService.ingestItems(poolId, items);
            
            log.info("[MqttProvider] 数据处理完成: poolId={}, 解析出 {} 条记录", poolId, items.size());
            
        } catch (Exception e) {
            log.error("[MqttProvider] 处理消息数据失败: poolId={}", poolId, e);
        }
    }
    
    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return mqttClient != null && mqttClient.getState().isConnected();
    }
    
    /**
     * 获取连接状态
     */
    public ConnectionState getConnectionState() {
        return connectionState;
    }
    
    /**
     * 关闭MQTT连接
     */
    public void close() {
        try {
            if (mqttClient != null && mqttClient.getState().isConnected()) {
                log.debug("[MqttProvider] 开始关闭MQTT连接: poolId={}", poolId);
                // 设置5秒超时，避免无限等待
                mqttClient.disconnectWith()
                        .reasonString("正常关闭")
                        .send()
                        .get(5, TimeUnit.SECONDS);
                log.debug("[MqttProvider] MQTT连接关闭成功: poolId={}", poolId);
            }
        } catch (Exception e) {
            if (e instanceof TimeoutException) {
                log.warn("[MqttProvider] 关闭MQTT连接超时: poolId={}", poolId);
            } else {
                log.error("[MqttProvider] 关闭MQTT连接失败: poolId={}", poolId, e);
            }
        } finally {
            updateConnectionState(ConnectionState.DISCONNECTED);
            connecting.set(false);
        }
    }

    /**
     * 统一更新连接状态并联动调度器
     */
    private void updateConnectionState(ConnectionState newState) {
        this.connectionState = newState;
        try {
            dataPoolService.updateConnectionState(poolId, newState.getCode());
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new ConnectionStateChangedEvent(poolId, newState));
            }
        } catch (Exception e) {
            log.error("[MqttProvider] 更新连接状态失败: poolId={}", poolId, e);
        }
    }
    
    /**
     * 获取数据池ID
     */
    public Long getPoolId() {
        return poolId;
    }
}

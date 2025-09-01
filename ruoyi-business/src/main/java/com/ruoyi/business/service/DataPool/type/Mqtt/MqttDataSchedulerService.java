package com.ruoyi.business.service.DataPool.type.Mqtt;

import com.ruoyi.business.domain.DataPool.DataPool;
import com.ruoyi.business.enums.PoolStatus;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * MQTT 数据调度服务
 * 定期检查 MQTT 数据池状态，触发连接和消息发布
 */
@Service
public class MqttDataSchedulerService {
    
    private static final Logger log = LoggerFactory.getLogger(MqttDataSchedulerService.class);
    
    @Resource
    private IDataPoolService dataPoolService;
    
    @Resource
    private MqttManager mqttManager;
    
    /**
     * 定时检查 MQTT 数据池
     * 每5秒执行一次
     */
//    @Scheduled(fixedRate = 5000)
    public void scheduledCheckDataPools() {
        try {
            // 查询所有运行中的 MQTT 数据池
            DataPool queryParam = new DataPool();
            queryParam.setSourceType("MQTT");
            queryParam.setStatus(PoolStatus.RUNNING.getCode());
            queryParam.setDelFlag("0"); // 未删除
            
            List<DataPool> mqttDataPools = dataPoolService.selectDataPoolList(queryParam);
            
            if (mqttDataPools.isEmpty()) {
                return;
            }
            
            // 处理每个 MQTT 数据池
            for (DataPool dataPool : mqttDataPools) {
                try {
                    processPool(dataPool);
                } catch (Exception e) {
                    log.error("[MqttScheduler] 处理数据池 {} 失败: {}", dataPool.getId(), e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("[MqttScheduler] 检查 MQTT 数据池时发生异常", e);
        }
    }
    
    /**
     * 处理单个 MQTT 数据池
     */
    private void processPool(DataPool dataPool) {
        Long poolId = dataPool.getId();
        
        try {
            // 获取或创建 MQTT 提供者
            MqttProvider provider = mqttManager.getOrCreateProvider(poolId);
            
            // 检查连接状态
            if (!provider.isConnected()) {
                log.debug("[MqttScheduler] MQTT 未连接，尝试连接: poolId={}", poolId);
                provider.connect();
                return;
            }
            
            // 检查是否需要触发发布
            if (shouldTriggerPublish(dataPool, provider)) {
                log.debug("[MqttScheduler] 触发 MQTT 消息发布: poolId={}", poolId);
                provider.publishMessage();
            } else {
                log.debug("[MqttScheduler] 跳过 MQTT 消息发布: poolId={}", poolId);
            }
            
        } catch (Exception e) {
            log.error("[MqttScheduler] 处理 MQTT 数据池 {} 失败: {}", poolId, e.getMessage(), e);
        }
    }
    
    /**
     * 判断是否需要触发发布
     */
    private boolean shouldTriggerPublish(DataPool dataPool, MqttProvider provider) {
        // 检查阈值触发
        Long pendingCount = dataPool.getPendingCount();
        if (pendingCount != null && pendingCount > 0) {
            // 如果有待处理数据，不触发新发布
            return false;
        }
        
        // 检查间隔触发（这里可以根据实际需求调整）
        // 目前简单实现：只要没有待处理数据且已连接，就触发
        return true;
    }
    
    /**
     * 手动触发 MQTT 连接
     */
    public void manualTriggerConnect(Long poolId) {
        try {
            DataPool dataPool = dataPoolService.selectDataPoolById(poolId);
            if (dataPool == null) {
                log.warn("[MqttScheduler] 数据池不存在: {}", poolId);
                return;
            }
            
            if (!"MQTT".equals(dataPool.getSourceType())) {
                log.warn("[MqttScheduler] 数据池类型不是 MQTT: {}", poolId);
                return;
            }
            
            MqttProvider provider = mqttManager.getOrCreateProvider(poolId);
            provider.connect();
            
            log.info("[MqttScheduler] 手动触发 MQTT 连接成功，数据池ID: {}", poolId);
            
        } catch (Exception e) {
            log.error("[MqttScheduler] 手动触发 MQTT 连接失败，数据池ID: {}", poolId, e);
        }
    }
    
    /**
     * 手动触发 MQTT 消息发布
     */
    public void manualTriggerPublish(Long poolId) {
        try {
            DataPool dataPool = dataPoolService.selectDataPoolById(poolId);
            if (dataPool == null) {
                log.warn("[MqttScheduler] 数据池不存在: {}", poolId);
                return;
            }
            
            if (!"MQTT".equals(dataPool.getSourceType())) {
                log.warn("[MqttScheduler] 数据池类型不是 MQTT: {}", poolId);
                return;
            }
            
            MqttProvider provider = mqttManager.getOrCreateProvider(poolId);
            provider.publishMessage();
            
            log.info("[MqttScheduler] 手动触发 MQTT 消息发布成功，数据池ID: {}", poolId);
            
        } catch (Exception e) {
            log.error("[MqttScheduler] 手动触发 MQTT 消息发布失败，数据池ID: {}", poolId, e);
        }
    }
}

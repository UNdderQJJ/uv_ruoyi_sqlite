package com.ruoyi.business.service.DataPool.type.TcpClient;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.business.domain.DataPool.DataPool;
import com.ruoyi.business.domain.config.TriggerConfig;
import com.ruoyi.business.enums.PoolStatus;
import com.ruoyi.business.enums.SourceType;
import com.ruoyi.business.enums.TriggerType;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.business.service.DataPool.type.TcpClient.tcp.TcpServerManager;
import com.ruoyi.business.service.DataPool.type.TcpClient.tcp.TcpServerProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * TCP 服务端数据调度器
 * 定时检查 TCP_CLIENT 类型（作为服务端监听端口）的数据池
 * 当 pending_count 低于阈值时，触发数据请求
 */
@Service
public class TcpServerDataSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(TcpServerDataSchedulerService.class);

    @Resource
    private IDataPoolService dataPoolService;
    
    @Resource
    private TcpServerManager tcpServerManager;

    private static final int DEFAULT_THRESHOLD = 100;

    /**
     * 定时检查数据池状态
     * 每5秒执行一次
     */
//    @Scheduled(fixedDelay = 5000)
    public void scheduledCheckDataPools() {
        try {
            DataPool query = new DataPool();
            query.setSourceType(SourceType.TCP_CLIENT.getCode());
            query.setStatus(PoolStatus.RUNNING.getCode());
            query.setDelFlag("0"); // 未删除

            List<DataPool> pools = dataPoolService.selectDataPoolList(query);
            
            if (pools == null || pools.isEmpty()) {
                return;
            }
            
            for (DataPool pool : pools) {
                try {
                    processPool(pool);
                } catch (Exception e) {
                    log.error("[TcpClientScheduler] 处理数据池 {} 异常: {}", pool.getPoolName(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("[TcpClientScheduler] 调度异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理单个数据池
     */
    private void processPool(DataPool pool) {
        try {
            TriggerConfig trigger = parseTrigger(pool);
            int threshold = trigger != null && trigger.getThreshold() != null ? trigger.getThreshold() : DEFAULT_THRESHOLD;

            // 判断是否触发数据请求
            if (shouldTriggerDataRequest(pool, trigger, threshold)) {
                TcpServerProvider provider = tcpServerManager.getOrCreateProvider(pool.getId());
                provider.requestData();
                log.debug("[TcpClientScheduler] 触发数据请求，数据池ID: {}, 阈值: {}", pool.getId(), threshold);
            }
        } catch (Exception e) {
            log.error("[TcpClientScheduler] 处理数据池 {} 失败: {}", pool.getId(), e.getMessage(), e);
        }
    }

    /**
     * 判断是否应该触发数据请求
     */
    private boolean shouldTriggerDataRequest(DataPool pool, TriggerConfig trigger, int threshold) {
        if (trigger == null || StringUtils.isBlank(trigger.getTriggerType())) {
            // 默认使用阈值触发
            return isPendingCountBelowThreshold(pool, threshold);
        }
        
        if (TriggerType.THRESHOLD.getCode().equals(trigger.getTriggerType())) {
            // 阈值触发：当 pending_count < threshold 时触发
            return isPendingCountBelowThreshold(pool, threshold);
        }
        
        if (TriggerType.INTERVAL.getCode().equals(trigger.getTriggerType())) {
            // 间隔触发：每次调度都触发
            return true;
        }
        
        // MANUAL 或未知类型：不自动触发
        return false;
    }

    /**
     * 检查 pending_count 是否低于阈值
     */
    private boolean isPendingCountBelowThreshold(DataPool pool, int threshold) {
        Long pendingCount = pool.getPendingCount();
        if (pendingCount == null) {
            // 如果 pending_count 为空，认为需要触发
            return true;
        }
        return pendingCount < threshold;
    }

    /**
     * 解析触发配置
     */
    private TriggerConfig parseTrigger(DataPool pool) {
        if (StringUtils.isBlank(pool.getTriggerConfigJson())) {
            return null;
        }
        
        try {
            return JSON.parseObject(pool.getTriggerConfigJson(), TriggerConfig.class);
        } catch (Exception e) {
            log.error("[TcpClientScheduler] 解析触发配置失败，数据池ID: {}, 错误: {}", pool.getId(), e.getMessage());
            return null;
        }
    }
}

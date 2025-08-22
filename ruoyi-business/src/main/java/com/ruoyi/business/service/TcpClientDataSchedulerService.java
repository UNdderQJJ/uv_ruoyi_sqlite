package com.ruoyi.business.service;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.business.domain.DataPool;
import com.ruoyi.business.domain.config.TriggerConfig;
import com.ruoyi.business.enums.PoolStatus;
import com.ruoyi.business.enums.SourceType;
import com.ruoyi.business.enums.TriggerType;
import com.ruoyi.business.tcp.TcpClientManager;
import com.ruoyi.business.tcp.TcpClientProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * TCP 客户端数据调度器
 * 定时检查 TCP_SERVER 类型（作为客户端连接远端）的数据池
 */
@Service
public class TcpClientDataSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(TcpClientDataSchedulerService.class);

    @Resource
    private IDataPoolService dataPoolService;
    @Resource
    private TcpClientManager tcpClientManager;

    private static final int DEFAULT_THRESHOLD = 100;

    @Scheduled(fixedDelay = 5000)
    public void scheduledCheckDataPools() {
        try {
            DataPool query = new DataPool();
            query.setSourceType(SourceType.TCP_SERVER.getCode());
            query.setStatus(PoolStatus.RUNNING.getCode());
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

    private void processPool(DataPool pool) {
        TriggerConfig trigger = parseTrigger(pool);
        int threshold = trigger != null && trigger.getThreshold() != null ? trigger.getThreshold() : DEFAULT_THRESHOLD;

        // 连接保障
        tcpClientManager.getOrCreateProvider(pool.getId()).ensureConnected();

        // 判断是否触发请求
        if (shouldTrigger(pool, trigger, threshold)) {
            tcpClientManager.getOrCreateProvider(pool.getId()).requestDataIfConnected();
        }
    }

    private boolean shouldTrigger(DataPool pool, TriggerConfig trigger, int threshold) {
        if (trigger == null || StringUtils.isBlank(trigger.getTriggerType())) {
            return pool.getPendingCount() < threshold;
        }
        if (TriggerType.THRESHOLD.getCode().equals(trigger.getTriggerType())) {
            return pool.getPendingCount() < threshold;
        }
        if (TriggerType.INTERVAL.getCode().equals(trigger.getTriggerType())) {
            return true;
        }
        return false; // MANUAL 或未知类型
    }

    private TriggerConfig parseTrigger(DataPool pool) {
        if (StringUtils.isBlank(pool.getTriggerConfigJson())) {
            return null;
        }
        try {
            return JSON.parseObject(pool.getTriggerConfigJson(), TriggerConfig.class);
        } catch (Exception e) {
            log.error("解析触发配置失败: {}", e.getMessage());
            return null;
        }
    }
}



package com.ruoyi.business.service.DataPool.Http;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.business.domain.DataPool;
import com.ruoyi.business.domain.config.TriggerConfig;
import com.ruoyi.business.enums.PoolStatus;
import com.ruoyi.business.enums.TriggerType;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * HTTP 数据调度服务
 * 定期检查 HTTP 数据池状态，触发数据请求
 */
@Service
public class HttpDataSchedulerService {
    
    private static final Logger log = LoggerFactory.getLogger(HttpDataSchedulerService.class);
    
    @Resource
    private IDataPoolService dataPoolService;
    
    @Resource
    private HttpManager httpManager;
    
    /**
     * 定时检查 HTTP 数据池
     * 每5秒执行一次
     */
    @Scheduled(fixedRate = 5000)
    public void scheduledCheckDataPools() {
        try {
            
            // 查询所有运行中的 HTTP 数据池
            DataPool queryParam = new DataPool();
            queryParam.setSourceType("HTTP");
            queryParam.setStatus(PoolStatus.RUNNING.getCode());
            queryParam.setDelFlag("0"); // 未删除
            
            List<DataPool> httpDataPools = dataPoolService.selectDataPoolList(queryParam);
            
            if (httpDataPools.isEmpty()) {
                return;
            }
//            log.debug("[HttpScheduler] 找到 {} 个运行中的 HTTP 数据池", httpDataPools.size());
            
            // 处理每个 HTTP 数据池
            for (DataPool dataPool : httpDataPools) {
                try {
                    processPool(dataPool);
                } catch (Exception e) {
                    log.error("[HttpScheduler] 处理数据池 {} 失败: {}", dataPool.getId(), e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("[HttpScheduler] 检查 HTTP 数据池时发生异常", e);
        }
    }
    
    /**
     * 处理单个 HTTP 数据池
     */
    private void processPool(DataPool dataPool) {
        Long poolId = dataPool.getId();
        
        try {
            // 获取或创建 HTTP 提供者
            HttpProvider provider = httpManager.getOrCreateProvider(poolId);
            
            // 检查是否需要触发请求
            if (shouldTriggerRequest(dataPool, provider)) {
                log.debug("[HttpScheduler] 触发 HTTP 数据请求，数据池ID: {}", poolId);
                provider.requestData();
            } else {
                log.debug("[HttpScheduler] 跳过 HTTP 数据请求，数据池ID: {},待打印数量：{}", poolId,dataPool.getPendingCount());
            }
            
        } catch (Exception e) {
            log.error("[HttpScheduler] 处理 HTTP 数据池 {} 失败: {}", poolId, e.getMessage(), e);
        }
    }
    
    /**
     * 判断是否需要触发请求
     */
    private boolean shouldTriggerRequest(DataPool dataPool, HttpProvider provider) {
        // 如果正在请求中，跳过
        if (provider.isRequestInProgress()) {
            return false;
        }

        String triggerConfigJson = dataPool.getTriggerConfigJson();
        if (StringUtils.isEmpty(triggerConfigJson)) {
            return false;
        }
        TriggerConfig triggerConfig = JSON.parseObject(triggerConfigJson, TriggerConfig.class);
        
        // 检查阈值触发
        Long pendingCount = dataPool.getPendingCount();
        if (pendingCount != null && pendingCount > triggerConfig.getThreshold()) {
            // 如果有待处理数据，不触发新请求
            return false;
        }
        
        // 检查间隔触发（这里可以根据实际需求调整）
        // 目前简单实现：只要没有待处理数据且不在请求中，就触发
        return true;
    }
    
    /**
     * 手动触发 HTTP 数据请求
     */
    public void manualTriggerRequest(Long poolId) {
        try {
            DataPool dataPool = dataPoolService.selectDataPoolById(poolId);
            if (dataPool == null) {
                log.warn("[HttpScheduler] 数据池不存在: {}", poolId);
                return;
            }
            
            if (!"HTTP".equals(dataPool.getSourceType())) {
                log.warn("[HttpScheduler] 数据池类型不是 HTTP: {}", poolId);
                return;
            }
            
            HttpProvider provider = httpManager.getOrCreateProvider(poolId);
            provider.requestData();
            
            log.info("[HttpScheduler] 手动触发 HTTP 数据请求成功，数据池ID: {}", poolId);
            
        } catch (Exception e) {
            log.error("[HttpScheduler] 手动触发 HTTP 数据请求失败，数据池ID: {}", poolId, e);
        }
    }
}

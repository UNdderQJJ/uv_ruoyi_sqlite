package com.ruoyi.business.service.DataPool.type.UDisk;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.business.domain.DataPool.DataPool;
import com.ruoyi.business.domain.config.TriggerConfig;
import com.ruoyi.business.enums.PoolStatus;
import com.ruoyi.business.enums.SourceType;
import com.ruoyi.business.enums.TriggerType;
import com.ruoyi.business.enums.ConnectionState;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.business.service.DataPool.DataPoolSchedulerService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import com.ruoyi.business.domain.config.UDiskSourceConfig;

/**
 * U盘数据调度器服务
 * 定期检查所有U盘类型的数据池，根据阈值触发数据读取
 * 
 * @author ruoyi
 */
@Service
public class UDiskDataSchedulerService {
    private static final Logger log = LoggerFactory.getLogger(UDiskDataSchedulerService.class);

    @Autowired
    private IDataPoolService dataPoolService;
    
    @Autowired
    private UDiskFileReaderService uDiskFileReaderService;

    @Autowired
    @Lazy
    private DataPoolSchedulerService dataPoolSchedulerService;


    // 默认阈值：当待打印数据少于此值时触发读取
    private static final int DEFAULT_THRESHOLD = 100;

    // 默认批次大小：每次最多读取的数据量
    private static final int DEFAULT_BATCH_SIZE = 10;

    // 自动监控调度
    private final ScheduledExecutorService monitorExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "u-disk-monitor");
        t.setDaemon(true);
        return t;
    });
    // 记录文件的最近修改时间，避免重复处理
    private final Map<Long, Long> poolIdToLastModified = new ConcurrentHashMap<>();

    @PostConstruct
    public void startAutoMonitor() {
        // 每3秒扫描一次，识别 autoUpdate=1 的U盘数据池
        monitorExecutor.scheduleWithFixedDelay(this::scanAndHandleAutoUpdatePools, 3, 3, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stopAutoMonitor() {
        monitorExecutor.shutdownNow();
    }
    
    /**
     * 手动触发数据读取
     * 
     * @param poolId 数据池ID
     * @param batchSize 批次大小（可选，为null时使用配置或默认值）
     * @return 读取的数据量
     */
    public String manualTriggerDataReading(Long poolId, Integer batchSize) {
        // 查询数据池
        DataPool dataPool = dataPoolService.selectDataPoolById(poolId);
        if (dataPool == null) {
            log.error("数据池不存在: {}", poolId);
            return "数据池不存在";
        }
        
        // 检查数据池类型
        if (!SourceType.U_DISK.getCode().equals(dataPool.getSourceType())) {
            log.error("数据池类型不是U盘类型: {}", dataPool.getSourceType());
            return "数据池类型不是U盘类型";
        }
        
//        // 检查文件是否已经读取完成
//        if ("1".equals(dataPool.getFileReadCompleted())) {
//            log.info("数据池 {} 的文件已经读取完成，无需再次读取", dataPool.getPoolName());
//            dataPoolService.updateDataPoolStatus(poolId, PoolStatus.WINING.getCode());
//            return "数据池"+dataPool.getPoolName()+"的文件已经读取完成，无需再次读取";
//        }
        
        // 获取批次大小
        int actualBatchSize = batchSize != null ? batchSize : getBatchSizeFromConfig(dataPool);

        //获取阈值
        int threshold =  getThresholdFromConfig(dataPool);

//           // 检查待打印数据量是否低于阈值
//        if (dataPool.getPendingCount() > threshold) {
//            log.debug("数据池 {} 待打印数据量 {} 未低于阈值 {}, 无需读取",
//                    dataPool.getPoolName(), dataPool.getPendingCount(), threshold);
//           return "数据池"+dataPool.getPoolName()+"的待打印数据量"+dataPool.getPendingCount()+"未低于阈值"+threshold;
//        }
        
        log.info("触发数据池 {} 读取数据, 批次大小: {}", dataPool.getPoolName(), actualBatchSize);
        
        // 调用文件读取服务，阈值设为0确保一定会读取
        int readCount = 0;
        try {
            readCount = uDiskFileReaderService.readDataIfBelowThreshold(dataPool, threshold, actualBatchSize);
        } catch (Exception e) {
            //更新数据池为故障
            dataPoolService.updateDataPoolStatus(poolId, PoolStatus.ERROR.getCode());
            // 更新连接状态为断开
            throw new IllegalStateException("文件读取出错"+e.getMessage());
        }
        // 更新连接状态为已连接（文件可读）
        dataPoolService.updateConnectionState(poolId, ConnectionState.CONNECTED.getCode());

        if (readCount > 0) {
            log.info("数据池 {} 成功读取 {} 条数据", dataPool.getPoolName(), readCount);

            return "数据池"+dataPool.getPoolName()+"成功读取"+readCount+"条数据";
        } else {
            log.info("数据池 {} 没有新数据可读取", dataPool.getPoolName());
            // 如果没有数据可读，可能是文件不存在或已读完，更新状态为断开
//            dataPoolService.updateConnectionState(poolId, ConnectionState.DISCONNECTED.getCode());
            return "数据池"+dataPool.getPoolName()+"没有新数据可读取";
        }

    }
    
    /**
     * 定时任务：每5秒检查一次所有U盘类型的数据池
     */
//    @Scheduled(fixedDelay = 5000)
    public void scheduledCheckDataPools() {
        try {
            // 查询所有运行中的U盘类型数据池
            DataPool queryParam = new DataPool();
            queryParam.setSourceType(SourceType.U_DISK.getCode());
            queryParam.setStatus(PoolStatus.RUNNING.getCode()); // 只检查运行中的数据池
            queryParam.setFileReadCompleted("0");// 只检查未读取完成的数据池
            queryParam.setDelFlag("0"); // 未删除
            
            List<DataPool> dataPools = dataPoolService.selectDataPoolList(queryParam);
            if (dataPools == null || dataPools.isEmpty()) {
                return;
            }
            
            // 遍历所有数据池，检查是否需要读取数据
            for (DataPool dataPool : dataPools) {
                try {
                    processDataPool(dataPool);
                } catch (Exception e) {
                    log.error("处理数据池 {} 时发生错误: {}", dataPool.getPoolName(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("调度器执行时发生错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 扫描 autoUpdate=1 的U盘数据池，检测U盘文件是否发生变化（重启或重插引发的变更），自动更新数据。
     */
    private void scanAndHandleAutoUpdatePools() {
        try {
            DataPool queryParam = new DataPool();
            queryParam.setSourceType(SourceType.U_DISK.getCode());
            queryParam.setDelFlag("0");
            queryParam.setAutoUpdate("1");
            List<DataPool> pools = dataPoolService.selectDataPoolList(queryParam);
            log.debug("[UDiskScheduler] 找到 {} 个需要运行的 UDisk 数据池", pools.size());
            if (pools == null || pools.isEmpty()) {
                return;
            }

            for (DataPool p : pools) {
            
                try {
                    handleAutoUpdatePool(p);
                } catch (Exception e) {
                    log.warn("自动监控处理数据池失败，poolId={}, msg={}", p.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("扫描自动监控U盘数据池失败: {}", e.getMessage(), e);
        }
    }

    private void handleAutoUpdatePool(DataPool dataPool) {
        // 解析U盘配置，获取文件路径
        if (StringUtils.isBlank(dataPool.getSourceConfigJson())) {
            return;
        }
        UDiskSourceConfig cfg;
        try {
            cfg = JSON.parseObject(dataPool.getSourceConfigJson(), UDiskSourceConfig.class);
        } catch (Exception ex) {
            log.warn("解析U盘配置失败，poolId={}, err={}", dataPool.getId(), ex.getMessage());
            return;
        }
        String filePath = cfg != null ? cfg.getFilePath() : null;
        if (StringUtils.isBlank(filePath)) {
            return;
        }

       File f = new File(filePath);
        long lastModified = f.exists() ? f.lastModified() : -1L;
        Long prev = poolIdToLastModified.get(dataPool.getId());

        // 首次记录或检测到变化
        if (prev == null || !prev.equals(lastModified)) {
            poolIdToLastModified.put(dataPool.getId(), lastModified);

            // 检测到变化：重置读取完成标志，交由统一调度器按阈值持续读取
            try {
                if (lastModified > 0) {
                    log.info("检测到U盘文件变更，启动统一调度器按阈值持续读取，poolId={}, path={}", dataPool.getId(), filePath);
                    // 重置读取位置与完成标志
                    uDiskFileReaderService.resetReadPosition(dataPool.getId(), null);
                    //更新成运行中
                    dataPoolService.updateDataPoolStatus(dataPool.getId(), PoolStatus.RUNNING.getCode());
                    // 更新连接状态为已连接（文件可读）
                    dataPoolService.updateConnectionState(dataPool.getId(), ConnectionState.CONNECTED.getCode());
                    // 交由动态调度器按阈值持续读取
                    dataPoolSchedulerService.startDataPoolScheduler(dataPool.getId());
                } else {
                    // 文件丢失，标记断开
                    dataPoolService.updateConnectionState(dataPool.getId(), ConnectionState.DISCONNECTED.getCode());
                }
            } catch (Exception e) {
                log.warn("自动读取失败，poolId={}, err={}", dataPool.getId(), e.getMessage());
            }
        }
    }
    
    /**
     * 处理单个数据池
     * 
     * @param dataPool 数据池对象
     */
    private void processDataPool(DataPool dataPool) {
//        log.debug("检查数据池: {}, ID: {}, 待打印数量: {}",
//                dataPool.getPoolName(), dataPool.getId(), dataPool.getPendingCount());
        
        // 从数据池配置中获取阈值和批次大小，如果没有配置则使用默认值
        int threshold = getThresholdFromConfig(dataPool);
        int batchSize = getBatchSizeFromConfig(dataPool);
        
        // 检查是否应该触发数据读取
        if (shouldTriggerDataReading(dataPool, threshold)) {
            log.info("数据池 {} 触发数据读取, 待打印数量: {}, 阈值: {}", 
                    dataPool.getPoolName(), dataPool.getPendingCount(), threshold);
            
            // 调用文件读取服务
            int readCount = uDiskFileReaderService.readDataIfBelowThreshold(dataPool, threshold, batchSize);
            
            if (readCount > 0) {
                log.info("数据池 {} 成功读取 {} 条数据", dataPool.getPoolName(), readCount);
            } else {
//                log.debug("数据池 {} 没有新数据可读取", dataPool.getPoolName());
            }
        }
    }
    
    /**
     * 从数据池配置中获取阈值
     * 
     * @param dataPool 数据池对象
     * @return 阈值
     */
    private int getThresholdFromConfig(DataPool dataPool) {
        TriggerConfig triggerConfig = parseTriggerConfig(dataPool);
        if (triggerConfig != null && triggerConfig.getThreshold() != null) {
            return triggerConfig.getThreshold();
        }
        return DEFAULT_THRESHOLD;
    }
    
    /**
     * 从数据池配置中获取批次大小
     * 
     * @param dataPool 数据池对象
     * @return 批次大小
     */
    private int getBatchSizeFromConfig(DataPool dataPool) {
        TriggerConfig triggerConfig = parseTriggerConfig(dataPool);
        if (triggerConfig != null && triggerConfig.getBatchSize() != null) {
            return triggerConfig.getBatchSize();
        }
        return DEFAULT_BATCH_SIZE;
    }
    
    /**
     * 解析触发配置
     * 
     * @param dataPool 数据池对象
     * @return 触发配置对象
     */
    private TriggerConfig parseTriggerConfig(DataPool dataPool) {
        String triggerConfigJson = dataPool.getTriggerConfigJson();
        if (StringUtils.isEmpty(triggerConfigJson)) {
            return null;
        }
        
        try {
            return JSON.parseObject(triggerConfigJson, TriggerConfig.class);
        } catch (Exception e) {
            log.error("解析触发配置失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 检查是否应该触发数据读取
     * 
     * @param dataPool 数据池对象
     * @param threshold 阈值
     * @return 是否应该触发
     */
    private boolean shouldTriggerDataReading(DataPool dataPool, int threshold) {
        TriggerConfig triggerConfig = parseTriggerConfig(dataPool);
        
        // 如果没有配置，默认使用阈值触发
        if (triggerConfig == null || StringUtils.isEmpty(triggerConfig.getTriggerType())) {
            return dataPool.getPendingCount() < threshold;
        }
        
        // 根据触发类型判断
        String triggerType = triggerConfig.getTriggerType();
        if (TriggerType.THRESHOLD.getCode().equals(triggerType)) {
            // 阈值触发
            return dataPool.getPendingCount() < threshold;
        } else if (TriggerType.INTERVAL.getCode().equals(triggerType)) {
            // 定时触发，由于已经在定时任务中，所以直接返回true
            return true;
        } else if (TriggerType.MANUAL.getCode().equals(triggerType)) {
            // 手动触发，不自动触发
            return false;
        }
        
        return dataPool.getPendingCount() < threshold;
    }
}

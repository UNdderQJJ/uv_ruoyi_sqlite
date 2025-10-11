package com.ruoyi.business.service.DataPool;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.business.domain.DataPool.DataPool;
import com.ruoyi.business.domain.config.TriggerConfig;
import com.ruoyi.business.enums.PoolStatus;
import com.ruoyi.business.enums.TriggerType;
import com.ruoyi.business.enums.ConnectionState;
import com.ruoyi.business.service.DataPool.type.Http.HttpManager;
import com.ruoyi.business.service.DataPool.type.Mqtt.MqttManager;
import com.ruoyi.business.service.DataPool.type.TcpClient.tcp.TcpServerManager;
import com.ruoyi.business.service.DataPool.type.TcpServer.tcp.TcpClientManager;
import com.ruoyi.business.service.DataPool.type.UDisk.UDiskDataSchedulerService;
import com.ruoyi.business.service.DataPool.type.WebSocket.WebSocketManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.context.event.EventListener;
import com.ruoyi.business.events.ConnectionStateChangedEvent;
import org.springframework.stereotype.Service;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 动态数据池调度器服务
 * 为每个数据池创建独立的定时任务，根据配置的时间间隔执行数据获取
 */
@Service
public class DataPoolSchedulerService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataPoolSchedulerService.class);

    @Resource
    private IDataPoolService dataPoolService;
    
    @Resource
    private UDiskDataSchedulerService uDiskDataSchedulerService;
    
    @Resource
    private TcpClientManager tcpClientManager;
    
    @Resource
    private TcpServerManager tcpServerManager;
    
    @Resource

    private HttpManager httpManager;
    
    @Resource
    private MqttManager mqttManager;
    
    @Resource
    private WebSocketManager webSocketManager;
    
    @Resource
    private TaskScheduler taskScheduler;

    // 记录每个数据池的定时任务
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    
    // 记录每个数据池的执行锁，确保串行执行
    private final ConcurrentHashMap<Long, ReentrantLock> poolLocks = new ConcurrentHashMap<>();
    
    // 记录正在执行的任务，防止重复执行
    private final ConcurrentHashMap<Long, Boolean> executingTasks = new ConcurrentHashMap<>();
    
    // 记录每个数据池的上次pendingCount，用于判断是否跳过执行
    private final ConcurrentHashMap<Long, Long> lastPendingCounts = new ConcurrentHashMap<>();

    // 默认时间间隔（毫秒）
    private static final long DEFAULT_INTERVAL = 5000L;

    /**
     * 启动数据池的定时任务
     */
    public void startDataPoolScheduler(Long poolId) {
        try {
            DataPool dataPool = dataPoolService.selectDataPoolById(poolId);
            if (dataPool == null) {
                log.warn(" 数据池不存在: {}", poolId);
                return;
            }

            // 检查是否已经存在定时任务，如果存在则不重复创建
            if (scheduledTasks.containsKey(poolId)) {
                ScheduledFuture<?> existingTask = scheduledTasks.get(poolId);
                if (existingTask != null && !existingTask.isCancelled()) {
                    log.debug("数据池定时任务已存在，跳过创建: poolId={}", poolId);
                    return;
                }
            }

            // 如果已经有定时任务在运行，先停止
            stopDataPoolScheduler(poolId);

            // 获取配置的时间间隔
            long interval = getDataFetchInterval(dataPool);
            
            // 创建定时任务，使用匿名内部类避免Lambda序列化问题
            ScheduledFuture<?> task = taskScheduler.scheduleWithFixedDelay(
                    () -> processDataPoolWithLock(dataPool),
                    Duration.ofMillis(interval)
            );
            
            scheduledTasks.put(poolId, task);
            log.info("启动数据池定时任务: poolId={}, interval={}ms", poolId, interval);
            
        } catch (Exception e) {
            log.error(" 启动数据池定时任务失败: poolId={}", poolId, e);
        }
    }

    /**
     * 启动数据池并启动定时任务
     * 供外部调用，用于启动数据池时直接启动定时任务
     */
    public void startDataPoolWithScheduler(Long poolId) {
        try {
            log.info("启动数据池并启动定时任务: poolId={}", poolId);
            
            // 先启动定时任务
            //睡眠2秒
            Thread.sleep(2000);
            startDataPoolScheduler(poolId);
            
            // 记录启动日志
            log.info("数据池启动完成: poolId={}", poolId);
            
        } catch (Exception e) {
            log.error("启动数据池失败: poolId={}", poolId, e);
        }
    }

    /**
     * 根据连接状态管理定时任务
     * 当连接断开时停止定时任务，连接成功时启动定时任务
     */
    public void manageSchedulerByConnectionState(Long poolId, ConnectionState connectionState) {
        try {
            DataPool dataPool = dataPoolService.selectDataPoolById(poolId);
            if (dataPool == null) {
                log.warn("[DataPoolScheduler] 数据池不存在: {}", poolId);
                return;
            }

            // 检查数据池是否处于运行状态
            if (!PoolStatus.RUNNING.getCode().equals(dataPool.getStatus())) {
                log.debug("[DataPoolScheduler] 数据池未运行，不管理定时任务: poolId={}", poolId);
                return;
            }

            switch (connectionState) {
                case CONNECTED:
                case LISTENING:
                    // 连接成功或监听中，启动定时任务
                    if (!scheduledTasks.containsKey(poolId)) {
                        startDataPoolScheduler(poolId);
                        log.info("[DataPoolScheduler] 连接成功，启动定时任务: poolId={}", poolId);
                    }
                    break;
                    
                case DISCONNECTED:
                case CONNECTING:
                case ERROR:
                    // 连接断开、正在连接或错误状态，停止定时任务
                    if (scheduledTasks.containsKey(poolId)) {
                        stopDataPoolScheduler(poolId);
                        log.info("[DataPoolScheduler] 连接断开，停止定时任务: poolId={}", poolId);
                    }
                    break;
            }
        } catch (Exception e) {
            log.error("[DataPoolScheduler] 根据连接状态管理定时任务失败: poolId={}, state={}", poolId, connectionState, e);
        }
    }

    /**
     * 监听连接状态变更事件
     */
    @EventListener
    public void onConnectionStateChanged(ConnectionStateChangedEvent event) {
        manageSchedulerByConnectionState(event.getPoolId(), event.getConnectionState());
    }

    /**
     * 停止数据池的定时任务
     */
    public void stopDataPoolScheduler(Long poolId) {
        try {
            ScheduledFuture<?> task = scheduledTasks.remove(poolId);
            if (task != null && !task.isCancelled()) {
                task.cancel(false);
                log.info("[DataPoolScheduler] 停止数据池定时任务: poolId={}", poolId);
            }
            
            // 清理执行标记
            executingTasks.remove(poolId);
            
            // 清理锁（可选，因为锁会在GC时自动清理）
            poolLocks.remove(poolId);
            
            // 清理pendingCount历史记录
            lastPendingCounts.remove(poolId);
            
        } catch (Exception e) {
            log.error("[DataPoolScheduler] 停止数据池定时任务失败: poolId={}", poolId, e);
        }
    }

    /**
     * 带锁处理单个数据池，确保串行执行
     */
    private void processDataPoolWithLock(@NotNull DataPool pool) {
        Long poolId = pool.getId();
        ReentrantLock lock = poolLocks.computeIfAbsent(poolId, k -> new ReentrantLock());
        
        // 检查是否已经在执行，防止重复执行
        if (executingTasks.putIfAbsent(poolId, true) != null) {
            log.debug("[DataPoolScheduler] 数据池正在执行中，跳过本次执行: poolId={}", poolId);
            return;
        }
        
        try {
            // 尝试获取锁，如果获取不到则跳过本次执行
            if (lock.tryLock()) {
                try {
                    // 在锁保护下重新获取最新的数据池信息，确保数据一致性
                    DataPool latestPool = dataPoolService.selectDataPoolById(poolId);
                    if (latestPool == null) {
                        log.debug("[DataPoolScheduler] 数据池不存在，停止定时任务: poolId={}", poolId);
                        stopDataPoolScheduler(poolId);
                        return;
                    }
                    
                    // 检查数据池状态
                    if (!PoolStatus.RUNNING.getCode().equals(latestPool.getStatus())) {
                        log.debug("[DataPoolScheduler] 数据池已停止，停止定时任务: poolId={}", poolId);
                        stopDataPoolScheduler(poolId);
                        return;
                    }
                    
                    // 使用最新的数据池信息进行处理
                    processDataPool(latestPool);
                } finally {
                    lock.unlock();
                }
            } else {
                log.debug("[DataPoolScheduler] 数据池正在执行中，跳过本次执行: poolId={}", poolId);
            }
        } catch (Exception e) {
            log.error("[DataPoolScheduler] 处理数据池失败: poolId={}", poolId, e);
        } finally {
            // 清除执行标记
            executingTasks.remove(poolId);
        }
    }

    /**
     * 处理单个数据池
     */
    private void processDataPool(DataPool pool) {
        try {
            // 检查数据池是否仍然处于运行状态
            DataPool currentPool = dataPoolService.selectDataPoolById(pool.getId());
            if (currentPool == null || !PoolStatus.RUNNING.getCode().equals(currentPool.getStatus())) {
                log.debug(" 数据池已停止或删除，停止定时任务: poolId={}", pool.getId());
                stopDataPoolScheduler(pool.getId());
                return;
            }

            String sourceType = pool.getSourceType();
            log.debug(" 执行数据池定时任务: poolId={}, sourceType={}", pool.getId(), sourceType);
            
            switch (sourceType) {
                case "U_DISK":
                    processUDiskPool(pool);
                    break;
                case "TCP_SERVER":
                    processTcpServerPool(pool);
                    break;
                case "TCP_CLIENT":
                    processTcpClientPool(pool);
                    break;
                case "HTTP":
                    processHttpPool(pool);
                    break;
                case "MQTT":
                    processMqttPool(pool);
                    break;
                case "WEBSOCKET":
                    processWebSocketPool(pool);
                    break;
                default:
                    log.warn("[DataPoolScheduler] 不支持的数据源类型: {}", sourceType);
            }
        } catch (Exception e) {
            log.error("[DataPoolScheduler] 处理数据池失败: poolId={}", pool.getId(), e);
        }
    }

    /**
     * 处理U盘数据池
     */
    private void processUDiskPool(DataPool pool) {
        DataPool dataPool = dataPoolService.selectDataPoolById(pool.getId());

        // 更新连接状态为已连接（文件可读）
        dataPoolService.updateConnectionState(dataPool.getId(), ConnectionState.CONNECTED.getCode());

        // 检查文件是否已经读取完成
        if ("1".equals(dataPool.getFileReadCompleted())) {
            return;
        }
        // 获取触发配置
        TriggerConfig trigger = parseTrigger(dataPool);
        int threshold = getThreshold(trigger);

        // 在锁保护下重新获取最新的待打印数量，避免脏数据
        long pendingCount = dataPoolService.selectDataPoolById(pool.getId()).getPendingCount();
        
        // 检查pendingCount是否与上次相同，如果相同则跳过执行
        Long lastPendingCount = lastPendingCounts.get(pool.getId());
        if (lastPendingCount != null && lastPendingCount.equals(pendingCount)) {
            return;
        }
        
        // 更新上次pendingCount记录
        lastPendingCounts.put(pool.getId(), pendingCount);
        
        // 检查是否需要触发读取
        if (pendingCount >= threshold) {
            return;
        }
        
        // 执行前再次验证pendingCount，确保数据一致性
        DataPool latestDataPool = dataPoolService.selectDataPoolById(pool.getId());
        if (latestDataPool.getPendingCount() >= threshold) {
            return;
        }
        uDiskDataSchedulerService.manualTriggerDataReading(pool.getId(), null);
    }

    /**
     * 处理TCP服务端数据池
     */
    private void processTcpServerPool(DataPool pool) {
        DataPool dataPool = dataPoolService.selectDataPoolById(pool.getId());
        TriggerConfig trigger = parseTrigger(dataPool);
        int threshold = getThreshold(trigger);
        
        // 连接保障
        tcpClientManager.getOrCreateProvider(dataPool.getId()).ensureConnected();
        
        // 判断是否触发请求
        if (shouldTrigger(dataPool, trigger, threshold)) {
            // 执行前最终验证pendingCount
            if (finalValidatePendingCount(dataPool.getId(), threshold)) {
                tcpClientManager.getOrCreateProvider(dataPool.getId()).requestDataIfConnected();
            }
        }
    }

    /**
     * 处理TCP客户端数据池
     */
    private void processTcpClientPool(DataPool pool) {
        DataPool dataPool = dataPoolService.selectDataPoolById(pool.getId());
        TriggerConfig trigger = parseTrigger(dataPool);
        int threshold = getThreshold(trigger);
        
        // 确保监听已启动
        tcpServerManager.getOrCreateProvider(dataPool.getId());
        
        // 判断是否触发请求
        if (shouldTrigger(dataPool, trigger, threshold)) {
            // 执行前最终验证pendingCount
            if (finalValidatePendingCount(dataPool.getId(), threshold)) {
                tcpServerManager.getOrCreateProvider(dataPool.getId()).requestData();
            }
        }
    }

    /**
     * 处理HTTP数据池
     */
    private void processHttpPool(DataPool pool) {
        DataPool dataPool = dataPoolService.selectDataPoolById(pool.getId());
        // 设置连接状态为连接中
        dataPoolService.updateConnectionState(pool.getId(), ConnectionState.CONNECTING.getCode());
        TriggerConfig trigger = parseTrigger(dataPool);
        int threshold = getThreshold(trigger);
        
        // 判断是否触发请求
        if (shouldTrigger(dataPool, trigger, threshold)) {
            // 执行前最终验证pendingCount
            if (finalValidatePendingCount(dataPool.getId(), threshold)) {
                httpManager.getOrCreateProvider(dataPool.getId()).requestData();
            }
        }
    }

    /**
     * 处理MQTT数据池
     */
    private void processMqttPool(DataPool pool) {
        DataPool dataPool = dataPoolService.selectDataPoolById(pool.getId());
        TriggerConfig trigger = parseTrigger(dataPool);
        int threshold = getThreshold(trigger);
        
        // 确保连接已建立
        mqttManager.getOrCreateProvider(dataPool.getId());
        
        // 判断是否触发请求
        if (shouldTrigger(dataPool, trigger, threshold)) {
            // 执行前最终验证pendingCount
            if (finalValidatePendingCount(dataPool.getId(), threshold)) {
                mqttManager.getOrCreateProvider(dataPool.getId()).publishMessage();
            }
        }
    }

    /**
     * 处理WebSocket数据池
     */
    private void processWebSocketPool(DataPool pool) {
        DataPool dataPool = dataPoolService.selectDataPoolById(pool.getId());
        TriggerConfig trigger = parseTrigger(dataPool);
        int threshold = getThreshold(trigger);
        
        // 确保连接已建立
        webSocketManager.getOrCreateProvider(dataPool.getId());
        
        // 判断是否触发请求
        if (shouldTrigger(dataPool, trigger, threshold)) {
            // 执行前最终验证pendingCount
            if (finalValidatePendingCount(dataPool.getId(), threshold)) {
                webSocketManager.getOrCreateProvider(dataPool.getId()).sendMessage();
            }
        }
    }

    /**
     * 获取数据获取间隔时间
     */
    private long getDataFetchInterval(DataPool pool) {
        Long interval = pool.getDataFetchInterval();
        return interval != null ? interval : DEFAULT_INTERVAL;
    }

    /**
     * 获取阈值
     */
    private int getThreshold(TriggerConfig trigger) {
        return trigger != null && trigger.getThreshold() != null ? trigger.getThreshold() : 100;
    }

    /**
     * 判断是否应该触发
     * 在锁保护下重新获取最新数据，避免脏数据问题
     */
    private boolean shouldTrigger(DataPool pool, TriggerConfig trigger, int threshold) {
        // 重新获取最新的数据池信息，避免脏数据
        DataPool latestPool = dataPoolService.selectDataPoolById(pool.getId());
        if (latestPool == null) {
            return false;
        }
        
        long currentPendingCount = latestPool.getPendingCount();
        
        // 检查pendingCount是否与上次相同，如果相同则跳过执行
        Long lastPendingCount = lastPendingCounts.get(pool.getId());
        if (lastPendingCount != null && lastPendingCount.equals(currentPendingCount)) {
            return false;
        }
        
        // 更新上次pendingCount记录
        lastPendingCounts.put(pool.getId(), currentPendingCount);
        
        if (trigger == null || StringUtils.isBlank(trigger.getTriggerType())) {
            return currentPendingCount < threshold;
        }
        if (TriggerType.THRESHOLD.getCode().equals(trigger.getTriggerType())) {
            return currentPendingCount < threshold;
        }
        if (TriggerType.INTERVAL.getCode().equals(trigger.getTriggerType())) {
            return true;
        }
        return false; // MANUAL 或未知类型
    }
    
    /**
     * 执行前最终验证pendingCount，确保数据一致性
     */
    private boolean finalValidatePendingCount(Long poolId, int threshold) {
        DataPool latestPool = dataPoolService.selectDataPoolById(poolId);
        if (latestPool == null) {
            return false;
        }
        
        long currentPendingCount = latestPool.getPendingCount();
        if (currentPendingCount >= threshold) {
            return false;
        }
        
        return true;
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
            log.error("解析触发配置失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 清理所有资源
     */
    public void cleanupAllResources() {
        try {
            // 停止所有定时任务
            for (Long poolId : scheduledTasks.keySet()) {
                stopDataPoolScheduler(poolId);
            }
            
            // 清理所有集合
            scheduledTasks.clear();
            poolLocks.clear();
            executingTasks.clear();
            lastPendingCounts.clear();
            
            log.info("[DataPoolScheduler] 清理所有资源完成");
        } catch (Exception e) {
            log.error("[DataPoolScheduler] 清理所有资源失败", e);
        }
    }

    /**
     * 清理已删除数据池的定时任务
     */
    public void cleanupExecuteTimeRecord(Long poolId) {
        stopDataPoolScheduler(poolId);
    }
    
    /**
     * 清除指定数据池的pendingCount历史记录
     * 当阈值配置发生变化时调用此方法
     */
    public void clearPendingCountHistory(Long poolId) {
        if (poolId != null) {
            lastPendingCounts.remove(poolId);
            log.info("[DataPoolScheduler] 已清除数据池pendingCount历史记录: poolId={}", poolId);
        }
    }

    /**
     * 获取当前运行的定时任务数量
     */
    public int getActiveTaskCount() {
        return scheduledTasks.size();
    }

    /**
     * 获取所有活跃的数据池ID
     */
    public java.util.Set<Long> getActivePoolIds() {
        return scheduledTasks.keySet();
    }
    
    /**
     * 检查指定数据池的定时任务状态
     */
    public boolean isDataPoolSchedulerActive(Long poolId) {
        ScheduledFuture<?> task = scheduledTasks.get(poolId);
        return task != null && !task.isCancelled();
    }
    
    /**
     * 获取定时任务详细信息
     */
    public String getDataPoolSchedulerInfo(Long poolId) {
        ScheduledFuture<?> task = scheduledTasks.get(poolId);
        if (task == null) {
            return "未启动";
        }
        if (task.isCancelled()) {
            return "已取消";
        }
        if (task.isDone()) {
            return "已完成";
        }
        return "运行中";
    }

    /**
     * 应用启动时恢复所有运行中数据池的定时任务
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
//        try {
//            log.info("[DataPoolScheduler] 开始恢复运行中数据池的定时任务...");
//
//            // 查询所有运行中的数据池
//            DataPool query = new DataPool();
//            query.setStatus(PoolStatus.RUNNING.getCode());
//            query.setDelFlag("0"); // 未删除
//
//            List<DataPool> pools = dataPoolService.selectDataPoolList(query);
//            if (pools == null || pools.isEmpty()) {
//                log.info("[DataPoolScheduler] 没有运行中的数据池需要恢复定时任务");
//                return;
//            }
//
//            for (DataPool pool : pools) {
//                try {
//                    // 仅在已连接时恢复任务，避免未连接时盲目触发
//                    if (ConnectionState.CONNECTED.getCode().equals(pool.getConnectionState())) {
//                        startDataPoolScheduler(pool.getId());
//                    } else {
//                        log.debug("[DataPoolScheduler] 跳过恢复未连接的数据池: poolId={}, state={}", pool.getId(), pool.getConnectionState());
//                    }
//                } catch (Exception e) {
//                    log.error("[DataPoolScheduler] 恢复数据池定时任务失败: poolId={}", pool.getId(), e);
//                }
//            }
//
//            log.info("[DataPoolScheduler] 成功恢复 {} 个数据池的定时任务", pools.size());
//        } catch (Exception e) {
//            log.error("[DataPoolScheduler] 恢复数据池定时任务时发生异常", e);
//        }
    }
}

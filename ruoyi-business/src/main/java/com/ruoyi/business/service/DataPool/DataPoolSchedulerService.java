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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.context.event.EventListener;
import com.ruoyi.business.events.ConnectionStateChangedEvent;
import org.springframework.stereotype.Service;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

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

            // 如果已经有定时任务在运行，先停止
            stopDataPoolScheduler(poolId);

            // 获取配置的时间间隔
            long interval = getDataFetchInterval(dataPool);
            
            // 创建定时任务，使用匿名内部类避免Lambda序列化问题
            ScheduledFuture<?> task = taskScheduler.scheduleWithFixedDelay(
                    new Runnable() {
                        @Override
                        public void run() {
                            processDataPool(dataPool);
                        }
                    },
                    interval
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
        } catch (Exception e) {
            log.error("[DataPoolScheduler] 停止数据池定时任务失败: poolId={}", poolId, e);
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
        // 检查文件是否已经读取完成
        if ("1".equals(dataPool.getFileReadCompleted())) {
            return;
        }

        // 获取触发配置
        TriggerConfig trigger = parseTrigger(dataPool);
        int threshold = getThreshold(trigger);
        
        // 检查是否需要触发读取
        if (dataPool.getPendingCount() < threshold) {
            uDiskDataSchedulerService.manualTriggerDataReading(pool.getId(), null);
        }
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
            tcpClientManager.getOrCreateProvider(dataPool.getId()).requestDataIfConnected();
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
            tcpServerManager.getOrCreateProvider(dataPool.getId()).requestData();
        }
    }

    /**
     * 处理HTTP数据池
     */
    private void processHttpPool(DataPool pool) {
        DataPool dataPool = dataPoolService.selectDataPoolById(pool.getId());
        TriggerConfig trigger = parseTrigger(dataPool);
        int threshold = getThreshold(trigger);
        
        // 判断是否触发请求
        if (shouldTrigger(dataPool, trigger, threshold)) {
            httpManager.getOrCreateProvider(dataPool.getId()).requestData();
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
            mqttManager.getOrCreateProvider(dataPool.getId()).publishMessage();
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
            webSocketManager.getOrCreateProvider(dataPool.getId()).sendMessage();
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
     */
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
     * 清理已删除数据池的定时任务
     */
    public void cleanupExecuteTimeRecord(Long poolId) {
        stopDataPoolScheduler(poolId);
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
        try {
            log.info("[DataPoolScheduler] 开始恢复运行中数据池的定时任务...");
            
            // 查询所有运行中的数据池
            DataPool query = new DataPool();
            query.setStatus(PoolStatus.RUNNING.getCode());
            query.setDelFlag("0"); // 未删除

            List<DataPool> pools = dataPoolService.selectDataPoolList(query);
            if (pools == null || pools.isEmpty()) {
                log.info("[DataPoolScheduler] 没有运行中的数据池需要恢复定时任务");
                return;
            }

            for (DataPool pool : pools) {
                try {
                    // 仅在已连接时恢复任务，避免未连接时盲目触发
                    if (ConnectionState.CONNECTED.getCode().equals(pool.getConnectionState())) {
                        startDataPoolScheduler(pool.getId());
                    } else {
                        log.debug("[DataPoolScheduler] 跳过恢复未连接的数据池: poolId={}, state={}", pool.getId(), pool.getConnectionState());
                    }
                } catch (Exception e) {
                    log.error("[DataPoolScheduler] 恢复数据池定时任务失败: poolId={}", pool.getId(), e);
                }
            }
            
            log.info("[DataPoolScheduler] 成功恢复 {} 个数据池的定时任务", pools.size());
        } catch (Exception e) {
            log.error("[DataPoolScheduler] 恢复数据池定时任务时发生异常", e);
        }
    }
}

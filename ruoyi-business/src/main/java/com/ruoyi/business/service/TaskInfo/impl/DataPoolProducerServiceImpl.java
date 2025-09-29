package com.ruoyi.business.service.TaskInfo.impl;

import com.ruoyi.business.domain.SystemLog.SystemLog;
import com.ruoyi.business.enums.SystemLogLevel;
import com.ruoyi.business.enums.SystemLogType;
import com.ruoyi.business.events.TaskPauseEvent;
import com.ruoyi.business.service.DataPoolTemplate.IDataPoolTemplateService;
import com.ruoyi.business.service.DeviceFileConfig.IDeviceFileConfigService;
import com.ruoyi.business.service.SystemLog.ISystemLogService;
import com.ruoyi.business.service.TaskInfo.DataPoolProducerService;
import com.ruoyi.business.service.TaskInfo.CommandQueueService;
import com.ruoyi.business.service.TaskInfo.runner.DataPoolProducerRunner;
import com.ruoyi.business.service.DataPoolItem.IDataPoolItemService;
import com.ruoyi.business.service.TaskInfo.ITaskDeviceLinkService;
import com.ruoyi.business.config.TaskDispatchProperties;
import com.ruoyi.business.events.TaskStartEvent;
import com.ruoyi.business.events.TaskStopEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * 数据生产服务实现
 * 负责管理和协调数据生成池运行器
 */
@Service
public class DataPoolProducerServiceImpl implements DataPoolProducerService {
    
    private static final Logger log = LoggerFactory.getLogger(DataPoolProducerServiceImpl.class);
    
    // 存储运行中的任务和对应的Runner
    private final ConcurrentHashMap<Long, DataPoolProducerRunner> runningRunners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Future<?>> runningFutures = new ConcurrentHashMap<>();
    
    @Autowired
    private CommandQueueService commandQueueService;
    
    @Autowired
    private IDataPoolItemService dataPoolItemService;
    
    @Autowired
    private ITaskDeviceLinkService taskDeviceLinkService;
    
    @Autowired
    private ThreadPoolTaskExecutor taskProducerExecutor;

    @Autowired
    private IDataPoolTemplateService iDataPoolTemplateService;

    @Autowired
    private IDeviceFileConfigService iDeviceFileConfigService;
    
    @Autowired
    private TaskDispatchProperties taskDispatchProperties;

    @Autowired
    private ISystemLogService systemLogService;
    
    @Override
    public void startProduction(Long taskId, Long poolId) {
        try {
            log.info("启动数据生产，任务ID: {}, 数据池ID: {}", taskId, poolId);
            
            // 检查是否已经在运行
            if (runningRunners.containsKey(taskId)) {
                log.warn("数据生产已在运行，任务ID: {}", taskId);
                return;
            }
            
            // 创建并启动Runner
            // 读取启动时的计划快照，绑定到本任务的Runner，避免不同任务混用
            int planPrintCountSnapshot = taskDispatchProperties.getPlanPrintCount();
            int originalCountSnapshot = taskDispatchProperties.getOriginalCount();

            DataPoolProducerRunner runner = new DataPoolProducerRunner(
                    taskId, poolId, commandQueueService, dataPoolItemService, taskDeviceLinkService,
                    iDataPoolTemplateService, iDeviceFileConfigService, taskDispatchProperties,
                    planPrintCountSnapshot, originalCountSnapshot, systemLogService);

            // 创建Future
            Future<?> future = taskProducerExecutor.submit(runner);
            
            // 存储Runner和Future
            runningRunners.put(taskId, runner);
            runningFutures.put(taskId, future);
            
            log.info("数据生产启动成功，任务ID: {}", taskId);

             //记录打印日志
            SystemLog systemLog = new SystemLog();
            systemLog.setLogType(SystemLogType.PRINT.getCode());
            systemLog.setLogLevel(SystemLogLevel.INFO.getCode());
            systemLog.setTaskId(taskId);
            systemLog.setPoolId(poolId);
            systemLog.setContent("指令生产启动成功！");
            systemLogService.insert(systemLog);
            
        } catch (Exception e) {
            log.error("启动数据生产失败，任务ID: {}", taskId, e);
             //记录打印日志
            SystemLog systemLog = new SystemLog();
            systemLog.setLogType(SystemLogType.PRINT.getCode());
            systemLog.setLogLevel(SystemLogLevel.ERROR.getCode());
            systemLog.setTaskId(taskId);
            systemLog.setPoolId(poolId);
            systemLog.setContent("启动数据生产失败："+e.getMessage());
            systemLogService.insert(systemLog);
            // 清理资源
            runningRunners.remove(taskId);
            runningFutures.remove(taskId);
        }
    }
    
    @Override
    public void stopProduction(Long taskId) {
        try {
            log.info("停止数据生产，任务ID: {}", taskId);
            
            // 停止Runner
            DataPoolProducerRunner runner = runningRunners.get(taskId);
            if (runner != null) {
                runner.stop();
            }
            
            // 取消Future
            Future<?> future = runningFutures.get(taskId);
            if (future != null) {
                future.cancel(true);
            }
            
            // 清理资源
            runningRunners.remove(taskId);
            runningFutures.remove(taskId);
            
            log.info("数据生产停止成功，任务ID: {}", taskId);
            //记录打印日志
            SystemLog systemLog = new SystemLog();
            systemLog.setLogType(SystemLogType.PRINT.getCode());
            systemLog.setLogLevel(SystemLogLevel.INFO.getCode());
            systemLog.setTaskId(taskId);
            systemLog.setContent("指令生产停止成功！");
            systemLogService.insert(systemLog);

            
        } catch (Exception e) {
            log.error("停止数据生产失败，任务ID: {}", taskId, e);
            //记录打印日志
            SystemLog systemLog = new SystemLog();
            systemLog.setLogType(SystemLogType.PRINT.getCode());
            systemLog.setLogLevel(SystemLogLevel.ERROR.getCode());
            systemLog.setTaskId(taskId);
            systemLog.setContent("指令生产停止失败："+e.getMessage());
            systemLogService.insert(systemLog);
        }
    }
    
    @Override
    public boolean isProducing(Long taskId) {
        DataPoolProducerRunner runner = runningRunners.get(taskId);
        return runner != null && runner.isRunning();
    }
    
    @Override
    public int getProducedCount(Long taskId) {
        DataPoolProducerRunner runner = runningRunners.get(taskId);
        return runner != null ? runner.getProducedCount() : 0;
    }
    
    @Override
    public Set<Long> getRunningTasks() {
        return runningRunners.keySet();
    }
    
    @Override
    public void pauseProduction(Long taskId) {
        try {
            log.info("暂停数据生产，任务ID: {}", taskId);
            
            DataPoolProducerRunner runner = runningRunners.get(taskId);
            if (runner != null) {
                runner.pause();
            }
            
            log.info("数据生产暂停成功，任务ID: {}", taskId);
            
        } catch (Exception e) {
            log.error("暂停数据生产失败，任务ID: {}", taskId, e);
        }
    }
    
    @Override
    public void resumeProduction(Long taskId) {
        try {
            log.info("恢复数据生产，任务ID: {}", taskId);
            
            DataPoolProducerRunner runner = runningRunners.get(taskId);
            if (runner != null) {
                runner.resume();
            }
            
            log.info("数据生产恢复成功，任务ID: {}", taskId);
            
        } catch (Exception e) {
            log.error("恢复数据生产失败，任务ID: {}", taskId, e);
        }
    }
    
    @Override
    public Map<String, Object> getProductionStatistics(Long taskId) {
        Map<String, Object> statistics = new java.util.HashMap<>();
        
        DataPoolProducerRunner runner = runningRunners.get(taskId);
        if (runner != null) {
            statistics.put("taskId", taskId);
            statistics.put("isRunning", runner.isRunning());
            statistics.put("isPaused", runner.isPaused());
            statistics.put("producedCount", runner.getProducedCount());
            statistics.put("processedCount", runner.getProcessedCount());
        } else {
            statistics.put("taskId", taskId);
            statistics.put("isRunning", false);
            statistics.put("isPaused", false);
            statistics.put("producedCount", 0);
            statistics.put("processedCount", 0);
        }
        
        return statistics;
    }
    
    /**
     * 监听任务启动事件
     */
    @EventListener
    public void handleTaskStartEvent(TaskStartEvent event) {
        try {
            log.info("收到任务启动事件，任务ID: {}", event.getTaskId());
            startProduction(event.getTaskId(), event.getRequest().getPoolId());
        } catch (Exception e) {
            log.error("处理任务启动事件异常，任务ID: {}", event.getTaskId(), e);
        }
    }

    /**
     * 监听任务暂停事件
     */
    @EventListener
    public void handleTaskPauseEvent(TaskPauseEvent event) {
        try {
            log.info("收到任务暂停事件，任务ID: {}", event.getTaskId());
            pauseProduction(event.getTaskId());
        } catch (Exception e) {
            log.error("处理任务暂停事件异常，任务ID: {}", event.getTaskId(), e);
        }
    }

    /**
     * 监听任务停止事件
     */
    @EventListener
    public void handleTaskStopEvent(TaskStopEvent event) {
        try {
            log.info("收到任务停止事件，任务ID: {}", event.getTaskId());
            stopProduction(event.getTaskId());
        } catch (Exception e) {
            log.error("处理任务停止事件异常，任务ID: {}", event.getTaskId(), e);
        }
    }
}

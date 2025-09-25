package com.ruoyi.business.service.TaskInfo.impl;

import com.ruoyi.business.domain.DataInspect.DataInspect;
import com.ruoyi.business.domain.SystemLog.SystemLog;
import com.ruoyi.business.domain.TaskInfo.PrintCommand;
import com.ruoyi.business.domain.TaskInfo.TaskDispatchStatus;
import com.ruoyi.business.domain.TaskInfo.TaskInfo;
import com.ruoyi.business.enums.InspectStatus;
import com.ruoyi.business.enums.ItemStatus;
import com.ruoyi.business.enums.SystemLogLevel;
import com.ruoyi.business.enums.SystemLogType;
import com.ruoyi.business.events.TaskPauseEvent;
import com.ruoyi.business.domain.TaskInfo.TaskDeviceLink;
import com.ruoyi.business.service.DataInspect.IDataInspectService;
import com.ruoyi.business.service.SystemLog.ISystemLogService;
import com.ruoyi.business.service.TaskInfo.*;
import com.ruoyi.business.domain.DeviceInfo.DeviceInfo;
import com.ruoyi.business.service.DeviceInfo.IDeviceInfoService;
import com.ruoyi.business.service.DeviceInfo.DeviceCommandService;
import com.ruoyi.business.service.TaskInfo.runner.CommandSenderRunner;
import com.ruoyi.business.service.DataPoolItem.IDataPoolItemService;
import com.ruoyi.business.domain.DataPoolItem.DataPoolItem;
import com.ruoyi.business.events.TaskStartEvent;
import com.ruoyi.business.events.TaskStopEvent;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

/**
 * 指令发送服务实现
 * 负责管理和协调指令发送运行器
 */
@Service
public class CommandSenderServiceImpl implements CommandSenderService {
    
    private static final Logger log = LoggerFactory.getLogger(CommandSenderServiceImpl.class);
    
    // 存储运行中的任务和对应的Runner
    private final ConcurrentHashMap<Long, CommandSenderRunner> runningRunners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Future<?>> runningFutures = new ConcurrentHashMap<>();
    
    @Autowired
    private TaskDispatcherService dispatcher;
    
    @Autowired
    private DeviceDataHandlerService deviceDataHandlerService;
    
    @Autowired
    private CommandQueueService commandQueueService;
    
    @Autowired
    private ThreadPoolTaskExecutor taskSenderExecutor;

    @Autowired
    private ITaskDeviceLinkService taskDeviceLinkService;

    @Autowired
    private IDeviceInfoService deviceInfoService;

    @Autowired
    private ISystemLogService systemLogService;

    // 任务队列维护与状态标记调度句柄
    private final ConcurrentHashMap<Long,ScheduledFuture<?>> queueMaintainers = new ConcurrentHashMap<>();
    
    @Override
    public void startSending(Long taskId) {
        try {
            log.info("启动指令发送，任务ID: {}", taskId);
            
            // 检查是否已经在运行
            if (runningRunners.containsKey(taskId)) {
                log.warn("指令发送已在运行，任务ID: {}", taskId);
                return;
            }
            // 向参与设备发送开始加工指令（start:）
            try {
                TaskDeviceLink query = new TaskDeviceLink();
                query.setTaskId(taskId);
                List<TaskDeviceLink> links = taskDeviceLinkService.list(query);
                if (links != null) {
                    for (TaskDeviceLink link : links) {
                        DeviceInfo d = deviceInfoService.selectDeviceInfoById(link.getDeviceId());
                        if (d != null) {
                            String startCmd = "start:";
                            boolean success = dispatcher.sendCommandToDevice(d.getId().toString(), startCmd);
                            if (success) {
                                log.info("已发送开始指令至设备，设备ID: {}, IP: {}, 端口: {}", d.getId(), d.getIpAddress(), d.getPort());
                                //记录打印日志
                                SystemLog systemLog = new SystemLog();
                                systemLog.setLogType(SystemLogType.PRINT.getCode());
                                systemLog.setLogLevel(SystemLogLevel.INFO.getCode());
                                systemLog.setTaskId(taskId);
                                systemLog.setDeviceId(link.getDeviceId());
                                systemLog.setContent("已下发开始指令至设备:"+startCmd);
                                systemLogService.insert(systemLog);
                            } else {
                                log.warn("发送开始指令失败，设备ID: {}, IP: {}, 端口: {}", d.getId(), d.getIpAddress(), d.getPort());
                                 //记录打印日志
                                SystemLog systemLog = new SystemLog();
                                systemLog.setLogType(SystemLogType.PRINT.getCode());
                                systemLog.setLogLevel(SystemLogLevel.ERROR.getCode());
                                systemLog.setTaskId(taskId);
                                systemLog.setDeviceId(link.getDeviceId());
                                systemLog.setContent("发送开始指令失败:"+startCmd);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("下发开始指令时出现异常，任务ID: {}", taskId, ex);
                //记录打印日志
                SystemLog systemLog = new SystemLog();
                systemLog.setLogType(SystemLogType.PRINT.getCode());
                systemLog.setLogLevel(SystemLogLevel.ERROR.getCode());
                systemLog.setTaskId(taskId);
                systemLog.setContent("下发开始指令时出现异常:"+ex.getMessage());
            }
            
            // 创建并启动Runner
            CommandSenderRunner runner = new CommandSenderRunner(
                    taskId, dispatcher, deviceDataHandlerService, commandQueueService);
            
            Future<?> future = taskSenderExecutor.submit(runner);
            
            // 存储Runner和Future
            runningRunners.put(taskId, runner);
            runningFutures.put(taskId, future);
            
            log.info("指令发送启动成功，任务ID: {}", taskId);
            
        } catch (Exception e) {
            log.error("启动指令发送失败，任务ID: {}", taskId, e);
            // 清理资源
            runningRunners.remove(taskId);
            runningFutures.remove(taskId);
        }
    }

    
    @Override
    public void stopSending(Long taskId) {
        try {
            log.info("停止指令发送，任务ID: {}", taskId);

            
            // 停止Runner
            CommandSenderRunner runner = runningRunners.get(taskId);
            if (runner != null) {
                runner.stop();
            }
            
            // 取消Future
            Future<?> future = runningFutures.get(taskId);
            if (future != null) {
                future.cancel(true);
            }


             // 向参与设备发送停止加工指令（stop:）
            try {
                TaskDeviceLink query = new TaskDeviceLink();
                query.setTaskId(taskId);
                List<TaskDeviceLink> links = taskDeviceLinkService.list(query);
                if (links != null) {
                    for (TaskDeviceLink link : links) {
                        DeviceInfo d = deviceInfoService.selectDeviceInfoById(link.getDeviceId());
                        if (d != null) {
                            String stopCmd = "stop:";
                            boolean success = dispatcher.sendCommandToDevice(d.getId().toString(), stopCmd);
                            if (success) {
                                log.info("已发送停止指令至设备，设备ID: {}, IP: {}, 端口: {}", d.getId(), d.getIpAddress(), d.getPort());
                                //记录打印日志
                                SystemLog systemLog = new SystemLog();
                                systemLog.setLogType(SystemLogType.PRINT.getCode());
                                systemLog.setLogLevel(SystemLogLevel.INFO.getCode());
                                systemLog.setTaskId(taskId);
                                systemLog.setDeviceId(link.getDeviceId());
                                systemLog.setContent("已下发停止指令至设备:"+stopCmd);
                            } else {
                                log.warn("发送停止指令失败，设备ID: {}, IP: {}, 端口: {}", d.getId(), d.getIpAddress(), d.getPort());
                                //记录打印日志
                                SystemLog systemLog = new SystemLog();
                                systemLog.setLogType(SystemLogType.PRINT.getCode());
                                systemLog.setLogLevel(SystemLogLevel.ERROR.getCode());
                                systemLog.setTaskId(taskId);
                                systemLog.setDeviceId(link.getDeviceId());
                                systemLog.setContent("下发停止指令失败:"+stopCmd);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("下发停止指令时出现异常，任务ID: {}", taskId, ex);
                //记录打印日志
                SystemLog systemLog = new SystemLog();
                systemLog.setLogType(SystemLogType.PRINT.getCode());
                systemLog.setLogLevel(SystemLogLevel.ERROR.getCode());
                systemLog.setTaskId(taskId);
                systemLog.setContent("下发停止指令时出现异常:"+ex.getMessage());
            }
            
            // 清理资源
            runningRunners.remove(taskId);
            runningFutures.remove(taskId);
            
            log.info("指令发送停止成功，任务ID: {}", taskId);

            // 停止队列维护任务
            stopQueueMaintenance(taskId);
            
        } catch (Exception e) {
            log.error("停止指令发送失败，任务ID: {}", taskId, e);
        }
    }
    
    @Override
    public boolean isSending(Long taskId) {
        CommandSenderRunner runner = runningRunners.get(taskId);
        return runner != null && runner.isRunning();
    }
    
    @Override
    public int getSentCount(Long taskId) {
        CommandSenderRunner runner = runningRunners.get(taskId);
        return runner != null ? runner.getSentCount() : 0;
    }
    
    @Override
    public Set<Long> getRunningTasks() {
        return runningRunners.keySet();
    }
    
    @Override
    public void pauseSending(Long taskId) {
        try {
            log.info("暂停指令发送，任务ID: {}", taskId);
            
            CommandSenderRunner runner = runningRunners.get(taskId);
            if (runner != null) {
                runner.pause();
            }

            // 暂停时也停止维护任务，避免误判
            stopQueueMaintenance(taskId);
            
            log.info("指令发送暂停成功，任务ID: {}", taskId);
            
        } catch (Exception e) {
            log.error("暂停指令发送失败，任务ID: {}", taskId, e);
        }
    }
    
    @Override
    public void resumeSending(Long taskId) {
        try {
            log.info("恢复指令发送，任务ID: {}", taskId);
            
            CommandSenderRunner runner = runningRunners.get(taskId);
            if (runner != null) {
                runner.resume();
            }
            
            log.info("指令发送恢复成功，任务ID: {}", taskId);
            
        } catch (Exception e) {
            log.error("恢复指令发送失败，任务ID: {}", taskId, e);
        }
    }


    private void stopQueueMaintenance(Long taskId) {
        try {
           ScheduledFuture<?> future = queueMaintainers.remove(taskId);
            if (future != null) {
                future.cancel(false);
                log.info("已停止队列维护任务，任务ID: {}", taskId);
            }
        } catch (Exception e) {
            log.error("停止队列维护任务失败，任务ID: {}", taskId, e);
        }
    }
    
    @Override
    public Map<String, Object> getSendingStatistics(Long taskId) {
        Map<String, Object> statistics = new java.util.HashMap<>();
        
        CommandSenderRunner runner = runningRunners.get(taskId);
        if (runner != null) {
            statistics.put("taskId", taskId);
            statistics.put("isRunning", runner.isRunning());
            statistics.put("isPaused", runner.isPaused());
            statistics.put("sentCount", runner.getSentCount());
            statistics.put("failedCount", runner.getFailedCount());
        } else {
            statistics.put("taskId", taskId);
            statistics.put("isRunning", false);
            statistics.put("isPaused", false);
            statistics.put("sentCount", 0);
            statistics.put("failedCount", 0);
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
            startSending(event.getTaskId());
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
            pauseSending(event.getTaskId());
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
            stopSending(event.getTaskId());
        } catch (Exception e) {
            log.error("处理任务停止事件异常，任务ID: {}", event.getTaskId(), e);
        }
    }
}

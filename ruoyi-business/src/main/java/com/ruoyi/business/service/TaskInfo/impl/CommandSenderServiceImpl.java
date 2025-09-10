package com.ruoyi.business.service.TaskInfo.impl;

import com.ruoyi.business.domain.TaskInfo.PrintCommand;
import com.ruoyi.business.domain.TaskInfo.TaskInfo;
import com.ruoyi.business.enums.ItemStatus;
import com.ruoyi.business.events.TaskPauseEvent;
import com.ruoyi.business.domain.TaskInfo.TaskDeviceLink;
import com.ruoyi.business.service.TaskInfo.ITaskDeviceLinkService;
import com.ruoyi.business.domain.DeviceInfo.DeviceInfo;
import com.ruoyi.business.service.DeviceInfo.IDeviceInfoService;
import com.ruoyi.business.service.DeviceInfo.DeviceCommandService;
import com.ruoyi.business.service.TaskInfo.CommandSenderService;
import com.ruoyi.business.service.TaskInfo.TaskDispatcherService;
import com.ruoyi.business.service.TaskInfo.DeviceDataHandlerService;
import com.ruoyi.business.service.TaskInfo.CommandQueueService;
import com.ruoyi.business.service.TaskInfo.runner.CommandSenderRunner;
import com.ruoyi.business.service.DataPoolItem.IDataPoolItemService;
import com.ruoyi.business.domain.DataPoolItem.DataPoolItem;
import com.ruoyi.business.service.TaskInfo.ITaskInfoService;
import com.ruoyi.business.events.TaskStartEvent;
import com.ruoyi.business.events.TaskStopEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

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
    private DeviceCommandService deviceCommandService;

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private IDataPoolItemService dataPoolItemService;

    @Autowired
    private ITaskInfoService taskInfoService;

    // 任务队列维护与状态标记调度句柄
    private final ConcurrentHashMap<Long, java.util.concurrent.ScheduledFuture<?>> queueMaintainers = new ConcurrentHashMap<>();
    
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
                            } else {
                                log.warn("发送开始指令失败，设备ID: {}, IP: {}, 端口: {}", d.getId(), d.getIpAddress(), d.getPort());
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("下发开始指令时出现异常，任务ID: {}", taskId, ex);
            }
            
            // 创建并启动Runner
            CommandSenderRunner runner = new CommandSenderRunner(
                    taskId, dispatcher, deviceDataHandlerService, commandQueueService);
            
            Future<?> future = taskSenderExecutor.submit(runner);
            
            // 存储Runner和Future
            runningRunners.put(taskId, runner);
            runningFutures.put(taskId, future);
            
            log.info("指令发送启动成功，任务ID: {}", taskId);

            // 启动队列维护与状态同步任务（每5秒）
            startQueueMaintenance(taskId);

            
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
                            } else {
                                log.warn("发送停止指令失败，设备ID: {}, IP: {}, 端口: {}", d.getId(), d.getIpAddress(), d.getPort());
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("下发停止指令时出现异常，任务ID: {}", taskId, ex);
            }
            
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

            // 恢复后重启维护任务
            startQueueMaintenance(taskId);
            
            log.info("指令发送恢复成功，任务ID: {}", taskId);
            
        } catch (Exception e) {
            log.error("恢复指令发送失败，任务ID: {}", taskId, e);
        }
    }

    private void startQueueMaintenance(Long taskId) {
        try {
            if (queueMaintainers.containsKey(taskId)) {
                return;
            }
            java.util.concurrent.ScheduledFuture<?> future = ((ThreadPoolTaskScheduler) taskScheduler)
                    .scheduleAtFixedRate(() -> maintainQueueAndMarkPrinting(taskId), 5000);
            queueMaintainers.put(taskId, future);
            log.info("已启动队列维护任务，任务ID: {}", taskId);
        } catch (Exception e) {
            log.error("启动队列维护任务失败，任务ID: {}", taskId, e);
        }
    }

    private void stopQueueMaintenance(Long taskId) {
        try {
            java.util.concurrent.ScheduledFuture<?> future = queueMaintainers.remove(taskId);
            if (future != null) {
                future.cancel(false);
                log.info("已停止队列维护任务，任务ID: {}", taskId);
            }
        } catch (Exception e) {
            log.error("停止队列维护任务失败，任务ID: {}", taskId, e);
        }
    }

    // 标记SENT为PRINTING并按30%阈值补充
    private void maintainQueueAndMarkPrinting(Long taskId) {
        try {
            // 1) 将队列中状态为SENT的命令，更新数据库为打印中（PRINTING）
            List<PrintCommand> snapshot = commandQueueService.getAllCommandsSnapshot();
            if (snapshot != null && !snapshot.isEmpty()) {
                List<DataPoolItem> toUpdate = new ArrayList<>();
                for (PrintCommand pc : snapshot) {
                    if (pc.getTaskId() != null && taskId.equals(pc.getTaskId()) && "SENT".equals(pc.getStatus())) {
                        DataPoolItem item = new DataPoolItem();
                        try {
                            item.setId(Long.valueOf(pc.getId()));
                            toUpdate.add(item);
                        } catch (NumberFormatException ignore) {
                        }
                    }
                }
                if (!toUpdate.isEmpty()) {
                    try {
                        dataPoolItemService.updateDataPoolItemsStatus(toUpdate, ItemStatus.PRINTING.getCode());
                    } catch (Exception ex) {
                        log.warn("批量更新数据项为PRINTING失败，任务ID: {}", taskId, ex);
                    }
                }
            }

            // 2) 队列补充：低于预加载数量的30%时补充
            int queueSize = commandQueueService.getQueueSize();
            TaskInfo t = taskInfoService.selectTaskInfoById(taskId);
            Integer preload = t != null ? t.getPreloadDataCount() : 20;
            int base = preload != null ? preload : 20;
            int threshold = (int) Math.ceil(base * 0.3);
            if (queueSize < threshold) {
                log.info("队列低于阈值，准备补充，任务ID: {}, 当前大小: {}, 阈值: {}", taskId, queueSize, threshold);
            }
        } catch (Exception e) {
            log.error("维护队列与标记打印中失败，任务ID: {}", taskId, e);
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

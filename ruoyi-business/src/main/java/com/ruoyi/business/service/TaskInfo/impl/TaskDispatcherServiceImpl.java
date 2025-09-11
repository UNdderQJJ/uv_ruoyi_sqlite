package com.ruoyi.business.service.TaskInfo.impl;

import com.ruoyi.business.domain.DataPool.DataPool;
import com.ruoyi.business.domain.SystemLog.SystemLog;
import com.ruoyi.business.domain.TaskInfo.*;
import com.ruoyi.business.enums.*;
import com.ruoyi.business.events.TaskPauseEvent;
import com.ruoyi.business.service.SystemLog.ISystemLogService;
import com.ruoyi.business.service.TaskInfo.*;
import com.ruoyi.business.service.DeviceInfo.IDeviceInfoService;
import com.ruoyi.business.domain.DeviceInfo.DeviceInfo;
import com.ruoyi.business.service.DeviceInfo.DeviceConfigService;
import com.ruoyi.business.service.DeviceInfo.DeviceCommandService;
import com.ruoyi.business.events.TaskStartEvent;
import com.ruoyi.business.events.TaskStopEvent;
import com.ruoyi.business.events.CommandCompletedEvent;
import com.ruoyi.business.utils.StxEtxProtocolUtil;
import com.ruoyi.common.exception.ServiceException;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.TaskScheduler;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.ruoyi.business.config.TaskDispatchProperties;
import com.ruoyi.business.service.TaskInfo.CommandQueueService;
import com.ruoyi.business.service.DataPoolItem.IDataPoolItemService;
import com.ruoyi.business.domain.DataPoolItem.DataPoolItem;
import com.ruoyi.business.service.TaskInfo.ITaskDeviceLinkService;
import com.ruoyi.business.domain.TaskInfo.TaskDeviceLink;
import com.ruoyi.business.service.DataPoolTemplate.IDataPoolTemplateService;
import com.ruoyi.business.domain.DataPoolTemplate.DataPoolTemplate;
import com.ruoyi.business.service.DeviceFileConfig.IDeviceFileConfigService;
import com.ruoyi.business.domain.DeviceFileConfig.DeviceFileConfig;

/**
 * 任务调度服务实现
 * 系统的"大脑"，维护所有共享状态，管理任务生命周期
 */
@Service
public class TaskDispatcherServiceImpl implements TaskDispatcherService {
    
    private static final Logger log = LoggerFactory.getLogger(TaskDispatcherServiceImpl.class);
    
    // 核心状态管理 - 使用ConcurrentHashMap保证线程安全
    private final ConcurrentHashMap<String, DeviceTaskStatus> deviceStatusMap = new ConcurrentHashMap<>();
    // 任务状态
    private final ConcurrentHashMap<Long, TaskDispatchStatus> taskStatusMap = new ConcurrentHashMap<>();
    // 设备通道
    private final ConcurrentHashMap<String, Channel> deviceChannels = new ConcurrentHashMap<>();
    // 设备心跳时间
    private final ConcurrentHashMap<String, Long> heartbeatTimestamps = new ConcurrentHashMap<>();
    // 设备在途指令计数器（线程安全）
    private final ConcurrentHashMap<String, AtomicInteger> inFlightCounters = new ConcurrentHashMap<>();
    // 设备计数器锁，确保每个设备的计数操作顺序执行
    private final ConcurrentHashMap<String, Object> deviceLocks = new ConcurrentHashMap<>();
    // 任务进度上报定时器
    private final ConcurrentHashMap<Long,ScheduledFuture<?>> progressUpdaters = new ConcurrentHashMap<>();
    
    // 移除本地队列，使用共享的CommandQueueService
    
    // 当前任务
    private volatile TaskInfo currentTask;


    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private ITaskInfoService taskInfoService;

    @Autowired
    private ITaskDeviceLinkService iTaskDeviceLinkService;
    
    @Autowired
    private IDeviceInfoService deviceInfoService;
    
    @Autowired
    private DeviceConfigService deviceConfigService;
    
    @Autowired
    private DeviceCommandService deviceCommandService;

    @Autowired
    private TaskDispatchProperties taskDispatchProperties;

    @Autowired
    private CommandQueueService commandQueueService;

    @Autowired
    private IDataPoolItemService dataPoolItemService;

    @Autowired
    private ITaskDeviceLinkService taskDeviceLinkService;

    @Autowired
    private ISystemLogService systemLogService;
    
    @Autowired
    private TaskScheduler taskScheduler;
    
    @Override
    public void startNewTask(TaskDispatchRequest request) {
        try {
            log.info("启动新任务调度，任务ID: {}", request.getTaskId());
            
            // 1. 初始化任务状态
            TaskDispatchStatus taskStatus = new TaskDispatchStatus();
            taskStatus.setTaskId(request.getTaskId());
            taskStatus.setStatus(TaskDispatchStatusEnum.INITIALIZING.getCode());
            taskStatus.setStartTime(System.currentTimeMillis());
            taskStatus.setDeviceCount(request.getDeviceIds() != null ? request.getDeviceIds().length : 0);
            taskStatus.setPoolId(request.getPoolId());
            taskStatus.setTaskName(request.getTaskName());
            taskStatus.setDescription(request.getDescription());
            taskStatus.setOriginalCommandCount(request.getOriginalCount());
            taskStatus.setPlannedPrintCount(request.getPrintCount());
            taskStatusMap.put(request.getTaskId(), taskStatus);
            taskDispatchProperties.setPlanPrintCount(request.getPrintCount());//设置计划打印数量
            
            // 2. 执行预检
            if (!executePreFlightChecks(request.getTaskId(), request.getDeviceIds())) {
                taskStatus.setStatus(TaskDispatchStatusEnum.FAILED.getCode());
                taskStatus.setErrorMessage("预检失败");
                taskStatus.setEndTime(System.currentTimeMillis());
                return;
            }
             // 3. 更新任务状态为运行中
            taskStatus.setStatus(TaskDispatchStatusEnum.RUNNING.getCode());
            // 更新任务状态
            taskInfoService.updateTaskStatus(request.getTaskId(), TaskStatus.RUNNING);
            //更新设备状态
            for(Long deviceId : request.getDeviceIds()){
                taskDeviceLinkService.updateDeviceStatus( request.getTaskId(), deviceId,TaskDeviceStatus.PRINTING.getCode());
                deviceInfoService.updateDeviceStatus(deviceId, DeviceStatus.ONLINE_PRINTING.getCode());
            }
            // 4. 发布任务启动事件，让其他服务监听并启动
            eventPublisher.publishEvent(new TaskStartEvent(this, request));
            
            // 5. 设置当前任务
            currentTask = taskInfoService.selectTaskInfoById(request.getTaskId());
            if (currentTask == null) {
                log.error("任务不存在，任务ID: {}", request.getTaskId());
                taskStatus.setStatus(TaskDispatchStatusEnum.FAILED.getCode());
                taskStatus.setErrorMessage("任务不存在");
                taskStatus.setEndTime(System.currentTimeMillis());
                return;
            }
            //6. 启动任务进度上报定时器
            startProgressUpdater(request.getTaskId());
            
            log.info("任务调度启动成功，任务ID: {}", request.getTaskId());
            
        } catch (Exception e) {
            log.error("启动任务调度失败，任务ID: {}", request.getTaskId(), e);
            TaskDispatchStatus taskStatus = taskStatusMap.get(request.getTaskId());
            if (taskStatus != null) {
                taskStatus.setStatus(TaskDispatchStatusEnum.FAILED.getCode());
                taskStatus.setErrorMessage(e.getMessage());
                taskStatus.setEndTime(System.currentTimeMillis());
            }
        }
    }
    
    @Override
    public void stopTaskDispatch(Long taskId) {
        try {
            log.info("停止任务调度，任务ID: {}", taskId);
            
            // 发布任务停止事件
            eventPublisher.publishEvent(new TaskStopEvent(this, taskId));
            
            // 更新任务状态
            TaskDispatchStatus taskStatus = taskStatusMap.get(taskId);
            if (taskStatus != null) {
                taskStatus.setStatus(TaskDispatchStatusEnum.STOPPED.getCode());
                taskStatus.setEndTime(System.currentTimeMillis());
            }

            // 更新任务状态
            taskInfoService.updateTaskStatus(taskId, TaskStatus.STOPPED);
            //更新设备状态
            List<TaskDeviceLink> linkList = taskDeviceLinkService.listByTaskId(taskId);
            for(TaskDeviceLink link : linkList){
                DeviceInfo deviceInfo = deviceInfoService.selectDeviceInfoById(link.getDeviceId());
                //在设备正常的情况下可以进行状态更新
                if(deviceInfo.getStatus().equals(DeviceStatus.ONLINE_PRINTING.getCode()) || deviceInfo.getStatus().equals(DeviceStatus.ONLINE_IDLE.getCode())) {
                    deviceInfoService.updateDeviceStatus(link.getDeviceId(), DeviceStatus.ONLINE_PRINTING.getCode());
                    taskDeviceLinkService.updateDeviceStatus(taskId, link.getDeviceId(), TaskDeviceStatus.WAITING.getCode());
                    //在设备异常的情况下只能进行状态更新
                    deviceStatusMap.get(link.getDeviceId().toString()).setStatus(TaskDeviceStatus.WAITING.getCode());
                }else {
                    DeviceTaskStatus deviceTask = deviceStatusMap.get(link.getDeviceId().toString());
                    deviceTask.setStatus(TaskDeviceStatus.ERROR.getCode());
                }

            }

            // 停止任务进度上报定时器
            stopProgressUpdater(taskId);
            
            // 清空当前任务
            if (currentTask != null && currentTask.getId().equals(taskId)) {
                currentTask = null;
            }
            
            log.info("任务调度已停止，任务ID: {}", taskId);
            
        } catch (Exception e) {
            log.error("停止任务调度失败，任务ID: {}", taskId, e);
        }
    }

    @Override
    public void finishTaskDispatch(Long taskId) {
         try {
            log.info("完成任务调度，任务ID: {}", taskId);


            // 更新任务状态
            TaskDispatchStatus taskStatus = taskStatusMap.get(taskId);
            if (taskStatus != null) {
                taskStatus.setStatus(TaskDispatchStatusEnum.COMPLETED.getCode());
                taskStatus.setEndTime(System.currentTimeMillis());
            }

            // 更新任务状态
            taskInfoService.updateTaskStatus(taskId, TaskStatus.COMPLETED);
            //更新设备状态
            List<TaskDeviceLink> linkList = taskDeviceLinkService.listByTaskId(taskId);
            for(TaskDeviceLink link : linkList){
                DeviceInfo deviceInfo = deviceInfoService.selectDeviceInfoById(link.getDeviceId());
                //在设备正常的情况下可以进行状态更新
                if(deviceInfo.getStatus().equals(DeviceStatus.ONLINE_PRINTING.getCode()) || deviceInfo.getStatus().equals(DeviceStatus.ONLINE_IDLE.getCode())) {
                    deviceInfoService.updateDeviceStatus(link.getDeviceId(), DeviceStatus.ONLINE_IDLE.getCode());
                    taskDeviceLinkService.updateDeviceStatus(taskId, link.getDeviceId(), TaskDeviceStatus.WAITING.getCode());
                }
                //在设备异常的情况下只能进行状态更新
                deviceStatusMap.get(link.getDeviceId().toString()).setStatus(TaskDeviceStatus.WAITING.getCode());
            }
            //休眠2秒等待设备缓存池消耗
             Thread.sleep(2000);

            // 发布任务停止事件
            eventPublisher.publishEvent(new TaskStopEvent(this, taskId));

            // 停止任务进度上报定时器
            stopProgressUpdater(taskId);

            // 清空当前任务
            if (currentTask != null && currentTask.getId().equals(taskId)) {
                currentTask = null;
            }

            log.info("任务调度完成已停止，任务ID: {}", taskId);

        } catch (Exception e) {
            log.error("停止任务调度失败，任务ID: {}", taskId, e);
        }
    }

    @Override
    public void pauseTaskDispatch(Long taskId) {
        try {
            log.info("暂停任务调度，任务ID: {}", taskId);


              TaskDispatchRequest request = new TaskDispatchRequest();
            
            // 更新任务状态
            TaskDispatchStatus taskStatus = taskStatusMap.get(taskId);
             request.setTaskId(taskId);
            request.setPoolId(taskStatus.getPoolId());
            // 发布任务暂停事件
            eventPublisher.publishEvent(new TaskPauseEvent(this, taskId));
            if (taskStatus != null) {
                taskStatus.setStatus(TaskDispatchStatusEnum.PAUSED.getCode());
            }

            // 更新任务状态
            taskInfoService.updateTaskStatus(taskId, TaskStatus.PAUSED);
            //更新设备状态
            List<TaskDeviceLink> linkList = taskDeviceLinkService.listByTaskId(taskId);
            for(TaskDeviceLink link : linkList){
                DeviceInfo deviceInfo = deviceInfoService.selectDeviceInfoById(link.getDeviceId());
                //在设备正常的情况下可以进行状态更新
                if(deviceInfo.getStatus().equals(DeviceStatus.ONLINE_PRINTING.getCode())) {
                    taskDeviceLinkService.updateDeviceStatus(taskId, link.getDeviceId(), TaskDeviceStatus.WAITING.getCode());
                }
            }
            // 暂停时停止上报
            stopProgressUpdater(taskId);
            
            log.info("任务调度已暂停，任务ID: {}", taskId);
            
        } catch (Exception e) {
            log.error("暂停任务调度失败，任务ID: {}", taskId, e);
        }
    }
    
    @Override
    public void resumeTaskDispatch(Long taskId) {
        try {
            log.info("恢复任务调度，任务ID: {}", taskId);
            
            TaskDispatchStatus taskStatus = taskStatusMap.get(taskId);
            if (taskStatus == null) {
                throw new ServiceException("任务状态不存在");
            }
            
            // 发布任务启动事件（恢复）
            TaskDispatchRequest request = new TaskDispatchRequest();
            request.setTaskId(taskId);
            request.setPoolId(taskStatus.getPoolId());
            eventPublisher.publishEvent(new TaskStartEvent(this, request));
            
            // 更新任务状态
            taskStatus.setStatus(TaskDispatchStatusEnum.RUNNING.getCode());
            taskInfoService.updateTaskStatus(taskId, TaskStatus.RUNNING);
            //更新设备状态
            List<TaskDeviceLink> linkList = taskDeviceLinkService.listByTaskId(taskId);
            for(TaskDeviceLink link : linkList){
                //在设备正常的情况下可以进行状态更新
                    taskDeviceLinkService.updateDeviceStatus(taskId, link.getDeviceId(), TaskDeviceStatus.PRINTING.getCode());
            }
            // 恢复时开启上报
            startProgressUpdater(taskId);
            
            log.info("任务调度已恢复，任务ID: {}", taskId);
            
        } catch (Exception e) {
            log.error("恢复任务调度失败，任务ID: {}", taskId, e);
        }
    }


    @Override
    public boolean executePreFlightChecks(Long taskId, Long[] deviceIds) {
        TaskInfo taskInfo = taskInfoService.selectTaskInfoById(taskId);

        log.info("执行预检流程，任务ID: {}", taskId);
        
        if (deviceIds == null || deviceIds.length == 0) {
            log.warn("设备ID数组为空，任务ID: {}", taskId);
            return false;
        }
        
        for (Long deviceId : deviceIds) {
            String deviceIdStr = deviceId.toString();
            
            try {
                // 1. 获取设备信息
                DeviceInfo deviceInfo = deviceInfoService.selectDeviceInfoById(deviceId);
                if (deviceInfo == null) {
                    log.warn("设备不存在，设备ID: {}", deviceIdStr);
                    return false;
                }
                
                // 2. 健康检查 - 发送诊断指令
                if (!sendDiagnosticCommand(deviceIdStr)) {
                    log.warn("设备健康检查失败，设备ID: {}", deviceIdStr);
                    return false;
                }

                //3.填充设备命令缓存池
                
                // 4. 初始化设备状态
                DeviceTaskStatus deviceStatus = new DeviceTaskStatus();
                deviceStatus.setDeviceId(deviceIdStr);
                deviceStatus.setStatus(TaskDeviceStatus.WAITING.getCode());
                deviceStatus.setInFlightCount(0);
                deviceStatus.setLastHeartbeat(System.currentTimeMillis());
                deviceStatus.setCurrentTaskId(taskId);
                deviceStatus.setCompletedCount(0);
                deviceStatus.setAssignedCount(0);
                deviceStatus.setDeviceName(deviceInfo.getName());
                deviceStatus.setIpAddress(deviceInfo.getIpAddress());
                deviceStatus.setPort(deviceInfo.getPort());
                deviceStatus.setConnectionStatus("CONNECTED");
                deviceStatus.setDeviceType(deviceInfo.getDeviceType());
                deviceStatus.setDeviceUuid(deviceInfo.getDeviceUuid());
                deviceStatus.setCachePoolSize(taskInfo.getPreloadDataCount());

                
                deviceStatusMap.put(deviceIdStr, deviceStatus);
                heartbeatTimestamps.put(deviceIdStr, System.currentTimeMillis());
                
                // 初始化线程安全的在途指令计数器和设备锁
                inFlightCounters.put(deviceIdStr, new AtomicInteger(0));
                deviceLocks.put(deviceIdStr, new Object());
                
            } catch (Exception e) {
                log.error("预检设备异常，设备ID: {}", deviceIdStr, e);
                return false;
            }
        }
        
        // 预加载指令到公共缓冲池
//        try {
//
//            int preload = taskInfo.getPreloadDataCount();//缓存池大小
//            Long poolId = taskInfo.getPoolId();
//            if (poolId == null) {
//                log.warn("任务未绑定数据池，跳过预加载，任务ID: {}", taskId);
//            } else if (preload > 0) {
//                // 获取模板与设备文件配置（取任务第一条设备关联）
//                TaskDeviceLink query = new TaskDeviceLink();
//                query.setTaskId(taskId);
//                List<TaskDeviceLink> links = taskDeviceLinkService.list(query);
//                if (links == null || links.isEmpty()) {
//                    log.warn("任务无设备关联，跳过预加载，任务ID: {}", taskId);
//                } else {
//                    DataPoolTemplate template = iDataPoolTemplateService.selectDataPoolTemplateById(links.get(0).getPoolTemplateId());
//                    DeviceFileConfig fileConfig = iDeviceFileConfigService.selectDeviceFileConfigById(links.get(0).getDeviceFileConfigId());
//                    for(TaskDeviceLink deviceLink :  links) {
//                        DeviceInfo deviceInfo = deviceInfoService.selectDeviceInfoById(deviceLink.getDeviceId());
//                        //获取数据项，同时更新为打印中
//                        List<DataPoolItem> items = dataPoolItemService.selectPendingItems(poolId, preload);
//                        int produced = 0;
//                        if (items != null) {
//                            for (DataPoolItem item : items) {
//                                String cmd = buildGenericCommand( item, template, fileConfig);
//                                if (cmd != null) {
//                                    if (sendCommandToDevice(deviceInfo.getId().toString(), cmd)) {
//                                        produced++;
//                                    }
//                                }
//                                //更新设备ID
//                                item.setDeviceId(deviceLink.getDeviceId().toString());
//                                //更新设备计数器为preload
//                                AtomicInteger counter = inFlightCounters.computeIfAbsent(deviceLink.getDeviceId().toString(), k -> new AtomicInteger(0));
//                                counter.set(preload);
//
//                            }
//                            //更新为打印中
//                            dataPoolItemService.updateDataPoolItemsStatus(items, ItemStatus.PRINTING.getCode());
//                            if (produced > 0) {
//                                log.info("预加载指令成功，设备ID: {}, 任务ID: {}, 预加载数量: {}", deviceInfo.getId(), taskId, produced);
//                            }
//                        }
//
//                    }
//                }
//            }
//        } catch (Exception e) {
//            log.error("预检阶段预加载异常，任务ID: {}", taskId, e);
//        }

        log.info("预检流程完成，任务ID: {}", taskId);
        return true;
    }

    /**
     * 启动任务进度上报定时器（每5秒）
     */
    private void startProgressUpdater(Long taskId) {
        try {
            // 避免重复启动
            ScheduledFuture<?> existing = progressUpdaters.get(taskId);
            if (existing != null && !existing.isCancelled()) {
                return;
            }
            ScheduledFuture<?> future = ((ThreadPoolTaskScheduler) taskScheduler)
                    .scheduleAtFixedRate(() -> safeUpdateProgress(taskId), 5000);
            progressUpdaters.put(taskId, future);
            log.info("已启动任务进度上报定时器，任务ID: {}", taskId);
        } catch (Exception e) {
            log.error("启动任务进度上报定时器失败，任务ID: {}", taskId, e);
        }
    }

    /**
     * 停止任务进度上报定时器
     */
    private void stopProgressUpdater(Long taskId) {
        try {
            TaskDispatchStatus taskStatus = taskStatusMap.get(taskId);
            // 先安全地更新一次进度
            safeUpdateProgress(taskId);
            //将此任务下的数据池数据打印中改为待打印
            dataPoolItemService.updateToPendingItem(taskStatus.getPoolId());
            // 停止定时器
            ScheduledFuture<?> future = progressUpdaters.remove(taskId);
            if (future != null) {
                future.cancel(false);
                log.info("已停止任务进度上报定时器，任务ID: {}", taskId);
            }
        } catch (Exception e) {
            log.error("停止任务进度上报定时器失败，任务ID: {}", taskId, e);
        }
    }

    /**
     * 安全地汇总并持久化任务与设备进度
     */
    private void safeUpdateProgress(Long taskId) {
        try {
            // 汇总每个设备的分配与完成数量，并更新 TaskDeviceLink
            List<TaskDeviceLink> links = taskDeviceLinkService.listByTaskId(taskId);
            if (links == null || links.isEmpty()) {
                return;
            }
            // 获取任务状态
            TaskDispatchStatus taskDispatch = taskStatusMap.get(taskId);

            int totalCompleted = 0;
            for (TaskDeviceLink link : links) {
                String deviceId = link.getDeviceId() != null ? link.getDeviceId().toString() : null;
                if (deviceId == null) {
                    continue;
                }
                DeviceTaskStatus status = deviceStatusMap.get(deviceId);
                if (status != null && taskId.equals(status.getCurrentTaskId())) {
                    // 已分配与已完成来自内存状态
                    link.setStatus(status.getStatus());
                    link.setAssignedQuantity(status.getAssignedCount());

                    //前往数据库查询相应数据完成数
                    Long completedCount = dataPoolItemService.getCompletedCount(deviceId, taskDispatch.getPoolId(),ItemStatus.PRINTED.getCode());

                    link.setCompletedQuantity(Math.toIntExact(completedCount));
                    totalCompleted += completedCount;
                    try {
                        taskDeviceLinkService.updateLink(link);
                    } catch (Exception ex) {
                        log.warn("更新TaskDeviceLink进度失败，taskId: {}, deviceId: {}", taskId, deviceId, ex);
                    }
                }
            }

            // 同步任务总完成数
            TaskInfo task = new TaskInfo();
            task.setId(taskId);
            task.setCompletedQuantity(totalCompleted);
            taskDispatch.setCompletedCommandCount(totalCompleted);
            try {
                taskInfoService.updateTaskInfo(task);
            } catch (Exception ex) {
                log.warn("更新TaskInfo完成数量失败，taskId: {}", taskId, ex);
            }
        } catch (Exception e) {
            log.error("任务进度上报定时任务执行异常，任务ID: {}", taskId, e);
        }
    }
    
    @Override
    public void reportCommandCompleted(String deviceId, Long taskId) {
        DeviceTaskStatus deviceStatus = deviceStatusMap.get(deviceId);
        if (deviceStatus != null) {
            // 获取设备专用锁，确保计数操作的顺序性
            Object deviceLock = deviceLocks.computeIfAbsent(deviceId, k -> new Object());
            
            synchronized (deviceLock) {
                // 使用线程安全的计数器减少在途指令计数
                AtomicInteger counter = inFlightCounters.computeIfAbsent(deviceId, k -> new AtomicInteger(0));
                int oldValue = counter.get();
                int newValue = counter.decrementAndGet();
                if (newValue < 0) {
                    counter.set(0); // 确保不会小于0
                    newValue = 0;
                }
                
                // 同步更新DeviceTaskStatus中的计数
                deviceStatus.setInFlightCount(newValue);
                
                log.info("指令完成报告，设备ID: {}, 计数器变化: {} -> {}", deviceId, oldValue, newValue);
                
                // 更新完成计数
                deviceStatus.setCompletedCount(deviceStatus.getCompletedCount() + 1);
                deviceStatus.setLastHeartbeat(System.currentTimeMillis());
                heartbeatTimestamps.put(deviceId, System.currentTimeMillis());
            }
            
            // 更新任务统计
            updateTaskStatistics(taskId);
        }
        
        // 发布指令完成事件
        eventPublisher.publishEvent(new CommandCompletedEvent(this, taskId, deviceId));

        // 检查计划打印数量：-1 表示无限，查询数据队列数量，如果为0则完成任务
        try {
            if (commandQueueService.getQueueSize(taskId) == 0) {
                finishTaskDispatch(taskId);
            }
        } catch (Exception ex) {
            log.warn("检查计划打印数量时发生异常，taskId: {}", taskId, ex);
        }
    }
    
    @Override
    public void reportError(String deviceId, String errorMessage) {
        DeviceTaskStatus deviceStatus = deviceStatusMap.get(deviceId);
        if (deviceStatus != null) {
            deviceStatus.setStatus(TaskDeviceStatus.ERROR.getCode());
            deviceStatus.setErrorMessage(errorMessage);
            deviceStatus.setLastErrorTime(System.currentTimeMillis());
            log.error("设备错误，设备ID: {}, 错误信息: {}", deviceId, errorMessage);
        }
    }
    
    // 队列相关方法已移至CommandQueueService
    
    @Override
    public boolean canDeviceReceiveCommand(String deviceId) {
        DeviceTaskStatus deviceStatus = deviceStatusMap.get(deviceId);
        if (deviceStatus == null || TaskDeviceStatus.ERROR.getCode().equals(deviceStatus.getStatus())) {
            return false;
        }

        // 获取设备专用锁，确保计数检查的准确性
        Object deviceLock = deviceLocks.computeIfAbsent(deviceId, k -> new Object());
        
        synchronized (deviceLock) {
            // 使用线程安全的计数器检查在途指令数量
            AtomicInteger counter = inFlightCounters.get(deviceId);
            if (counter != null) {
                int inFlightCount = counter.get();
                // 可以根据设备能力设置最大在途指令数，这里暂时设为20
                int maxInFlight = deviceStatus.getCachePoolSize();
                if (inFlightCount >= maxInFlight) {
//                    log.debug("设备在途指令已达上限，设备ID: {}, 在途数量: {}", deviceId, inFlightCount);
                    return false;
                }
            }
        }
        
        return true;
    }
    
    @Override
    public void reportCommandSent(String deviceId) {
        DeviceTaskStatus deviceStatus = deviceStatusMap.get(deviceId);
        if (deviceStatus != null) {
            // 获取设备专用锁，确保计数操作的顺序性
            Object deviceLock = deviceLocks.computeIfAbsent(deviceId, k -> new Object());
            
            synchronized (deviceLock) {
                // 使用线程安全的计数器增加在途指令计数
                AtomicInteger counter = inFlightCounters.computeIfAbsent(deviceId, k -> new AtomicInteger(0));
                int oldValue = counter.get();
                int newValue = counter.incrementAndGet();
                
                // 同步更新DeviceTaskStatus中的计数
                deviceStatus.setInFlightCount(newValue);
                deviceStatus.setStatus(TaskDeviceStatus.PRINTING.getCode());
                deviceStatus.setLastHeartbeat(System.currentTimeMillis());
                heartbeatTimestamps.put(deviceId, System.currentTimeMillis());
                
                log.info("指令发送报告，设备ID: {}, 计数器变化: {} -> {}", deviceId, oldValue, newValue);
            }
        }
    }

    private int resolvePreloadCount(TaskInfo taskInfo) {
        Integer limit = null;
        if (taskInfo != null) {
            limit = taskInfo.getPreloadDataCount();
        }
        if (limit == null || limit <= 0) {
            limit = taskDispatchProperties != null && taskDispatchProperties.getPreloadCount() != null
                    ? taskDispatchProperties.getPreloadCount()
                    : 20;
        }
        return limit;
    }

    private String buildGenericCommand(DataPoolItem item, DataPoolTemplate template, DeviceFileConfig fileConfig) {
        if (template == null || fileConfig == null) {
            return item.getItemData();
        }
        Integer xAxis = template.getXAxis();
        Integer yAxis = template.getYAxis();
        Integer angle = template.getAngle();
        Integer width = template.getWidth();
        Integer height = template.getHeight();
        StringBuilder command = new StringBuilder();
        command.append("seta:data#").append(fileConfig.getVariableName()).append("=").append(item.getItemData())
                .append("+size#").append(width).append("|").append(height)
                .append("+pos#").append(xAxis).append("|").append(yAxis).append("|").append(angle).append("|").append("0");
        return command.toString();
    }

    /**
     * 统一的指令发送方法
     * 优先通过已注册的设备通道下发（使用STX/ETX协议）；无通道时退回短连TCP。
     * 
     * @param deviceId 设备ID
     * @param command 指令内容
     * @return 是否发送成功
     */
    public boolean sendCommandToDevice(String deviceId, String command) {
        try {
            if (command == null || command.trim().isEmpty()) {
                log.warn("指令内容为空，设备ID: {}", deviceId);
                return false;
            }
            
            // 优先通过通道发送
            Object ch = getDeviceChannel(deviceId);
            if (ch instanceof Channel channel && channel.isActive()) {
                return sendCommandViaChannel(channel, deviceId, command);
            }
            
            // 回退到短连接TCP
            DeviceInfo deviceInfo = deviceInfoService.selectDeviceInfoById(Long.valueOf(deviceId));
            if (deviceInfo != null && deviceInfo.getIpAddress() != null && deviceInfo.getPort() != null) {
                return deviceCommandService.sendCommand(deviceInfo.getIpAddress(), deviceInfo.getPort(), command);
            }
            
            log.warn("设备信息不完整，无法发送指令，设备ID: {}", deviceId);
            return false;
            
        } catch (Exception e) {
            log.error("发送指令异常，设备ID: {}, 指令: {}", deviceId, command, e);
            return false;
        }
    }

    /**
     * 获取设备池ID
     *
     * @param taskId 任务ID
     * @return 设备池ID
     */
    @Override
    public Long getPoolId(Long taskId) {
        TaskDispatchStatus taskStatus = taskStatusMap.get(taskId);
        return  taskStatus != null ? taskStatus.getPoolId() : null;
    }

    /**
     * 获取设备任务ID
     *
     * @param deviceId 设备ID
     * @return 设备任务ID
     */
    @Override
    public Long getDeviceTaskId(String deviceId) {
        DeviceTaskStatus deviceStatus = deviceStatusMap.get(deviceId);
        return deviceStatus != null ? deviceStatus.getCurrentTaskId() : null;
    }

    /**
     * 通过Netty通道发送STX/ETX协议指令
     * 
     * @param channel Netty通道
     * @param deviceId 设备ID
     * @param command 指令内容
     * @return 是否发送成功
     */
    private boolean sendCommandViaChannel(Channel channel, String deviceId, String command) {
        try {
            // 使用STX/ETX协议格式发送
            byte[] commandBytes = StxEtxProtocolUtil.buildCommand(command);
            channel.writeAndFlush(Unpooled.wrappedBuffer(commandBytes));

            log.debug("<========= 设备ID: {}, 指令: {}", deviceId, command);
            //记录通讯日志
            SystemLog systemLog = new SystemLog();
            systemLog.setLogType(SystemLogType.COMMUNICATION.getCode());
            systemLog.setTaskId(Long.valueOf(deviceId));
            systemLog.setDeviceId(Long.valueOf(deviceId));
            systemLog.setLogLevel(SystemLogLevel.INFO.getCode());
            systemLog.setContent("发送指令===>"+command);
            systemLogService.insert(systemLog);

            return true;
        } catch (Exception e) {
            log.error("通过通道发送指令失败，设备ID: {}, 指令: {}", deviceId, command, e);
            return false;
        }
    }
    
    /**
     * 优先通过已注册的设备通道下发；无通道时退回短连TCP。
     * @deprecated 请使用 sendCommandToDevice(String deviceId, String command) 方法
     */
    @Deprecated
    private boolean sendViaChannelOrTcp(DeviceInfo d, String cmd) {
        return sendCommandToDevice(d.getId().toString(), cmd);
    }
    
    @Override
    public TaskDispatchStatus getTaskDispatchStatus(Long taskId) {
        return taskStatusMap.get(taskId);
    }
    
    @Override
    public DeviceTaskStatus getDeviceTaskStatus(String deviceId) {
        return deviceStatusMap.get(deviceId);
    }
    
    @Override
    public void registerDeviceChannel(String deviceId, Object channel) {
        if (channel instanceof Channel) {
            deviceChannels.put(deviceId, (Channel) channel);
            log.info("注册设备通道，设备ID: {}", deviceId);
        }
    }
    
    @Override
    public void unregisterDeviceChannel(String deviceId) {
        deviceChannels.remove(deviceId);
        log.info("注销设备通道，设备ID: {}", deviceId);
    }
    
    @Override
    public Object getDeviceChannel(String deviceId) {
        return deviceChannels.get(deviceId);
    }
    
    /**
     * 获取设备在途指令数量（线程安全）
     * @param deviceId 设备ID
     * @return 在途指令数量
     */
    public int getDeviceInFlightCount(String deviceId) {
        // 获取设备专用锁，确保计数读取的准确性
        Object deviceLock = deviceLocks.computeIfAbsent(deviceId, k -> new Object());
        
        synchronized (deviceLock) {
            AtomicInteger counter = inFlightCounters.get(deviceId);
            return counter != null ? counter.get() : 0;
        }
    }
    
    @Override
    public Set<Long> getRunningTasks() {
        return taskStatusMap.entrySet().stream()
                .filter(entry -> TaskDispatchStatusEnum.RUNNING.getCode().equals(entry.getValue().getStatus()))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }
    
    @Override
    public boolean hasRunningTasks() {
        return !taskStatusMap.isEmpty();
    }
    
    @Override
    public Map<String, Object> getTaskStatistics(Long taskId) {
        Map<String, Object> statistics = new HashMap<>();
        
        TaskDispatchStatus taskStatus = taskStatusMap.get(taskId);
        if (taskStatus != null) {
            statistics.put("taskId", taskId);
            statistics.put("status", taskStatus.getStatus());
            statistics.put("startTime", taskStatus.getStartTime());
            statistics.put("endTime", taskStatus.getEndTime());
            statistics.put("totalCommandCount", taskStatus.getTotalCommandCount());
            statistics.put("sentCommandCount", taskStatus.getSentCommandCount());
            statistics.put("completedCommandCount", taskStatus.getCompletedCommandCount());
            statistics.put("failedCommandCount", taskStatus.getFailedCommandCount());
            statistics.put("deviceCount", taskStatus.getDeviceCount());
            statistics.put("onlineDeviceCount", taskStatus.getOnlineDeviceCount());
            statistics.put("progressPercentage", taskStatus.getProgressPercentage());
        }
        
        return statistics;
    }
    
    /**
     * 发送诊断指令
     */
    private boolean sendDiagnosticCommand(String deviceId) {
        try {
            // 发送clearbuf指令清除缓存
            log.debug("发送clearbuf清除缓存池缓存指令，设备ID: {}", deviceId);
            sendCommandToDevice(deviceId, DeviceConfigKey.CLEARBUF.key());
            
             //发送geta指令获取缓冲区数量
            log.debug("发送geta:指令获取缓冲区数量，设备ID: {}", deviceId);
            sendCommandToDevice(deviceId, DeviceConfigKey.GETA.key());
            
            return true;
        } catch (Exception e) {
            log.error("发送诊断指令失败，设备ID: {}", deviceId, e);
            return false;
        }
    }
    
    /**
     * 更新任务统计信息
     */
    private void updateTaskStatistics(Long taskId) {
        TaskDispatchStatus taskStatus = taskStatusMap.get(taskId);
        if (taskStatus != null) {
            // 统计各设备状态
            int totalCompleted = 0;// 已完成指令数量
            int totalSent = 0;// 已发送指令数量
            int onlineDeviceCount = 0;// 在线设备数量
            
            for (DeviceTaskStatus deviceStatus : deviceStatusMap.values()) {
                if (taskId.equals(deviceStatus.getCurrentTaskId())) {
                    totalCompleted += deviceStatus.getCompletedCount();
                    totalSent += deviceStatus.getInFlightCount() + deviceStatus.getCompletedCount();
                    
                    if (!TaskDeviceStatus.ERROR.getCode().equals(deviceStatus.getStatus())) {
                        onlineDeviceCount++;
                    }
                }
            }
            
            taskStatus.setCompletedCommandCount(totalCompleted);
            taskStatus.setSentCommandCount(totalSent);
            taskStatus.setOnlineDeviceCount(onlineDeviceCount);
            
            // 计算进度百分比
            if (taskStatus.getTotalCommandCount() != null && taskStatus.getTotalCommandCount() > 0) {
                double progress = (double) totalCompleted / taskStatus.getTotalCommandCount() * 100;
                taskStatus.setProgressPercentage(Math.min(progress, 100.0));
            }
        }
    }
    
    @Override
    public String assignDeviceForCommand(PrintCommand command) {
        // 获取任务关联的设备
        if (currentTask == null) {
            return null;
        }
        
        // 查找可用的设备（在线且缓存未满）
        for (Map.Entry<String, DeviceTaskStatus> entry : deviceStatusMap.entrySet()) {
            String deviceId = entry.getKey();
            DeviceTaskStatus deviceStatus = entry.getValue();
            
            // 检查设备是否在线且可以接收指令
            if (canDeviceReceiveCommand(deviceId)) {

//                // 检查设备是否属于当前任务
//                if (isDeviceInCurrentTask(deviceId)) {
//
//                }
                 return deviceId;
            }
        }
        
        return null; // 无可用设备
    }
    
    /**
     * 检查设备是否属于当前任务
     */
    private boolean isDeviceInCurrentTask(String deviceId) {
        // 这里应该检查设备是否属于当前任务
         TaskDeviceLink deviceLink =new TaskDeviceLink();
         deviceLink.setDeviceId(Long.valueOf(deviceId));
         deviceLink.setTaskId(currentTask.getId());
         List<TaskDeviceLink> taskDeviceLink = iTaskDeviceLinkService.list(deviceLink);
        return taskDeviceLink != null && !taskDeviceLink.isEmpty();
    }

}

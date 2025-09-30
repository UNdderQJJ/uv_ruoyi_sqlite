package com.ruoyi.business.service.TaskInfo.impl;
import com.ruoyi.business.domain.SystemLog.SystemLog;
import com.ruoyi.business.domain.TaskInfo.*;
import com.ruoyi.business.enums.*;
import com.ruoyi.business.service.DataPool.DataSourceLifecycleService;
import com.ruoyi.business.service.SystemLog.ISystemLogService;
import com.ruoyi.business.service.TaskInfo.*;
import com.ruoyi.business.service.DeviceInfo.IDeviceInfoService;
import com.ruoyi.business.domain.DeviceInfo.DeviceInfo;
import com.ruoyi.business.service.DeviceInfo.DeviceCommandService;
import com.ruoyi.business.events.TaskStartEvent;
import com.ruoyi.business.events.TaskStopEvent;
import com.ruoyi.business.events.CommandCompletedEvent;
import com.ruoyi.business.utils.StxEtxProtocolUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.ruoyi.business.config.TaskDispatchProperties;
import com.ruoyi.business.service.TaskInfo.CommandQueueService;
import com.ruoyi.business.service.DataPoolItem.IDataPoolItemService;
import com.ruoyi.business.service.TaskInfo.ITaskDeviceLinkService;
import com.ruoyi.business.domain.TaskInfo.TaskDeviceLink;

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
    
    // 完成计数缓冲（设备维度，内存聚合，供批量持久化使用）
    private final ConcurrentHashMap<String, AtomicInteger> completedCountsBuffer = new ConcurrentHashMap<>();

    // 当前任务
    private volatile TaskInfo currentTask;

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private ITaskInfoService taskInfoService;
    
    @Autowired
    private IDeviceInfoService deviceInfoService;
    
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

    @Resource
    private DataSourceLifecycleService dataSourceLifecycleService;

    @Autowired
    private DataPoolProducerService dataPoolProducerService;


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
            taskStatus.setSentCommandCount(request.getSentCommandCount());//已发送指令数量
            taskStatus.setReceivedCommandCount(request.getReceivedCommandCount());//接收到的指令数量
            taskDispatchProperties.setPlanPrintCount(request.getPrintCount());//设置计划打印数量
            taskDispatchProperties.setOriginalCount(request.getSentCommandCount());//设置已完成的原始数量
             // 加入任务线程
            taskStatusMap.put(request.getTaskId(), taskStatus);
            try {
                // 2. 执行预检
                if (!executePreFlightChecks(request.getTaskId(), request.getDeviceIds())) {
                    taskStatus.setStatus(TaskDispatchStatusEnum.FAILED.getCode());
                    taskStatus.setErrorMessage("预检失败");
                    taskStatus.setEndTime(System.currentTimeMillis());
                }
            } catch (Exception e) {
                //移除改任务下所有的设备与任务
                removeTaskDevices(request.getTaskId());
                throw new RuntimeException(e);
            }


            // 2.1. 启动数据池，进行数据获取填充进热数据库
            try {
                dataSourceLifecycleService.startDataSource(request.getPoolId());
            } catch (Exception e) {
                throw new RuntimeException("数据池启动失败");
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
            
            log.info("任务调度启动成功，任务ID: {}", request.getTaskId());

            //记录打印日志
            SystemLog systemLog = new SystemLog();
            systemLog.setLogType(SystemLogType.PRINT.getCode());
            systemLog.setLogLevel(SystemLogLevel.INFO.getCode());
            systemLog.setTaskId(request.getTaskId());
            systemLog.setPoolId(request.getPoolId());
            systemLog.setContent("任务调度启动成功!");
            systemLogService.insert(systemLog);
            
        } catch (Exception e) {
            log.error("启动任务调度失败，任务ID: {}", request.getTaskId(), e);
            //记录打印日志
            SystemLog systemLog = new SystemLog();
            systemLog.setLogType(SystemLogType.PRINT.getCode());
            systemLog.setLogLevel(SystemLogLevel.ERROR.getCode());
            systemLog.setTaskId(request.getTaskId());
            systemLog.setPoolId(request.getPoolId());
            systemLog.setContent("启动任务调度失败:"+e.getMessage());
            systemLogService.insert(systemLog);
            TaskDispatchStatus taskStatus = taskStatusMap.get(request.getTaskId());
            if (taskStatus != null) {
                taskStatus.setStatus(TaskDispatchStatusEnum.FAILED.getCode());
                taskStatus.setErrorMessage(e.getMessage());
                taskStatus.setEndTime(System.currentTimeMillis());
            }
            //移除改任务下所有的设备与任务
            removeTaskDevices(request.getTaskId());
            throw new  RuntimeException(e.getMessage());
        }
    }
    
    @Override
    public void stopTaskDispatch(Long taskId) {
        try {
            log.info("停止任务调度，任务ID: {}", taskId);

            // 关闭数据生成队列
            dataPoolProducerService.stopProduction(taskId);
            // 停止下发数据队列
            commandQueueService.clearQueue(taskId);

            // 在停止前等待本任务下所有设备缓存池清空
            waitUntilDeviceBuffersEmpty(taskId, 10000L);

            // 更新任务状态
            TaskDispatchStatus taskStatus = taskStatusMap.get(taskId);
            if (taskStatus != null) {
                taskStatus.setStatus(TaskDispatchStatusEnum.STOPPED.getCode());
                taskStatus.setEndTime(System.currentTimeMillis());
            }
            // 更新任务状态
            taskInfoService.updateTaskStatus(taskId, TaskStatus.STOPPED);

            // 发布任务停止事件
            eventPublisher.publishEvent(new TaskStopEvent(this, taskId));

            // 停止任务进度上报定时器
            stopProgressUpdater(taskId);

            // 停止数据源
            dataSourceLifecycleService.stopDataSource(getPoolId(taskId));

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
                if (deviceTask != null) {
                    deviceTask.setStatus(TaskDeviceStatus.ERROR.getCode());
                }
                }
                //更新任务关联设备数据
                DeviceTaskStatus deviceTask = deviceStatusMap.get(link.getDeviceId().toString());
                TaskDeviceLink link1 = new TaskDeviceLink();
                link1.setId(link.getId());
               link.setCachePoolSize(deviceTask.getDeviceBufferCount());
               taskDeviceLinkService.updateLink(link1);

            }
            
            // 在停止时输出接收与完成统计
            TaskDispatchStatus statusForLog = taskStatusMap.get(taskId);
            if (statusForLog != null) {
                 Integer sent = statusForLog.getSentCommandCount();
                Integer rect = statusForLog.getReceivedCommandCount();
                log.info("任务停止统计-taskId: {}, 已发送指令数量: {}, 已接收指令数量: {}",
                        taskId, sent == null ? 0 : sent, rect == null ? 0 : rect);
            }

            // 清空当前任务 与缓存池数据
            if (currentTask != null && currentTask.getId().equals(taskId)) {
                //清除commandQueue对应任务的缓存池数据
                commandQueueService.clearQueue(taskId);
                commandQueueService.removeTrackingQueue(taskId);
                // 清空当前任务
                currentTask = null;
            }

            //移除改任务下所有的设备与任务
            removeTaskDevices(taskId);
            
            log.info("任务调度已停止，任务ID: {}", taskId);

                //记录打印日志
                SystemLog systemLog = new SystemLog();
                systemLog.setLogType(SystemLogType.PRINT.getCode());
                systemLog.setLogLevel(SystemLogLevel.INFO.getCode());
                systemLog.setTaskId(taskId);
                systemLog.setPoolId(getPoolId(taskId));
                systemLog.setContent("任务调度已停止!");
                systemLogService.insert(systemLog);
            
        } catch (Exception e) {
            log.error("停止任务调度失败，任务ID: {}", taskId, e);
                //记录打印日志
                SystemLog systemLog = new SystemLog();
                systemLog.setLogType(SystemLogType.PRINT.getCode());
                systemLog.setLogLevel(SystemLogLevel.ERROR.getCode());
                systemLog.setTaskId(taskId);
                systemLog.setPoolId(getPoolId(taskId));
                systemLog.setContent("停止任务调度失败:"+e.getMessage());
                systemLogService.insert(systemLog);
        }
    }

    @Override
    public void finishTaskDispatch(Long taskId) {
         try {
            log.info("完成任务调度，任务ID: {}", taskId);

            // 在完成前等待本任务下所有设备缓存池清空
            waitUntilDeviceBuffersEmpty(taskId, 10000L);

            // 更新任务状态
            TaskDispatchStatus taskStatus = taskStatusMap.get(taskId);
            if (taskStatus != null) {
                taskStatus.setStatus(TaskDispatchStatusEnum.COMPLETED.getCode());
                taskStatus.setEndTime(System.currentTimeMillis());
            }

            // 更新任务状态
            taskInfoService.updateTaskStatus(taskId, TaskStatus.COMPLETED);

            // 发布任务停止事件
            eventPublisher.publishEvent(new TaskStopEvent(this, taskId));

            // 停止任务进度上报定时器
            stopProgressUpdater(taskId);

            // 停止数据源
            dataSourceLifecycleService.stopDataSource(getPoolId(taskId));

            // 在完成时输出接收与完成统计
            TaskDispatchStatus statusForLog = taskStatusMap.get(taskId);
            if (statusForLog != null) {
                     Integer sent = statusForLog.getSentCommandCount();
                Integer rect = statusForLog.getReceivedCommandCount();
                log.info("任务完成统计-taskId: {}, 已发送指令数量: {}, 已接收指令数量: {}",
                        taskId, sent == null ? 0 : sent, rect == null ? 0 : rect);
            }

            //更新设备状态
            List<TaskDeviceLink> linkList = taskDeviceLinkService.listByTaskId(taskId);
            for(TaskDeviceLink link : linkList){
                DeviceInfo deviceInfo = deviceInfoService.selectDeviceInfoById(link.getDeviceId());
                //在设备正常的情况下可以进行状态更新
                if(deviceInfo.getStatus().equals(DeviceStatus.ONLINE_PRINTING.getCode()) || deviceInfo.getStatus().equals(DeviceStatus.ONLINE_IDLE.getCode())) {
                    deviceInfoService.updateDeviceStatus(link.getDeviceId(), DeviceStatus.ONLINE_IDLE.getCode());
                    taskDeviceLinkService.updateDeviceStatus(taskId, link.getDeviceId(), TaskDeviceStatus.COMPLETED.getCode());
                }
                //在设备异常的情况下只能进行状态更新
                deviceStatusMap.get(link.getDeviceId().toString()).setStatus(TaskDeviceStatus.WAITING.getCode());
            }

            //休眠3秒等待设备缓存池消耗完成
            Thread.sleep(3000);

            // 停止下发数据队列（在设备消耗完缓存池后）
            commandQueueService.clearQueue(taskId);

             // 清空当前任务 与缓存池数据
            if (currentTask != null && currentTask.getId().equals(taskId)) {
                //清除commandQueue对应任务的缓存池数据
                commandQueueService.clearQueue(taskId);
                commandQueueService.removeTrackingQueue(taskId);
                // 清空当前任务
                currentTask = null;
            }
            //移除改任务下所有的设备与任务
             removeTaskDevices(taskId);

            log.info("打印任务已完成，任务ID: {}", taskId);

                //记录打印日志
                SystemLog systemLog = new SystemLog();
                systemLog.setLogType(SystemLogType.PRINT.getCode());
                systemLog.setLogLevel(SystemLogLevel.INFO.getCode());
                systemLog.setTaskId(taskId);
                systemLog.setPoolId(getPoolId(taskId));
                systemLog.setContent("打印任务已完成!");
                systemLogService.insert(systemLog);

        } catch (Exception e) {
            log.error("完成任务调度失败，任务ID: {}", taskId, e);

                //记录打印日志
                SystemLog systemLog = new SystemLog();
                systemLog.setLogType(SystemLogType.PRINT.getCode());
                systemLog.setLogLevel(SystemLogLevel.ERROR.getCode());
                systemLog.setTaskId(taskId);
                systemLog.setPoolId(getPoolId(taskId));
                systemLog.setContent("打印任务完成调度失败："+e.getMessage());
                systemLogService.insert(systemLog);
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
                TaskDeviceLink link = taskDeviceLinkService.selectByTaskIdAndDeviceId(taskId,deviceId);
                if (deviceInfo == null) {
                    log.warn("设备不存在，设备ID: {}", deviceIdStr);
                    return false;
                }

                // 1.5 连接检测 - 必须存在活跃通道
                Object ch = getDeviceChannel(deviceIdStr);
                if (!(ch instanceof Channel) || !((Channel) ch).isActive()) {
                    log.warn("设备未连接或通道不活跃，设备ID: {}", deviceIdStr);
                    throw new RuntimeException(deviceInfo.getName() + "设备未连接，请先连接设备");
                }

                // 2. 健康检查 - 发送诊断指令
                if (!sendDiagnosticCommand(taskId,deviceIdStr)) {
                    log.warn("设备健康检查失败，设备ID: {}", deviceIdStr);
                    return false;
                }

                //3.填充设备命令缓存池

                // 4. 初始化设备状态
                DeviceTaskStatus deviceStatus = new DeviceTaskStatus();
                deviceStatus.setDeviceId(deviceIdStr);
                deviceStatus.setStatus(TaskDeviceStatus.WAITING.getCode());
                deviceStatus.setInFlightCount(0);
                deviceStatus.setDeviceBufferCount(0);
                deviceStatus.setLastHeartbeat(System.currentTimeMillis());
                deviceStatus.setCurrentTaskId(taskId);
                deviceStatus.setCompletedCount(link.getCompletedQuantity());//获取任务完成数量
                deviceStatus.setReceivedCount(link.getReceivedQuantity());//获取任务接收数量
                deviceStatus.setAssignedCount(link.getAssignedQuantity());//获取任务分配数量
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

                //记录打印日志
                SystemLog systemLog = new SystemLog();
                systemLog.setLogType(SystemLogType.PRINT.getCode());
                systemLog.setLogLevel(SystemLogLevel.INFO.getCode());
                systemLog.setTaskId(taskId);
                systemLog.setDeviceId(deviceId);
                systemLog.setPoolId(getPoolId(taskId));
                systemLog.setContent("预检设备完成!");
                systemLogService.insert(systemLog);

                //休眠0.1秒
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("预检设备异常，设备ID: {}", deviceIdStr, e);
                //记录打印日志
                SystemLog systemLog = new SystemLog();
                systemLog.setLogType(SystemLogType.PRINT.getCode());
                systemLog.setLogLevel(SystemLogLevel.ERROR.getCode());
                systemLog.setTaskId(taskId);
                systemLog.setDeviceId(deviceId);
                systemLog.setPoolId(getPoolId(taskId));
                systemLog.setContent("预检设备异常:"+e.getMessage());
                systemLogService.insert(systemLog);
                throw  new RuntimeException(e.getMessage());
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
     * 移除任务下的所有DeviceTaskStatus线程设备
     */
    private void removeTaskDevices(Long taskId) {
        //获取所有改设备任务
        List<DeviceTaskStatus> deviceStatuses = deviceStatusMap.values().stream()
                .filter(status -> status.getCurrentTaskId() != null && status.getCurrentTaskId().equals(taskId)).toList();
        deviceStatuses.forEach(status -> {
            deviceStatusMap.remove(status.getDeviceId());
        });
        taskStatusMap.remove(taskId);
    }


    /**
     * 停止任务进度上报定时器
     */
    private void stopProgressUpdater(Long taskId) {
        try {

            TaskDispatchStatus taskStatus = taskStatusMap.get(taskId);
            // 先安全地更新一次进度
//            safeUpdateProgress(taskId);
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
     * 等待当前任务下设备的 deviceBufferCount 清空（为0）
     * 最长等待 waitMs 毫秒，中间每200ms检查一次
     */
    private void waitUntilDeviceBuffersEmpty(Long taskId, long waitMs) {
        long deadline = System.currentTimeMillis() + Math.max(waitMs, 0L);
        try {
            while (System.currentTimeMillis() < deadline) {
                boolean allEmpty = true;
                List<TaskDeviceLink> deviceLinks = taskDeviceLinkService.listByTaskId(taskId);
                for (TaskDeviceLink link : deviceLinks) {
                    String deviceId = link.getDeviceId().toString();
                    DeviceTaskStatus status = deviceStatusMap.get(deviceId);
                    if (status != null && TaskDeviceStatus.ERROR.getCode().equals(status.getStatus())) {
                        // 故障设备不阻塞退出
                        continue;
                    }
                    Integer buf = status != null ? status.getDeviceBufferCount() : null;
                    if (buf != null && buf > 0) {
                        allEmpty = false;
                        break;
                    }
                }
                if (allEmpty) {
                    return;
                }
                Thread.sleep(200);
            }
            log.warn("等待设备缓存池清空超时，taskId: {}", taskId);
        } catch (Exception e) {
            log.warn("等待设备缓存池清空异常，taskId: {}", taskId, e);
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

                deviceStatus.setLastHeartbeat(System.currentTimeMillis());
                heartbeatTimestamps.put(deviceId, System.currentTimeMillis());
            }
            
            // 关键：仅在内存中累加完成计数，交由统一调度批量持久化
            completedCountsBuffer.computeIfAbsent(deviceId, k -> new AtomicInteger(0)).incrementAndGet();

        }
        
        // 发布指令完成事件
        eventPublisher.publishEvent(new CommandCompletedEvent(this, taskId, deviceId));

        // 检查计划打印数量：-1 表示无限，查询数据队列数量，如果为0则完成任务
        TaskDispatchStatus taskStatus = taskStatusMap.get(taskId);
        // 任务不存在则返回
        if (ObjectUtils.isEmpty(taskStatus)){
            return;
        }

        try {
            if (commandQueueService.getQueueSize(taskId) == 0) {
                if (taskStatus.getPlannedPrintCount() == -1) {
                    // 检查任务是否已经启动完成（数据加载是否已经开始）
                    if (!isTaskStartupCompleted(taskId)) {
                        return;
                    }
                    //查询待打印与打印中的数量
                    int planPrintCount = dataPoolItemService.countByPending(taskStatus.getPoolId());
                    if (planPrintCount == 0) {
                        finishTaskDispatch(taskId);
                    }
                }else if (taskStatus.getPlannedPrintCount() > 0) {
                    // 检查任务是否已经启动完成（数据加载是否已经开始）
                    if (!isTaskStartupCompleted(taskId)) {
                        return;
                    }
                    //查询打印中的数量
                    int printingCount = dataPoolItemService.countByPrinting(taskStatus.getPoolId());
                    if (printingCount == 0) {
                        finishTaskDispatch(taskId);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("检查计划打印数量时发生异常，taskId: {}", taskId, ex);
        }
    }

    /**
     * 原子抽取并清空完成计数缓冲（供统一调度任务使用）
     */
    public Map<String, Integer> getAndClearCompletedBuffer() {
        Map<String, Integer> snapshot = new HashMap<>();
        for (Map.Entry<String, AtomicInteger> e : completedCountsBuffer.entrySet()) {
            snapshot.put(e.getKey(), e.getValue().get());
        }
        completedCountsBuffer.clear();
        return snapshot;
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

    /**
     * 请求设备缓冲区数量
     *
     * @param deviceId 设备ID
     */
    @Override
    public void requestDeviceBufferCount(String deviceId) {
        try {
            Object ch = getDeviceChannel(deviceId);
            if (ch instanceof Channel channel && channel.isActive()) {
                String cmd = "geta:"; // 按协议发送 'geta:'
                byte[] commandBytes = StxEtxProtocolUtil.buildCommand(cmd);
                channel.writeAndFlush(Unpooled.wrappedBuffer(commandBytes));
                log.debug("请求设备缓冲区数量，设备ID: {} -> {}", deviceId, cmd);
            } else {
                log.warn("设备通道不可用，无法请求缓冲区数量，设备ID: {}", deviceId);
            }
        } catch (Exception e) {
            log.error("请求设备缓冲区数量异常，设备ID: {}", deviceId, e);
        }
    }
    
    @Override
    public void updateDeviceBufferCount(String deviceId, Integer bufferCount) {
        try {
            if (deviceId == null || bufferCount == null) {
                return;
            }
            // 获取设备专用锁，确保顺序性
            Object deviceLock = deviceLocks.computeIfAbsent(deviceId, k -> new Object());
            synchronized (deviceLock) {
                DeviceTaskStatus deviceStatus = deviceStatusMap.get(deviceId);
                if (deviceStatus != null) {
                    deviceStatus.setDeviceBufferCount(bufferCount);
                    deviceStatus.setInFlightCount(bufferCount);
                }
                // 同步覆盖线程安全计数器
                AtomicInteger counter = inFlightCounters.computeIfAbsent(deviceId, k -> new AtomicInteger(0));
                counter.set(Math.max(0, bufferCount));
            }
        } catch (Exception e) {
            log.warn("同步设备缓存池数量失败，deviceId: {}", deviceId, e);
        }
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
        if (ObjectUtils.isEmpty(taskId) || ObjectUtils.isEmpty(taskStatusMap.get(taskId))){
            return null;
        }
        TaskDispatchStatus taskStatus = taskStatusMap.get(taskId);
        if(ObjectUtils.isEmpty(taskStatus)){
            return null;
        }
        return taskStatus.getPoolId();
    }

    /**
     * 获取设备任务ID
     *
     * @param deviceId 设备ID
     * @return 设备任务ID
     */
    @Override
    public Long getDeviceTaskId(String deviceId) {
        if(ObjectUtils.isEmpty(deviceId) || ObjectUtils.isEmpty(deviceStatusMap.get(deviceId))){
            return null;
        }
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

            log.debug("设备ID: {}, 发送指令===>: {}", deviceId, command);
            //记录通讯日志
            SystemLog systemLog = new SystemLog();
            systemLog.setLogType(SystemLogType.COMMUNICATION.getCode());
            systemLog.setLogLevel(SystemLogLevel.INFO.getCode());
            systemLog.setTaskId(Long.valueOf(deviceId));
            systemLog.setDeviceId(Long.valueOf(deviceId));
            systemLog.setPoolId(getPoolId(systemLog.getTaskId()));
            systemLog.setContent("发送指令===>"+command);
            systemLogService.insert(systemLog);

            return true;
        } catch (Exception e) {
            log.error("通过通道发送指令失败，设备ID: {}, 指令: {}", deviceId, command, e);
            //记录通讯日志
            SystemLog systemLog = new SystemLog();
            systemLog.setLogType(SystemLogType.COMMUNICATION.getCode());
            systemLog.setLogLevel(SystemLogLevel.ERROR.getCode());
            systemLog.setTaskId(Long.valueOf(deviceId));
            systemLog.setDeviceId(Long.valueOf(deviceId));
            systemLog.setPoolId(getPoolId(systemLog.getTaskId()));
            systemLog.setContent("发送指令失败===>"+command+";"+"错误:"+e.getMessage());
            systemLogService.insert(systemLog);
            return false;
        }
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
    private boolean sendDiagnosticCommand(Long taskId,String deviceId) {
        try {


            TaskInfo taskInfo = taskInfoService.selectTaskInfoById(taskId);
            // 在任务是待开始状态时 发送clearbuf指令清除缓存
            if (taskInfo.getStatus().equals(TaskStatus.PENDING.getCode())) {
                log.debug("发送clearbuf清除缓存池缓存指令，设备ID: {}", deviceId);
                sendCommandToDevice(deviceId, DeviceConfigKey.CLEARBUF.key());
            }

            //发送geta指令获取缓冲区数量
            log.debug("发送geta:指令获取缓冲区数量，设备ID: {}", deviceId);
            sendCommandToDevice(deviceId, DeviceConfigKey.GETA.key());
            
            return true;
        } catch (Exception e) {
            log.error("发送诊断指令失败，设备ID: {}", deviceId, e);
            return false;
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
            
            // 检查设备是否在线且可以接收指令
            if (canDeviceReceiveCommand(deviceId)) {
                 return deviceId;
            }
        }
        
        return null; // 无可用设备
    }
    

    /**
     * 自动检测任务完成状态
     * 每5秒检查一次运行中的任务是否已完成
     */
    @Scheduled(fixedRate = 5000)
    public void autoCheckTaskCompletion() {
        try {
            // 获取所有运行中的任务
            Set<Long> runningTasks = getRunningTasks();
            if (runningTasks.isEmpty()) {
                return;
            }

            for (Long taskId : runningTasks) {
                checkAndCompleteTask(taskId);
            }
        } catch (Exception e) {
            log.error("自动检测任务完成状态异常", e);
        }
    }

    /**
     * 检查并完成指定任务
     * @param taskId 任务ID
     */
    private void checkAndCompleteTask(Long taskId) {
        try {
            TaskDispatchStatus taskStatus = taskStatusMap.get(taskId);
            if (taskStatus == null || !TaskDispatchStatusEnum.RUNNING.getCode().equals(taskStatus.getStatus())) {
                return;
            }

            // 检查任务是否已经启动完成（数据加载是否已经开始）
            if (!isTaskStartupCompleted(taskId)) {
                log.debug("任务 {} 尚未启动完成，跳过完成检查", taskId);
                return;
            }

            // 检查指令队列是否为空
            int queueSize = commandQueueService.getQueueSize(taskId);
            if (queueSize > 0) {
                log.debug("任务 {} 指令队列还有 {} 条指令，继续等待", taskId, queueSize);
                return;
            }

             // 在完成前等待本任务下所有设备缓存池清空
                List<TaskDeviceLink> deviceLinks = taskDeviceLinkService.listByTaskId(taskId);
                for (TaskDeviceLink link : deviceLinks) {
                    String deviceId = link.getDeviceId().toString();
                    DeviceTaskStatus status = deviceStatusMap.get(deviceId);
                    if (status != null && TaskDeviceStatus.ERROR.getCode().equals(status.getStatus())) {
                        // 故障设备不阻塞退出
                        continue;
                    }
                    Integer buf = status != null ? status.getInFlightCount() : null;
                    if (buf != null && buf > 0) {
                      return;
                    }
                }

            // 检查计划打印数量
            if (taskStatus.getPlannedPrintCount() != null && taskStatus.getPlannedPrintCount() != -1) {
                // 有具体计划打印数量，检查是否达到目标
                int completedCount = taskStatus.getSentCommandCount() != null ? taskStatus.getSentCommandCount() : 0;
                int plannedCount = taskStatus.getPlannedPrintCount();

                if (completedCount >= plannedCount) {
                    log.info("任务 {} 已完成计划打印数量，完成数量: {}, 计划数量: {}", taskId, completedCount, plannedCount);
                    finishTaskDispatch(taskId);
                    return;
                }
            }

            // 检查数据池中是否还有待打印的数据
            Long poolId = taskStatus.getPoolId();
            if (poolId != null) {
                try {
                    int pendingCount = dataPoolItemService.countByPending(poolId);
                    int printingCount = dataPoolItemService.countByPrinting(poolId);

                    if (taskStatus.getPlannedPrintCount() != -1 && printingCount == 0) {
                        log.info("任务 {} 数据池中无打印中的数据，任务完成", taskId);
                        finishTaskDispatch(taskId);
                        return;
                    }else if (pendingCount == 0) {
                        log.info("任务 {} 数据池中无待打印数据，任务完成", taskId);
                        finishTaskDispatch(taskId);
                        return;
                    }

                    log.debug("任务 {} 数据池状态 , 打印中: {}", taskId, printingCount);
                } catch (Exception e) {
                    log.warn("检查任务 {} 数据池状态异常", taskId, e);
                }
            }

        } catch (Exception e) {
            log.error("检查任务 {} 完成状态异常", taskId, e);
        }
    }

    /**
     * 检查任务是否已经启动完成（数据加载是否已经开始）
     * @param taskId 任务ID
     * @return true 如果任务已启动完成，false 如果任务还在启动阶段
     */
    private boolean isTaskStartupCompleted(Long taskId) {
        try {
            TaskDispatchStatus taskStatus = taskStatusMap.get(taskId);
            if (taskStatus == null) {
                return false;
            }

            // 检查任务启动时间，如果启动时间太短（少于10秒），认为还在启动阶段
            long startTime = taskStatus.getStartTime();
            long currentTime = System.currentTimeMillis();
            long runningTime = currentTime - startTime;

            if (runningTime < 10000) { // 启动后10秒内认为还在启动阶段
                log.debug("任务 {} 启动时间过短 ({}ms)，认为还在启动阶段", taskId, runningTime);
                return false;
            }

            // 检查指令队列是否有数据（说明数据生成已经开始）
            int queueSize = commandQueueService.getQueueSize(taskId);
            if (queueSize > 0) {
                log.debug("任务 {} 指令队列已有数据 ({}条)，认为启动完成", taskId, queueSize);
                return true;
            }

            // 如果任务运行时间超过30秒，即使没有明显的活动迹象，也认为启动完成
            if (runningTime > 30000) {
                log.debug("任务 {} 运行时间超过30秒 ({}ms)，认为启动完成", taskId, runningTime);
                return true;
            }

            log.debug("任务 {} 尚未启动完成，运行时间: {}ms", taskId, runningTime);
            return false;

        } catch (Exception e) {
            log.error("检查任务 {} 启动完成状态异常", taskId, e);
            // 异常情况下，如果任务运行时间超过10秒，认为启动完成
            TaskDispatchStatus taskStatus = taskStatusMap.get(taskId);
            if (taskStatus != null) {
                long runningTime = System.currentTimeMillis() - taskStatus.getStartTime();
                return runningTime > 10000;
            }
            return false;
        }
    }

}

package com.ruoyi.tcp.business.Task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.domain.TaskInfo.TaskInfo;
import com.ruoyi.business.enums.TaskStatus;
import com.ruoyi.business.domain.TaskInfo.TaskDeviceLink;
import com.ruoyi.business.enums.TaskDeviceStatus;
import com.ruoyi.business.service.DataPool.DataSourceLifecycleService;
import com.ruoyi.business.service.TaskInfo.ITaskDeviceLinkService;
import com.ruoyi.business.service.DeviceInfo.IDeviceInfoService;
import com.ruoyi.business.domain.DeviceInfo.DeviceInfo;
import com.ruoyi.business.service.TaskInfo.ITaskInfoService;
import com.ruoyi.business.service.DeviceInfo.DeviceCommandService;
import com.ruoyi.business.service.DataPoolItem.IDataPoolItemService;
import com.ruoyi.business.service.TaskInfo.TaskDispatcherService;
import com.ruoyi.business.domain.TaskInfo.TaskDispatchRequest;
import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.utils.StringUtils;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 任务中心 TaskInfo TCP 处理器
 * 负责处理 /business/taskInfo/* 路径的请求
 */
@Component
public class TaskInfoManagementHandler {

    private static final Logger log = LoggerFactory.getLogger(TaskInfoManagementHandler.class);

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ITaskInfoService taskInfoService;

    @Resource
    private ITaskDeviceLinkService taskDeviceLinkService;

    @Resource
    private IDeviceInfoService deviceInfoService;

    @Resource
    private DeviceCommandService deviceCommandService;

    @Resource
    private IDataPoolItemService dataPoolItemService;

    @Resource
    private DataSourceLifecycleService dataSourceLifecycleService;

    @Resource
    private TaskDispatcherService taskDispatcherService;





    /**
     * 任务中心统一入口
     * 根据 path 分派到具体方法
     */
    public TcpResponse handleTaskInfoRequest(String path, String body) {
        try {
            if (path.endsWith("/create")) {
                return create(body);
            } else if (path.endsWith("/update")) {
                return update(body);
            } else if (path.endsWith("/delete")) {
                return delete(body);
            } else if (path.endsWith("/get")) {
                return get(body);
            } else if (path.endsWith("/list")) {
                return list(body);
            } else if (path.endsWith("/count")) {
                return count(body);
            } else if (path.endsWith("/start")) {
                return start(body);
            } else if (path.endsWith("/pause")) {
                return pause(body);
            } else if (path.endsWith("/resume")) {
                return resume(body);
            } else if (path.endsWith("/complete")) {
                return complete(body);
            } else if (path.endsWith("/fail")) {
                return fail(body);
            } else if (path.endsWith("/startDispatch")) {
                return startDispatch(body);
            } else if (path.endsWith("/stopDispatch")) {
                return stopDispatch(body);
            } else if (path.endsWith("/pauseDispatch")) {
                return pauseDispatch(body);
            } else if (path.endsWith("/resumeDispatch")) {
                return resumeDispatch(body);
            } else if (path.endsWith("/getDispatchStatus")) {
                return getDispatchStatus(body);
            } else if (path.endsWith("/getTaskStatistics")) {
                return getTaskStatistics(body);
            }
            return TcpResponse.error("未知的任务中心接口: " + path);
        } catch (Exception e) {
            log.error("处理任务中心请求发生异常", e);
            return TcpResponse.error(e.getMessage());
        }
    }

    /** 新增任务（支持同时添加关联设备） */
    private TcpResponse create(String body) throws Exception {
        TaskInfo taskInfo = objectMapper.readValue(StringUtils.isEmpty(body) ? "{}" : body, TaskInfo.class);
        taskInfo.setStatus(TaskStatus.PENDING.getCode());//待开始
        int rows = taskInfoService.insertTaskInfo(taskInfo);

        if (rows > 0) {
            // 解析可选的 deviceIds 数组，用于创建 task_device_link 关联
            try {
                // 允许 body 中包含字段: deviceIds, assignedQuantity, deviceFileConfigId, poolTemplateId
                MapWrapper map = objectMapper.readValue(StringUtils.isEmpty(body) ? "{}" : body, MapWrapper.class);
                if (map.deviceIds != null && map.deviceIds.length > 0) {
                    List<TaskDeviceLink> links = new java.util.ArrayList<>();
                    List<String> deviceNames = new java.util.ArrayList<>(); // 收集设备名称
                    
                    for (Long deviceId : map.deviceIds) {
                        TaskDeviceLink link = new TaskDeviceLink();
                        link.setTaskId(taskInfo.getId());
                        link.setDeviceId(deviceId);
                        // 查询设备名称（可选）
                        try {
                            DeviceInfo di = deviceInfoService.selectDeviceInfoById(deviceId);
                            if (di != null) {
                                link.setDeviceName(di.getName());
                                deviceNames.add(di.getName()); // 收集设备名称
                            }
                        } catch (Exception ignore) { }
                        link.setDeviceFileConfigId(map.deviceFileConfigId);
                        link.setPoolTemplateId(map.poolTemplateId);
                        link.setAssignedQuantity(taskInfo.getPreloadDataCount());//提前下发的数据条数
                        link.setCompletedQuantity(0);
                        link.setStatus(TaskDeviceStatus.WAITING.getCode());//等待
                        link.setDelFlag(0);
                        links.add(link);
                    }
                    taskDeviceLinkService.batchCreateLinks(links);
                    
                    // 将设备名称用逗号分隔存储到任务中
                    if (!deviceNames.isEmpty()) {
                        String deviceNamesStr = String.join(",", deviceNames);
                        taskInfo.setDeviceName(deviceNamesStr);
                        // 更新任务信息，保存设备名称
                        taskInfoService.updateTaskInfo(taskInfo);
                        log.info("[TaskCreation] 任务设备名称已保存: {}", deviceNamesStr);
                    }
                    
                    // 执行任务创建后的完整流程
                    executeTaskCreationWorkflow(taskInfo, map);

                }
            } catch (Exception e) {
                log.warn("解析任务关联设备时发生非致命异常: {}", e.getMessage());
            }
            return TcpResponse.success(taskInfo);
        }
        return TcpResponse.error("新增任务失败");
    }



    /**
     * 执行任务创建后的完整工作流程
     * 使用新的任务调度架构
     */
    private void executeTaskCreationWorkflow(TaskInfo taskInfo, MapWrapper map) {
        try {
            log.info("[TaskCreationWorkflow] 开始执行任务创建后的工作流程，任务ID: {}", taskInfo.getId());
            
            // 1. 启动数据池，进行数据获取填充进热数据库
            if (map.poolTemplateId != null) {
                try {
                    String result = dataSourceLifecycleService.startDataSource(taskInfo.getPoolId());
                    log.info("[TaskCreationWorkflow] 启动数据池结果：{}", result);
                } catch (Exception e) {
                    log.error("[TaskCreationWorkflow] 启动数据池异常，ID: {}", taskInfo.getPoolId(), e);
                }
            }
            
            // 2. 创建任务调度请求
            TaskDispatchRequest request = new TaskDispatchRequest();
            request.setTaskId(taskInfo.getId());
            request.setDeviceIds(map.deviceIds);
            request.setPoolId(taskInfo.getPoolId());
            request.setPreloadCount(taskInfo.getPreloadDataCount());
            request.setBatchSize(1000);
            request.setTaskName(taskInfo.getName());
            request.setDescription(taskInfo.getDescription());
            request.setDeviceFileConfigId(map.deviceFileConfigId);
            request.setPoolTemplateId(map.poolTemplateId);
            request.setAssignedQuantity(map.assignedQuantity);
            
            // 3. 启动任务调度
            taskDispatcherService.startNewTask(request);
            
            log.info("[TaskCreationWorkflow] 任务调度已启动，任务ID: {}", taskInfo.getId());
            
        } catch (Exception e) {
            log.error("[TaskCreationWorkflow] 执行任务创建后工作流程异常，任务ID: {}", taskInfo.getId(), e);
        }
    }

    /**
     * 用于解析创建任务时的扩展字段
     */
    private static class MapWrapper {
        public Long[] deviceIds;
        public Integer assignedQuantity;
        public Long deviceFileConfigId;
        public Long poolTemplateId;
    }


    /** 更新任务（支持增加或减少设备关联） */
    private TcpResponse update(String body) throws Exception {
        TaskInfo taskInfo = objectMapper.readValue(body, TaskInfo.class);
        int rows = taskInfoService.updateTaskInfo(taskInfo);
        
        if (rows > 0) {
            // 解析可选的设备关联更新参数
            try {
                MapWrapper map = objectMapper.readValue(body, MapWrapper.class);
                
                // 如果提供了 deviceIds，则更新设备关联
                if (map.deviceIds != null) {
                    // 先删除该任务的所有现有设备关联
                    taskDeviceLinkService.deleteByTaskId(taskInfo.getId());
                    
                    // 如果 deviceIds 不为空，则重新创建关联
                    if (map.deviceIds.length > 0) {
                        List<TaskDeviceLink> links = new java.util.ArrayList<>();
                        List<String> deviceNames = new java.util.ArrayList<>(); // 收集设备名称
                        
                        for (Long deviceId : map.deviceIds) {
                            TaskDeviceLink link = new TaskDeviceLink();
                            link.setTaskId(taskInfo.getId());
                            link.setDeviceId(deviceId);
                            // 查询设备名称（可选）
                            try {
                                DeviceInfo di = deviceInfoService.selectDeviceInfoById(deviceId);
                                if (di != null) {
                                    link.setDeviceName(di.getName());
                                    deviceNames.add(di.getName()); // 收集设备名称
                                }
                            } catch (Exception ignore) { }
                            link.setDeviceFileConfigId(map.deviceFileConfigId);
                            link.setPoolTemplateId(map.poolTemplateId);
                            link.setAssignedQuantity(map.assignedQuantity);
                            link.setCompletedQuantity(0);
                            link.setStatus(TaskDeviceStatus.WAITING.getCode());//等待
                            link.setDelFlag(0);
                            links.add(link);
                        }
                        taskDeviceLinkService.batchCreateLinks(links);
                        
                        // 更新任务中的设备名称
                        String deviceNamesStr = String.join(",", deviceNames);
                        taskInfo.setDeviceName(deviceNamesStr);
                        taskInfoService.updateTaskInfo(taskInfo);
                        log.info("[TaskUpdate] 任务设备名称已更新: {}", deviceNamesStr);
                    } else {
                        // 如果 deviceIds 为空数组，清空设备名称
                        taskInfo.setDeviceName("");
                        taskInfoService.updateTaskInfo(taskInfo);
                        log.info("[TaskUpdate] 任务设备名称已清空");
                    }
                }
            } catch (Exception e) {
                log.warn("更新任务设备关联时发生非致命异常: {}", e.getMessage());
            }
            return TcpResponse.success(taskInfo);
        }
        return TcpResponse.error("更新任务失败");
    }

    /** 批量删除任务（ids） */
    private TcpResponse delete(String body) throws Exception {
        // body: {"ids":[1,2,3]}
        Long[] ids = objectMapper.readValue(body, Long[].class);
        int rows = taskInfoService.deleteTaskInfoByIds(ids);
        return TcpResponse.success(rows);
    }

    /** 根据ID获取任务（body可为{"id":1}或直接传long） */
    private TcpResponse get(String body) throws Exception {
        Long id;
        if (StringUtils.isNumeric(body)) {
            id = Long.valueOf(body);
        } else {
            TaskInfo req = objectMapper.readValue(body, TaskInfo.class);
            id = req.getId();
        }
        TaskInfo data = taskInfoService.selectTaskInfoById(id);
        return TcpResponse.success(data);
    }

    /** 列表查询（支持 name/status/poolId 过滤） */
    private TcpResponse list(String body) throws Exception {
        TaskInfo query = StringUtils.isEmpty(body) ? new TaskInfo() : objectMapper.readValue(body, TaskInfo.class);
        List<TaskInfo> list = taskInfoService.selectTaskInfoList(query);
        return TcpResponse.success(list);
    }

    /** 统计数量（支持过滤） */
    private TcpResponse count(String body) throws Exception {
        TaskInfo query = StringUtils.isEmpty(body) ? new TaskInfo() : objectMapper.readValue(body, TaskInfo.class);
        Integer cnt = taskInfoService.countTaskInfo(query);
        return TcpResponse.success(cnt);
    }

    /** 启动任务 */
    private TcpResponse start(String body) throws Exception {
        Long id = parseId(body);
        int rows = taskInfoService.startTask(id);
        return TcpResponse.success(rows);
    }

    /** 暂停任务 */
    private TcpResponse pause(String body) throws Exception {
        Long id = parseId(body);
        int rows = taskInfoService.pauseTask(id);
        return TcpResponse.success(rows);
    }

    /** 恢复任务 */
    private TcpResponse resume(String body) throws Exception {
        Long id = parseId(body);
        int rows = taskInfoService.resumeTask(id);
        return TcpResponse.success(rows);
    }

    /** 完成任务 */
    private TcpResponse complete(String body) throws Exception {
        Long id = parseId(body);
        int rows = taskInfoService.completeTask(id);
        return TcpResponse.success(rows);
    }

    /** 失败任务 */
    private TcpResponse fail(String body) throws Exception {
        Long id = parseId(body);
        int rows = taskInfoService.failTask(id);
        return TcpResponse.success(rows);
    }

    /** 解析ID，支持{"id":1}或直接传long字符串 */
    private Long parseId(String body) throws Exception {
        if (StringUtils.isEmpty(body)) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (StringUtils.isNumeric(body)) {
            return Long.valueOf(body);
        }
        TaskInfo req = objectMapper.readValue(body, TaskInfo.class);
        return req.getId();
    }

    /** 启动任务调度 */
    private TcpResponse startDispatch(String body) throws Exception {
        TaskDispatchRequest request = objectMapper.readValue(body, TaskDispatchRequest.class);
        taskDispatcherService.startNewTask(request);
        return TcpResponse.success("任务调度启动成功");
    }

    /** 停止任务调度 */
    private TcpResponse stopDispatch(String body) throws Exception {
        Long id = parseId(body);
        taskDispatcherService.stopTaskDispatch(id);
        return TcpResponse.success("任务调度停止成功");
    }

    /** 暂停任务调度 */
    private TcpResponse pauseDispatch(String body) throws Exception {
        Long id = parseId(body);
        taskDispatcherService.pauseTaskDispatch(id);
        return TcpResponse.success("任务调度暂停成功");
    }

    /** 恢复任务调度 */
    private TcpResponse resumeDispatch(String body) throws Exception {
        Long id = parseId(body);
        taskDispatcherService.resumeTaskDispatch(id);
        return TcpResponse.success("任务调度恢复成功");
    }

    /** 获取任务调度状态 */
    private TcpResponse getDispatchStatus(String body) throws Exception {
        Long id = parseId(body);
        var status = taskDispatcherService.getTaskDispatchStatus(id);
        return TcpResponse.success(status);
    }

    /** 获取任务统计信息 */
    private TcpResponse getTaskStatistics(String body) throws Exception {
        Long id = parseId(body);
        var statistics = taskDispatcherService.getTaskStatistics(id);
        return TcpResponse.success(statistics);
    }
}



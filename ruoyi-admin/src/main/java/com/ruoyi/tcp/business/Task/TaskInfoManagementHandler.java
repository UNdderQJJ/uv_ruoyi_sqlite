package com.ruoyi.tcp.business.Task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.domain.DataPool.DataPool;
import com.ruoyi.business.domain.DeviceFileConfig.DeviceFileConfig;
import com.ruoyi.business.domain.TaskInfo.TaskInfo;
import com.ruoyi.business.enums.DeviceStatus;
import com.ruoyi.business.enums.ItemStatus;
import com.ruoyi.business.enums.TaskStatus;
import com.ruoyi.business.domain.TaskInfo.TaskDeviceLink;
import com.ruoyi.business.enums.TaskDeviceStatus;
import com.ruoyi.business.service.DataPool.DataSourceLifecycleService;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.business.service.DeviceFileConfig.IDeviceFileConfigService;
import com.ruoyi.business.service.TaskInfo.ITaskDeviceLinkService;
import com.ruoyi.business.service.DeviceInfo.IDeviceInfoService;
import com.ruoyi.business.domain.DeviceInfo.DeviceInfo;
import com.ruoyi.business.service.TaskInfo.ITaskInfoService;
import com.ruoyi.business.service.DeviceInfo.DeviceCommandService;
import com.ruoyi.business.service.DataPoolItem.IDataPoolItemService;
import com.ruoyi.business.service.TaskInfo.TaskDispatcherService;
import com.ruoyi.business.domain.TaskInfo.TaskDispatchRequest;
import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.core.page.PageQuery;
import com.ruoyi.common.core.page.PageResult;
import com.ruoyi.common.utils.StringUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private IDataPoolService dataPoolService;

    @Resource
    private IDataPoolItemService dataPoolItemService;

    @Resource
    private TaskDispatcherService taskDispatcherService;

    @Resource
    private IDeviceFileConfigService deviceFileConfigService;





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
            } else if (path.endsWith("/pageList")) {
                return pageList(body);
            }else if (path.endsWith("/count")) {
                return count(body);
            } else if (path.endsWith("/start")) {
                return startDispatch(body);
            } else if (path.endsWith("/stop")) {
                return stopDispatch(body);
            }  else if (path.endsWith("/getDispatchStatus")) {
                return getDispatchStatus(body);
            } else if (path.endsWith("/getTaskStatistics")) {
                return getTaskStatistics(body);
            } else if (path.endsWith("/getTodayProductivity")) {
                return getTodayProductivity();
            } else if (path.endsWith("/getQualityRate")) {
                return getQualityRate();
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
        // 允许 body 中包含字段: deviceIds
        MapWrapper map = objectMapper.readValue(StringUtils.isEmpty(body) ? "{}" : body, MapWrapper.class);
        taskInfo.setCompletedQuantity(0);//初始化完成数量为0
        taskInfo.setStatus(TaskStatus.PENDING.getCode());//待开始
        //校验所有设备模版是否一致
        List<Long> deviceIdList = List.of(map.deviceIds);
        boolean isConsistent = false;
        try {
            isConsistent = deviceFileConfigService.checkDeviceFileConfig(deviceIdList);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        if (!isConsistent){
            return TcpResponse.error("设备模版变量名称不一致，请检查设备模版是否一致");
        }
        int rows = taskInfoService.insertTaskInfo(taskInfo);

        List<DeviceFileConfig> deviceFileConfig = deviceFileConfigService.selectDefaultDeviceFileConfigListByDeviceId(deviceIdList.get(0));

        if (rows > 0) {
            // 解析可选的 deviceIds 数组，用于创建 task_device_link 关联
            try {
                if (map.deviceIds != null && map.deviceIds.length > 0) {

                    List<TaskDeviceLink> links = new ArrayList<>();
                    List<String> deviceNames = new ArrayList<>(); // 收集设备名称
                    List<String> deviceIds = new ArrayList<>(); // 收集设备ID
                    
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
                                deviceIds.add(deviceId.toString());
                            }
                        } catch (Exception ignore) { }

                        link.setDeviceFileConfigId(deviceFileConfig.get(0).getId());
                        link.setPoolTemplateId(taskInfo.getPoolTemplateId());
                        link.setAssignedQuantity(taskInfo.getPreloadDataCount());//提前下发的数据条数
                        link.setCompletedQuantity(0);
                        link.setStatus(TaskDeviceStatus.WAITING.getCode());//等待
                        link.setDelFlag(0);
                        links.add(link);
                    }
                    // 批量创建关联
                    taskDeviceLinkService.batchCreateLinks(links);

                    //更新设备当前任务
                    deviceInfoService.updateCurrentTask(deviceIds, taskInfo.getId());
                    
                    // 将设备名称用逗号分隔存储到任务中
                    if (!deviceNames.isEmpty()) {
                        String deviceNamesStr = String.join(",", deviceNames);
                        String deviceIdsStr = String.join(",", deviceIds);
                        taskInfo.setDeviceName(deviceNamesStr);
                        taskInfo.setDeviceId(deviceIdsStr);
                        // 更新任务信息，保存设备名称
                        taskInfoService.updateTaskInfo(taskInfo);
                        log.info("[TaskCreation] 任务设备名称已保存: {}", deviceNamesStr);
                    }

                }
            } catch (Exception e) {
                log.warn("解析任务关联设备时发生非致命异常: {}", e.getMessage());
            }
            return TcpResponse.success(taskInfo);
        }
        return TcpResponse.error("新增任务失败");
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

         MapWrapper map = objectMapper.readValue(body, MapWrapper.class);

          List<Long> deviceIdList = List.of(map.deviceIds);
        // 如果提供了 deviceIds，则更新设备关联
        //校验所有设备模版是否一致
        boolean isConsistent = false;
        try {
            isConsistent = deviceFileConfigService.checkDeviceFileConfig(deviceIdList);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        if (!isConsistent) {
            return TcpResponse.error("设备模版变量名称不一致，请检查设备模版是否一致");
        }

        int rows = taskInfoService.updateTaskInfo(taskInfo);

         List<DeviceFileConfig> deviceFileConfig = deviceFileConfigService.selectDefaultDeviceFileConfigListByDeviceId(deviceIdList.get(0));
        
        if (rows > 0) {
            // 解析可选的设备关联更新参数
            try {
                //清除设备的当前任务
                deviceInfoService.removeCurrentTask(taskInfo.getId());

                    // 先删除该任务的所有现有设备关联
                    taskDeviceLinkService.deleteByTaskId(taskInfo.getId());
                    
                    // 如果 deviceIds 不为空，则重新创建关联
                    if (map.deviceIds.length > 0) {
                        List<TaskDeviceLink> links = new java.util.ArrayList<>();
                        List<String> deviceNames = new java.util.ArrayList<>(); // 收集设备名称
                        List<String> deviceIds = new java.util.ArrayList<>();
                        
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
                                    deviceIds.add(deviceId.toString());
                                }
                            } catch (Exception ignore) { }
                            link.setDeviceFileConfigId(deviceFileConfig.get(0).getId());
                            link.setPoolTemplateId(taskInfo.getPoolTemplateId());
                            link.setAssignedQuantity(taskInfo.getPreloadDataCount());
                            link.setCompletedQuantity(0);
                            link.setStatus(TaskDeviceStatus.WAITING.getCode());//等待
                            link.setDelFlag(0);
                            links.add(link);
                        }
                        // 批量创建关联
                        taskDeviceLinkService.batchCreateLinks(links);

                        // 更新设备当前任务
                        deviceInfoService.updateCurrentTask(deviceIds, taskInfo.getId());
                        
                        // 更新任务中的设备名称
                        String deviceNamesStr = String.join(",", deviceNames);
                        String deviceIdsStr = String.join(",", deviceIds);
                        taskInfo.setDeviceName(deviceNamesStr);
                        taskInfo.setDeviceId(deviceIdsStr);
                        taskInfoService.updateTaskInfo(taskInfo);

                        log.info("[TaskUpdate] 任务设备名称已更新: {}", deviceNamesStr);
                    } else {
                        // 如果 deviceIds 为空数组，清空设备名称
                        taskInfo.setDeviceName("");
                        taskInfoService.updateTaskInfo(taskInfo);
                        log.info("[TaskUpdate] 任务设备名称已清空");
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
        //判断是否正在运行中
        for(Long taskId :ids){
            TaskInfo taskInfo = taskInfoService.selectTaskInfoById(taskId);
            if(taskInfo.getStatus().equals(TaskStatus.RUNNING.getCode())){
                return TcpResponse.error("任务正在运行中，无法删除！");
            }
        }

         int rows = taskInfoService.deleteTaskInfoByIds(ids);
        //删除后将相应的设备置为空闲，且故障的不做改变
        for (Long taskId : ids){
            List<TaskDeviceLink> links = taskDeviceLinkService.listByTaskId(taskId);
            if(ObjectUtils.isEmpty( links)){
                return TcpResponse.success(rows);
            }
            for (TaskDeviceLink link : links) {
                // 如果设备已经故障，则不处理
                if(link.getStatus().equals(TaskDeviceStatus.ERROR.getCode())){
                     deviceInfoService.removeCurrentTask(link.getDeviceId(),DeviceStatus.ERROR.getCode());
                }
                //移除设备任务,设置设备状态为空闲
                deviceInfoService.removeCurrentTask(link.getDeviceId(),DeviceStatus.ONLINE_IDLE.getCode());
            }
        }
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

    /** 列表查询（不分页格式，支持 name/status/poolId 筛选） */
    private TcpResponse list(String body) throws Exception {
         TaskInfo query = new TaskInfo();
        if (StringUtils.isNotEmpty(body)) {
            query = objectMapper.readValue(body, TaskInfo.class);
        }
        List<TaskInfo> data = taskInfoService.selectTaskInfoList(query);
        return TcpResponse.success(data);
    }


    /** 列表查询（分页格式，支持 name/status/poolId 过滤） */
    private TcpResponse pageList(String body) throws Exception {
        if (StringUtils.isEmpty(body)) {
            return TcpResponse.error("请求体不能为空");
        }

        // 解析请求参数
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});
        
        // 构建查询条件
        TaskInfo query = objectMapper.convertValue(params, TaskInfo.class);
        
        // 构建分页参数
        PageQuery pageQuery = new PageQuery();
        if (params.containsKey("pageNum")) {
            pageQuery.setPageNum((Integer) params.get("pageNum"));
        }
        if (params.containsKey("pageSize")) {
            pageQuery.setPageSize((Integer) params.get("pageSize"));
        }
        if (params.containsKey("orderByColumn")) {
            pageQuery.setOrderByColumn((String) params.get("orderByColumn"));
        }
        if (params.containsKey("isAsc")) {
            pageQuery.setIsAsc((String) params.get("isAsc"));
        }
        if (params.containsKey("reasonable")) {
            pageQuery.setReasonable((Boolean) params.get("reasonable"));
        }

        // 执行分页查询
        PageResult<TaskInfo> result = taskInfoService.selectTaskInfoPageList(query, pageQuery);
        
        log.info("[TaskInfoManagement] 任务列表查询成功，总数: {}, 当前页: {}, 每页: {}", 
            result.getTotal(), result.getPageNum(), result.getPageSize());
        return TcpResponse.success("查询成功", result);
    }

    /** 统计数量（支持过滤） */
    private TcpResponse count(String body) throws Exception {
        TaskInfo query = StringUtils.isEmpty(body) ? new TaskInfo() : objectMapper.readValue(body, TaskInfo.class);
        Integer cnt = taskInfoService.countTaskInfo(query);
        return TcpResponse.success(cnt);
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
        Long id = parseId(body);;
        TaskInfo taskInfo = taskInfoService.selectTaskInfoById(id);

        // 判断任务是否已经启动
        if(taskInfo.getStatus().equals(TaskStatus.RUNNING.getCode())){
            return TcpResponse.error("任务已经启动");
        }
        // 判断任务是否已经完成
        if(taskInfo.getStatus().equals(TaskStatus.COMPLETED.getCode())){
            return TcpResponse.error("任务已完成");
        }

        TaskDeviceLink query = new TaskDeviceLink();
        query.setTaskId(id);
        List<TaskDeviceLink> links = taskDeviceLinkService.list(query);
        if(ObjectUtils.isEmpty( links)){
            return TcpResponse.error("请先配置任务设备关联");
        }
        Long[] deviceIds = links.stream().map(TaskDeviceLink::getDeviceId).toArray(Long[]::new);
        // 创建调度请求
        TaskDispatchRequest request = new TaskDispatchRequest();
            request.setTaskId(taskInfo.getId());
            request.setDeviceIds(deviceIds);
            request.setPoolId(taskInfo.getPoolId());
            request.setPreloadCount(taskInfo.getPreloadDataCount());
            request.setBatchSize(1000);
            request.setTaskName(taskInfo.getName());
            request.setDescription(taskInfo.getDescription());
            request.setDeviceFileConfigId(links.get(0).getDeviceFileConfigId());
            request.setPoolTemplateId(links.get(0).getPoolTemplateId());
            request.setAssignedQuantity(taskInfo.getPreloadDataCount());
            request.setOriginalCount(taskInfo.getCompletedQuantity());
            request.setPrintCount(taskInfo.getPlannedQuantity());

        try {
            taskDispatcherService.startNewTask(request);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return TcpResponse.success("任务调度启动成功");
    }

    /** 停止任务调度 */
    private TcpResponse stopDispatch(String body) throws Exception {
        Long id = parseId(body);
        taskDispatcherService.stopTaskDispatch(id);
        return TcpResponse.success("任务调度停止成功");
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

    /** 今日计划/实际产量 **/
    private TcpResponse getTodayProductivity() {
        Map<String,Integer> result = new HashMap<>();
        //今日计划数量
        int plannedQuantity = 0;
        //实际生产数量
        int completedQuantity = 0;
        //查询所有任务
        List<TaskInfo> taskInfoList = taskInfoService.selectTaskInfoList(new TaskInfo());

        if(taskInfoList.isEmpty()){
            return TcpResponse.success(result);
        }
        for (TaskInfo taskInfo : taskInfoList){
            //为故障的任务直接跳过
            if(taskInfo.getStatus().equals(TaskStatus.ERROR.getCode())){
                continue;
            }
            //查询数据池表
            DataPool pool = dataPoolService.selectDataPoolById(taskInfo.getPoolId());
            if(taskInfo.getPlannedQuantity() == -1){
                plannedQuantity += pool.getTotalCount();
            }else {
                plannedQuantity += taskInfo.getPlannedQuantity();
            }
            completedQuantity += taskInfo.getCompletedQuantity();
        }
        //今日计划数量
        result.put("plannedQuantity",plannedQuantity);
        //实际生产数量
        result.put("completedQuantity",completedQuantity);

        return TcpResponse.success(result);
    }


    /**合格率**/
    private TcpResponse getQualityRate() {
       //合格率等于除去待打印的数据/成功数据
        //除去待打印数据
        int passedQuantity = 0;
        //已完成数据
        int failedQuantity = 0;
        //合格率
        double qualityRate;
        //查询所有任务
        List<TaskInfo> taskInfoList = taskInfoService.selectTaskInfoList(new TaskInfo());

        for (TaskInfo taskInfo : taskInfoList){
            //为故障的任务直接跳过
            if(taskInfo.getStatus().equals(TaskStatus.ERROR.getCode())){
               continue;
            }
            DataPool pool = dataPoolService.selectDataPoolById(taskInfo.getPoolId());
            if(ObjectUtils.isEmpty( pool)){
                continue;
            }
            //查询不包含待打印的数据
            passedQuantity += dataPoolItemService.countByNotPending(pool.getId());
            //查询成功的数据
            failedQuantity += dataPoolItemService.countByStatus(pool.getId(), ItemStatus.QC_COMPLETED.getCode());
        }
            //获取合格率保留四舍五入4位小数
            qualityRate= (double) failedQuantity /passedQuantity;
            qualityRate = Math.round(qualityRate * 10000) / 100.0;

        return TcpResponse.success(qualityRate);
    }

}



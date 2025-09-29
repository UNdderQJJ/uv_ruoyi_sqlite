package com.ruoyi.business.config;

import com.ruoyi.business.domain.DataInspect.DataInspect;
import com.ruoyi.business.domain.DataPoolItem.DataPoolItem;
import com.ruoyi.business.domain.TaskInfo.TaskDispatchStatus;
import com.ruoyi.business.enums.ItemStatus;
import com.ruoyi.business.service.DataInspect.IDataInspectService;
import com.ruoyi.business.service.DataPoolItem.IDataPoolItemService;
import com.ruoyi.business.service.TaskInfo.CommandQueueService;
import com.ruoyi.business.service.TaskInfo.ITaskDeviceLinkService;
import com.ruoyi.business.service.TaskInfo.ITaskInfoService;
import com.ruoyi.business.service.TaskInfo.SentRecord;
import com.ruoyi.business.domain.TaskInfo.DeviceTaskStatus;
import com.ruoyi.business.domain.TaskInfo.TaskDeviceLink;
import com.ruoyi.business.domain.TaskInfo.TaskInfo;
import com.ruoyi.business.service.TaskInfo.impl.TaskDispatcherServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调度器配置
 * 提供TaskScheduler用于动态创建定时任务
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10); // 设置线程池大小
        scheduler.setThreadNamePrefix("data-pool-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();
        return scheduler;
    }

    @Autowired
    @Lazy
    private TaskDispatcherServiceImpl taskDispatcherService;

    @Autowired
    private IDataInspectService dataInspectService;

    @Autowired
    private IDataPoolItemService dataPoolItemService;

    @Autowired
    private CommandQueueService commandQueueService;

    @Autowired
    private ITaskDeviceLinkService taskDeviceLinkService;

    @Autowired
    private ITaskInfoService taskInfoService;

    /**
     * 统一持久化：批量将缓冲数据一次性落库
     */
    @Scheduled(fixedRate = 2000)
    @Transactional(rollbackFor = Exception.class)
    public void persistTaskData() {
        // 抽取并清空完成计数缓冲（设备维度聚合）
        Map<String, Integer> countsToUpdate = taskDispatcherService.getAndClearCompletedBuffer();
        
        //    1. 更新数据项状态为 PRINTED，并写入 deviceId
        //    2. 批量插入 DataInspect 质检记录
        for (Long taskId : taskDispatcherService.getRunningTasks()) {
            List<SentRecord> sentRecords = commandQueueService.drainSentRecordsForTask(taskId);
            if (sentRecords == null || sentRecords.isEmpty()) {
                continue;
            }
            List<DataPoolItem> toUpdate = new ArrayList<>(sentRecords.size());
            List<DataInspect> toInsert = new ArrayList<>(sentRecords.size());
            for (SentRecord rec : sentRecords) {
                // 1) 构建数据项更新
                DataPoolItem item = new DataPoolItem();
                item.setId(rec.getDataPoolItemId());
                item.setDeviceId(rec.getDeviceId());
                toUpdate.add(item);

                // 2) 构建质检记录
                DataInspect inspect = new DataInspect();
                inspect.setItemId(rec.getDataPoolItemId());
                inspect.setItemData(rec.getDataPoolItemData());
                inspect.setPoolId(rec.getPoolId());
                inspect.setTaskId(taskId);
                inspect.setPrintDeviceId(Long.valueOf(rec.getDeviceId()));
                inspect.setPrintTime(rec.getPrintTime());
                inspect.setInspectStatus("PENDING");
                toInsert.add(inspect);
            }
            //批量更新数据项状态为PRINTED
            dataPoolItemService.updateDataPoolItemsStatus(toUpdate, ItemStatus.PRINTED.getCode());
            //批量插入质检记录
            dataInspectService.batchInsertDataInspect(toInsert);
        }

        // 基于完成计数，增量刷新 TaskDeviceLink 与 TaskInfo 的完成数量
        if (countsToUpdate != null && !countsToUpdate.isEmpty()) {
            // 按任务ID聚合完成数量
           Map<Long, Integer> taskIdToDelta = new HashMap<>();

            for (Map.Entry<String, Integer> e : countsToUpdate.entrySet()) {
                String deviceIdStr = e.getKey();
                Integer delta = e.getValue();
                if (delta == null || delta <= 0) {
                    continue;
                }
                Long taskId = taskDispatcherService.getDeviceTaskId(deviceIdStr);
                if (taskId == null) {
                    continue;
                }
                // 获取当前设备的在途打印数量
                DeviceTaskStatus deviceStatus = taskDispatcherService.getDeviceTaskStatus(deviceIdStr);
                // 更新 TaskDeviceLink.completedQuantity += delta
                // 同时更新 assignedQuantity（在途打印数量）和吞吐率
                TaskDeviceLink query = new TaskDeviceLink();
                query.setTaskId(taskId);
                query.setDeviceId(Long.valueOf(deviceIdStr));
                List<TaskDeviceLink> links = taskDeviceLinkService.list(query);
                if (links != null && !links.isEmpty()) {
                    TaskDeviceLink link = links.get(0);
                    
                    // 更新完成数量
                    Integer completedCount = link.getCompletedQuantity() == null ? 0 : link.getCompletedQuantity();
                    link.setCompletedQuantity(completedCount + delta);
                    


                    if (deviceStatus != null) {
                        // 更新在途打印数量（assignedQuantity）
                        link.setCachePoolSize(deviceStatus.getInFlightCount());

                        // 更新设备完成数量
                        link.setCompletedQuantity(deviceStatus.getCompletedCount());

                        //获取设备已接收的数量
                        deviceStatus.setReceivedCount( deviceStatus.getReceivedCount() +delta);
                        link.setReceivedQuantity(deviceStatus.getReceivedCount());

                        //获取当前完成数量
                        Integer currentCompletedCount = deviceStatus.getCurrentCompletedCount() == null ? 0 : deviceStatus.getCurrentCompletedCount();
                        deviceStatus.setCurrentCompletedCount(currentCompletedCount + delta);
                        // 计算并更新吞吐率（每秒完成数量）
                            long currentTime = System.currentTimeMillis();
                            long taskStartTime = taskDispatcherService.getTaskDispatchStatus(taskId).getStartTime();
                            long runningSeconds = Math.max(1, (currentTime - taskStartTime) / 1000); // 至少1秒
                            int throughput = (int) ((currentCompletedCount + delta) / runningSeconds);
                            link.setThroughput(throughput);

                    }
                    
                    taskDeviceLinkService.updateLink(link);
                }

                if (deviceStatus != null) {
                    taskIdToDelta.merge(taskId, deviceStatus.getReceivedCount(), Integer::sum);
                }
            }

            // 汇总后更新 TaskInfo.completedQuantity += sumDelta
            for (Map.Entry<Long, Integer> e : taskIdToDelta.entrySet()) {
                Long taskId = e.getKey();
                Integer delta = e.getValue();
                if (delta == null || delta <= 0) continue;
                TaskDispatchStatus status = taskDispatcherService.getTaskDispatchStatus(taskId);
                TaskInfo task = taskInfoService.selectTaskInfoById(taskId);
                if (task != null) {
                    Integer current = task.getReceivedQuantity() == null ? 0 : task.getReceivedQuantity();
                    TaskInfo toUpdateTask = new TaskInfo();
                    toUpdateTask.setId(taskId);
                    toUpdateTask.setCompletedQuantity(status.getSentCommandCount());
                    toUpdateTask.setReceivedQuantity(delta);
                    taskInfoService.updateTaskInfo(toUpdateTask);
                }
            }
        }

    }
}

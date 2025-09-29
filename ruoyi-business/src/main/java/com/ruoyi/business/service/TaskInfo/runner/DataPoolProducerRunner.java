package com.ruoyi.business.service.TaskInfo.runner;

import com.ruoyi.business.config.TaskDispatchProperties;
import com.ruoyi.business.domain.DataPoolTemplate.DataPoolTemplate;
import com.ruoyi.business.domain.DeviceFileConfig.DeviceFileConfig;
import com.ruoyi.business.domain.SystemLog.SystemLog;
import com.ruoyi.business.domain.TaskInfo.PrintCommand;
import com.ruoyi.business.domain.DataPoolItem.DataPoolItem;
import com.ruoyi.business.enums.ItemStatus;
import com.ruoyi.business.enums.SystemLogLevel;
import com.ruoyi.business.enums.SystemLogType;
import com.ruoyi.business.service.DataPoolItem.IDataPoolItemService;
import com.ruoyi.business.service.DataPoolTemplate.IDataPoolTemplateService;
import com.ruoyi.business.service.DeviceFileConfig.IDeviceFileConfigService;
import com.ruoyi.business.service.SystemLog.ISystemLogService;
import com.ruoyi.business.service.TaskInfo.ITaskDeviceLinkService;
import com.ruoyi.business.service.TaskInfo.CommandQueueService;
import com.ruoyi.business.domain.TaskInfo.TaskDeviceLink;
import com.ruoyi.business.enums.PrintCommandStatusEnum;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 数据生成池运行器
 * 负责从数据源高效地准备打印指令
 */
public class DataPoolProducerRunner implements Runnable {
    
    private static final Logger log = LoggerFactory.getLogger(DataPoolProducerRunner.class);
    
    private final Long taskId;
    private final Long poolId;
    private final CommandQueueService commandQueueService;
    private final IDataPoolItemService dataPoolItemService;
    private final ITaskDeviceLinkService taskDeviceLinkService;
    private final IDataPoolTemplateService iDataPoolTemplateService;
    private final IDeviceFileConfigService iDeviceFileConfigService;
    private final TaskDispatchProperties taskDispatchProperties;
    private final ISystemLogService systemLogService;
    
    private volatile boolean running = true;
    private volatile boolean paused = false;
    private final AtomicInteger producedCount = new AtomicInteger(0);
    private final AtomicInteger processedCount = new AtomicInteger(0);
    
    // 计划数管理器 - 线程安全的计划数控制
    private final AtomicInteger planTargetCount = new AtomicInteger(0); // 计划目标总数
    private final AtomicInteger planGeneratedCount = new AtomicInteger(0); // 已生成数量
    private final AtomicInteger planRemainingCount = new AtomicInteger(0); // 剩余可生成数量
    private final AtomicBoolean planInitialized = new AtomicBoolean(false); // 计划是否已初始化
    // 每个任务的计划数快照，避免被全局配置覆盖
    private final int planPrintCountSnapshot; // 启动时的计划打印总数
    private final int originalCountSnapshot;  // 启动时的已完成数量
    
    public DataPoolProducerRunner(Long taskId, Long poolId,
                                  CommandQueueService commandQueueService,
                                  IDataPoolItemService dataPoolItemService,
                                  ITaskDeviceLinkService taskDeviceLinkService, 
                                  IDataPoolTemplateService iDataPoolTemplateService, 
                                  IDeviceFileConfigService iDeviceFileConfigService,
                                  TaskDispatchProperties taskDispatchProperties,
                                  int planPrintCountSnapshot,
                                  int originalCountSnapshot,
                                  ISystemLogService systemLogService ) {
        this.taskId = taskId;
        this.poolId = poolId;
        this.commandQueueService = commandQueueService;
        this.dataPoolItemService = dataPoolItemService;
        this.taskDeviceLinkService = taskDeviceLinkService;
        this.iDataPoolTemplateService = iDataPoolTemplateService;
        this.iDeviceFileConfigService = iDeviceFileConfigService;
        this.taskDispatchProperties = taskDispatchProperties;
        this.planPrintCountSnapshot = planPrintCountSnapshot;
        this.originalCountSnapshot = originalCountSnapshot;
        this.systemLogService = systemLogService;
    }
    
    @Override
    public void run() {
        log.info("数据生成池启动，任务ID: {}, 数据池ID: {}", taskId, poolId);

        // 初始化计划数管理器
        initializePlanManager();
        
        while (running) {
            try {
                if (paused) {
                    Thread.sleep(500); // 暂停时等待500ms
                    continue;
                }
                
                // 检查是否已达到计划目标
                if (isPlanTargetReached()) {
                    log.info("计划打印数量已满，停止生成，任务ID: {}, 数据池ID: {}, 已生成: {}, 计划目标: {}", 
                            taskId, poolId, planGeneratedCount.get(), planTargetCount.get());
                    logPlanCompletion();
                    break;
                }
                
                    fetchAndProcessData();
                Thread.sleep(500); // 避免CPU占用过高
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("数据生成池被中断，任务ID: {}", taskId);
                throw new RuntimeException(e.getMessage());
            } catch (Exception e) {
                log.error("数据生产异常，任务ID: {}", taskId, e);
                // 这里可以通过事件发布错误，而不是直接调用dispatcher
                try {
                    Thread.sleep(5000); // 异常后等待5秒再重试
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
        log.info("数据生成池停止，任务ID: {}, 总生产数量: {}", taskId, producedCount.get());
    }
    
    /**
     * 获取并处理数据
     */
    private void fetchAndProcessData() {
        try {
            // 检查队列大小，避免队列过满
            int queueSize = commandQueueService.getQueueSize(taskId);
            int maxQueueSize = taskDispatchProperties.getCommandQueueSize();

            if (queueSize > maxQueueSize * 0.5) { // 队列超过50%时暂停生产
                log.debug("指令队列已满，暂停生产，任务ID: {}, 队列大小: {}, 最大容量: {}", taskId, queueSize, maxQueueSize);
                return;
            }
            
            // 计算本次可查询的数据量（考虑计划数限制）
            int queryBatchSize = calculateQueryBatchSize();
            if (queryBatchSize <= 0) {
                log.debug("已达到计划目标或无可查询数据，任务ID: {}", taskId);
                return;
            }
            
            // 批量查询待打印数据
            List<DataPoolItem> items = dataPoolItemService.selectPendingItems(poolId, queryBatchSize);
            
            if (items == null || items.isEmpty()) {
                log.debug("没有待处理数据，任务ID: {}", taskId);
                return;
            }

            // 根据计划数限制实际处理的数据量
            List<DataPoolItem> itemsToProcess = limitItemsByPlan(items);
            if (itemsToProcess.isEmpty()) {
                log.debug("受计划数限制，无数据可处理，任务ID: {}", taskId);
                return;
            }

            //更新成打印中
            dataPoolItemService.updateItemsStatus(itemsToProcess, ItemStatus.PRINTING.getCode());
            
            log.debug("获取到 {} 条待处理数据，实际处理 {} 条，任务ID: {}", items.size(), itemsToProcess.size(), taskId);
            
            // 获取任务关联的设备信息
            TaskDeviceLink query = new TaskDeviceLink();
            query.setTaskId(taskId);
            List<TaskDeviceLink> deviceLinks = taskDeviceLinkService.list(query);
            
            if (deviceLinks == null || deviceLinks.isEmpty()) {
                log.warn("任务没有关联设备，任务ID: {}", taskId);
                return;
            }

            //查询数据池模板
            DataPoolTemplate template = iDataPoolTemplateService.selectDataPoolTemplateById(deviceLinks.get(0).getPoolTemplateId());
            if(ObjectUtils.isEmpty(template)){
                logTemplateError("当前数据池模版为空!");
                throw new RuntimeException("当前数据池模版为空!");
            }
            
            //查询设备模版
            DeviceFileConfig fileConfig = iDeviceFileConfigService.selectDeviceFileConfigById(deviceLinks.get(0).getDeviceFileConfigId());
            if(ObjectUtils.isEmpty(fileConfig)) {
                logTemplateError("当前设备模版为空!");
                throw new RuntimeException("当前设备模版为空!");
            }

            // 处理每条数据 - 每条数据只生成一个打印指令
            int generatedThisBatch = 0;
            for (DataPoolItem item : itemsToProcess) {
                if (!running || paused) {
                    break;
                }
                
                // 检查是否已达到计划目标
                if (isPlanTargetReached()) {
                    log.debug("处理过程中达到计划目标，停止处理，任务ID: {}", taskId);
                    break;
                }
                
                // 为数据创建打印指令（不指定设备，由调度器分配）
                PrintCommand command = buildPrintCommand(item, template, fileConfig);
                if (command != null) {
                    commandQueueService.addCommandToQueue(command);
                    producedCount.incrementAndGet();
                    planGeneratedCount.incrementAndGet();
                    planRemainingCount.decrementAndGet();
                    generatedThisBatch++;
                    
                    // 防止队列逼近容量上限
                    if (commandQueueService.getQueueSize(taskId) >= maxQueueSize) {
                        break;
                    }
                }
                
                processedCount.incrementAndGet();
            }
            
            //记录打印日志
            log.info("本轮生成指令数量: {}, 累计生成: {}, 剩余计划: {}, 任务ID: {}", 
                    generatedThisBatch, planGeneratedCount.get(), planRemainingCount.get(), taskId);
            
        } catch (Exception e) {
            log.error("处理数据异常，任务ID: {}", taskId, e);
            logTemplateError("指令生成异常:" + e.getMessage());
            throw new RuntimeException("指令生成异常:" + e.getMessage());
        }
    }
    
    /**
     * 初始化计划数管理器
     */
    private void initializePlanManager() {
        int planPrintCount = this.planPrintCountSnapshot;
        int originalCount = this.originalCountSnapshot;
        
        if (planPrintCount != -1) {
            // 有具体计划数
            planTargetCount.set(planPrintCount);
            planGeneratedCount.set(originalCount);
            planRemainingCount.set(Math.max(0, planPrintCount - originalCount));
            planInitialized.set(true);
            
            log.info("计划数管理器初始化 - 任务ID: {}, 计划目标: {}, 已完成: {}, 剩余: {}", 
                    taskId, planTargetCount.get(), planGeneratedCount.get(), planRemainingCount.get());
        } else {
            // 无计划数限制
            planTargetCount.set(Integer.MAX_VALUE);
            planGeneratedCount.set(0);
            planRemainingCount.set(Integer.MAX_VALUE);
            planInitialized.set(true);
            
            log.info("计划数管理器初始化 - 任务ID: {}, 无计划数限制", taskId);
        }
    }
    
    /**
     * 检查是否已达到计划目标
     */
    private boolean isPlanTargetReached() {
        if (!planInitialized.get()) {
            return false;
        }
        
        if (planTargetCount.get() == Integer.MAX_VALUE) {
            return false; // 无计划数限制
        }
        
        return planRemainingCount.get() <= 0;
    }
    
    /**
     * 计算本次查询的批次大小
     */
    private int calculateQueryBatchSize() {
        int baseBatchSize = taskDispatchProperties.getBatchSize();
        
        if (!planInitialized.get()) {
            return baseBatchSize;
        }
        
        if (planTargetCount.get() == Integer.MAX_VALUE) {
            // 无计划数限制，使用基础批次大小
            return baseBatchSize;
        }
        
        // 有计划数限制，取剩余数量和批次大小的较小值
        int remaining = planRemainingCount.get();
        return Math.min(remaining, baseBatchSize);
    }
    
    /**
     * 根据计划数限制实际处理的数据量
     */
    private List<DataPoolItem> limitItemsByPlan(List<DataPoolItem> items) {
        if (!planInitialized.get() || planTargetCount.get() == Integer.MAX_VALUE) {
            return items; // 无计划数限制，返回全部
        }
        
        int remaining = planRemainingCount.get();
        if (remaining <= 0) {
            return new ArrayList<>(); // 已达到计划目标
        }
        
        if (items.size() <= remaining) {
            return items; // 数据量不超过剩余计划数
        }
        
        // 截取到剩余计划数的数量
        return items.subList(0, remaining);
    }
    
    /**
     * 记录计划完成日志
     */
    private void logPlanCompletion() {
        SystemLog systemLog = new SystemLog();
        systemLog.setLogType(SystemLogType.PRINT.getCode());
        systemLog.setLogLevel(SystemLogLevel.INFO.getCode());
        systemLog.setTaskId(taskId);
        systemLog.setPoolId(poolId);
        systemLog.setContent(String.format("计划打印数量已满，停止生成。已生成: %d, 计划目标: %d", 
                planGeneratedCount.get(), planTargetCount.get()));
        systemLogService.insert(systemLog);
    }
    
    /**
     * 记录模板错误日志
     */
    private void logTemplateError(String message) {
        SystemLog systemLog = new SystemLog();
        systemLog.setLogType(SystemLogType.PRINT.getCode());
        systemLog.setLogLevel(SystemLogLevel.ERROR.getCode());
        systemLog.setTaskId(taskId);
        systemLog.setPoolId(poolId);
        systemLog.setContent(message);
        systemLogService.insert(systemLog);
    }
    
    /**
     * 构建打印指令（不指定设备，由调度器动态分配）
     */
    private PrintCommand buildPrintCommand(DataPoolItem item, DataPoolTemplate template,DeviceFileConfig fileConfig) {
        try {
            PrintCommand command = new PrintCommand();
            // 使用数据项ID作为指令ID，便于关联数据库项
            command.setId(String.valueOf(item.getId()));
            command.setTaskId(taskId);
            // deviceId 由调度器动态分配，这里不设置
            command.setData(item.getItemData());
            command.setStatus(PrintCommandStatusEnum.PENDING.getCode());
            command.setCreateTime(System.currentTimeMillis());
            command.setRetryCount(0);
            command.setMaxRetryCount(3);
            command.setPriority(1);
            
            // 构建打印指令模板,使用设备的模版信息都相同
            String printCommand = buildGenericCommand(item,template,fileConfig);
            command.setCommand(printCommand);
            
            return command;
            
        } catch (Exception e) {
            log.error("构建打印指令失败，数据项ID: {}", item.getId(), e);
            return null;
        }
    }
    
    /**
     * 构建打印指令模板（不包含设备）
     */
    private String buildGenericCommand(DataPoolItem item, DataPoolTemplate template,DeviceFileConfig fileConfig) {
        // 构建的指令模板，设备特定信息由调度器在发送时添加
        // 格式：seta:data#v1=text+size#width|height+pos#x|y|r|ng
        Integer xAxis = template.getXAxis();// X轴坐标
        Integer yAxis = template.getYAxis();// Y轴坐标
        Integer angle = template.getAngle();// 旋转角度
        Integer width = template.getWidth();// 宽度
        Integer height = template.getHeight();// 高度
        StringBuilder command = new StringBuilder();
        command.append("seta:data#").append(fileConfig.getVariableName()).append("=").append(item.getItemData())
                .append("+size#").append(width).append("|").append( height)
                .append("+pos#").append(xAxis).append("|").append(yAxis).append("|").append(angle).append("|").append("0");
        
        return command.toString();
    }
    
    /**
     * 停止生产
     */
    public void stop() {
        this.running = false;
        log.info("数据生成池停止请求，任务ID: {}", taskId);
    }
    
    /**
     * 暂停生产
     */
    public void pause() {
        this.paused = true;
        log.info("数据生成池暂停，任务ID: {}", taskId);
    }
    
    /**
     * 恢复生产
     */
    public void resume() {
        this.paused = false;
        log.info("数据生成池恢复，任务ID: {}", taskId);
    }
    
    /**
     * 获取已生产数量
     */
    public int getProducedCount() {
        return producedCount.get();
    }
    
    /**
     * 获取已处理数量
     */
    public int getProcessedCount() {
        return processedCount.get();
    }
    
    /**
     * 是否正在运行
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * 是否已暂停
     */
    public boolean isPaused() {
        return paused;
    }
    
    /**
     * 获取计划目标数量
     */
    public int getPlanTargetCount() {
        return planTargetCount.get();
    }
    
    /**
     * 获取已生成数量
     */
    public int getPlanGeneratedCount() {
        return planGeneratedCount.get();
    }
    
    /**
     * 获取剩余计划数量
     */
    public int getPlanRemainingCount() {
        return planRemainingCount.get();
    }
    
    /**
     * 是否已初始化计划数管理器
     */
    public boolean isPlanInitialized() {
        return planInitialized.get();
    }
    
    /**
     * 获取计划完成百分比
     */
    public double getPlanCompletionPercentage() {
        if (!planInitialized.get() || planTargetCount.get() == Integer.MAX_VALUE) {
            return 0.0;
        }
        
        if (planTargetCount.get() == 0) {
            return 100.0;
        }
        
        return (double) planGeneratedCount.get() / planTargetCount.get() * 100.0;
    }
}

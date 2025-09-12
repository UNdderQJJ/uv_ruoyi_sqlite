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
import com.ruoyi.business.config.TaskDispatchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
    
    public DataPoolProducerRunner(Long taskId, Long poolId,
                                  CommandQueueService commandQueueService,
                                  IDataPoolItemService dataPoolItemService,
                                  ITaskDeviceLinkService taskDeviceLinkService, 
                                  IDataPoolTemplateService iDataPoolTemplateService, 
                                  IDeviceFileConfigService iDeviceFileConfigService,
                                  TaskDispatchProperties taskDispatchProperties,
                                  ISystemLogService systemLogService ) {
        this.taskId = taskId;
        this.poolId = poolId;
        this.commandQueueService = commandQueueService;
        this.dataPoolItemService = dataPoolItemService;
        this.taskDeviceLinkService = taskDeviceLinkService;
        this.iDataPoolTemplateService = iDataPoolTemplateService;
        this.iDeviceFileConfigService = iDeviceFileConfigService;
        this.taskDispatchProperties = taskDispatchProperties;
        this.systemLogService = systemLogService;
    }
    
    @Override
    public void run() {
        log.info("数据生成池启动，任务ID: {}, 数据池ID: {}", taskId, poolId);

          int printCount = taskDispatchProperties.getPlanPrintCount();// 打印数量
        
        while (running) {
            //如果是计划打印，数据池队列等于计划打印时这停止生成
            if (printCount != -1 && commandQueueService.getQueueSize(taskId) >= printCount) {
                log.info("计划打印数量已满，停止生成，任务ID: {}, 数据池ID: {}", taskId, poolId);
                //记录打印日志
                SystemLog systemLog = new SystemLog();
                systemLog.setLogType(SystemLogType.PRINT.getCode());
                systemLog.setLogLevel(SystemLogLevel.INFO.getCode());
                systemLog.setTaskId(taskId);
                systemLog.setPoolId(poolId);
                systemLog.setContent("计划打印数量已满，停止生成,数量:"+commandQueueService.getQueueSize(taskId));
                systemLogService.insert(systemLog);
                break;
            }
            try {
                if (paused) {
                    Thread.sleep(1000); // 暂停时等待1秒
                    continue;
                }
                fetchAndProcessData();
                Thread.sleep(2000); // 避免CPU占用过高
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("数据生成池被中断，任务ID: {}", taskId);
                break;
                            } catch (Exception e) {
                    log.error("数据生产异常，任务ID: {}", taskId, e);
                    // 这里可以通过事件发布错误，而不是直接调用dispatcher
                    try {
                        Thread.sleep(5000); // 异常后等待5秒再重试
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
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
            int queueSize = commandQueueService.getQueueSize();
            int maxQueueSize = taskDispatchProperties.getCommandQueueSize();
            int printCount = taskDispatchProperties.getPlanPrintCount();// 打印数量
            if(printCount == -1){
                printCount = (int) Math.floor(taskDispatchProperties.getBatchSize()*0.2);
            }

            if (queueSize > maxQueueSize * 0.5) { // 队列超过50%时暂停生产
                log.debug("指令队列已满，暂停生产，任务ID: {}, 队列大小: {}, 最大容量: {}", taskId, queueSize, maxQueueSize);
                return;
            }
            
            // 批量查询待打印数据
            List<DataPoolItem> items = dataPoolItemService.selectPendingItems(poolId, printCount);
            
            if (items == null || items.isEmpty()) {
                log.debug("没有待处理数据，任务ID: {}", taskId);
                return;
            }

            //更新成打印中
            dataPoolItemService.updateItemsStatus(items,ItemStatus.PRINTING.getCode());
            
            log.debug("获取到 {} 条待处理数据，任务ID: {}", items.size(), taskId);
            
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
            //查询设备模版
            DeviceFileConfig fileConfig = iDeviceFileConfigService.selectDeviceFileConfigById(deviceLinks.get(0).getDeviceFileConfigId());
            
            // 处理每条数据 - 每条数据只生成一个打印指令
            for (DataPoolItem item : items) {
                if (!running || paused) {
                    break;
                }
                
                // 为数据创建打印指令（不指定设备，由调度器分配）
                PrintCommand command = buildPrintCommand(item, template, fileConfig);
                if (command != null) {
                    commandQueueService.addCommandToQueue(command);
                    producedCount.incrementAndGet();
                }
                
                processedCount.incrementAndGet();
            }
            //记录打印日志
                SystemLog systemLog = new SystemLog();
                systemLog.setLogType(SystemLogType.PRINT.getCode());
                systemLog.setLogLevel(SystemLogLevel.INFO.getCode());
                systemLog.setTaskId(taskId);
                systemLog.setPoolId(poolId);
                systemLog.setContent("指令生成数量:"+producedCount.get());
                systemLogService.insert(systemLog);
            
        } catch (Exception e) {
            log.error("处理数据异常，任务ID: {}", taskId, e);
                //记录打印日志
                SystemLog systemLog = new SystemLog();
                systemLog.setLogType(SystemLogType.PRINT.getCode());
                systemLog.setLogLevel(SystemLogLevel.INFO.getCode());
                systemLog.setTaskId(taskId);
                systemLog.setPoolId(poolId);
                systemLog.setContent("指令生成异常:"+e.getMessage());
                systemLogService.insert(systemLog);
            throw e;
        }
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
}

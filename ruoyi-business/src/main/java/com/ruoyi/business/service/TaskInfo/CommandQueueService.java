package com.ruoyi.business.service.TaskInfo;

import com.ruoyi.business.domain.TaskInfo.PrintCommand;
import java.util.List;

/**
 * 指令队列服务接口
 * 用于解耦数据生成池和调度中心
 */
public interface CommandQueueService {
    
    /**
     * 添加指令到队列
     * 
     * @param command 打印指令
     */
    void addCommandToQueue(PrintCommand command);
    
    /**
     * 获取下一个指令
     * 
     * @return 打印指令
     * @throws InterruptedException 中断异常
     */
    PrintCommand getNextCommand() throws InterruptedException;
    
    /**
     * 获取队列大小
     * 
     * @return 队列大小
     */
    int getQueueSize();

    /*
     * 获取当前任务的队列大小
     */
    int getQueueSize(Long taskId);
    
    /**
     * 清空队列
     */
    void clearQueue();

    /**
     * 获取当前队列中所有指令的快照（不会移除）
     */
    List<PrintCommand> getAllCommandsSnapshot();
    
    /**
     * 从队列中移除指定指令
     * 
     * @param command 要移除的指令
     * @return 是否移除成功
     */
    boolean removeCommand(PrintCommand command);
    
    /**
     * 记录已发送的数据项（轻量通道）
     *
     * @param taskId 任务ID
     * @param dataPoolItemId 数据项ID
     * @param deviceId 设备ID
     */
    void addSentRecord(Long taskId, Long dataPoolItemId, String deviceId);

    /**
     * 兼容方法（无设备ID），等价于 deviceId=null
     */
    default void addSentRecord(Long taskId, Long dataPoolItemId) {
        addSentRecord(taskId, dataPoolItemId, null);
    }

    /**
     * 针对指定任务，获取并移除其已发送记录（原子方式抽取）
     *
     * @param taskId 任务ID
     * @return 该任务的已发送记录（包含数据项ID与设备ID）
     */
    List<SentRecord> drainSentRecordsForTask(Long taskId);
}

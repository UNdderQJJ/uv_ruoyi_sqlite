package com.ruoyi.business.service.TaskInfo.impl;

import com.ruoyi.business.domain.TaskInfo.PrintCommand;
import com.ruoyi.business.service.TaskInfo.CommandQueueService;
import com.ruoyi.business.config.TaskDispatchProperties;
import com.ruoyi.business.service.TaskInfo.SentRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 指令队列服务实现
 * 提供线程安全的指令队列管理
 */
@Service
public class CommandQueueServiceImpl implements CommandQueueService {
    
    @Autowired
    private TaskDispatchProperties taskDispatchProperties;
    
    // 指令缓冲池 - 生成池消费者桥梁
    private BlockingQueue<PrintCommand> commandQueue;

    // 新增：使用线程安全的Set来追踪已在队列中的指令数据，确保唯一性
    // 我们假设用 PrintCommand 的 data 字段作为唯一标识
    private final Set<String> queuedCommandDataSet = ConcurrentHashMap.newKeySet();
    
    // 已发送记录（轻量：按任务分桶，含deviceId）
    private final ConcurrentHashMap<Long, BlockingQueue<SentRecord>> taskIdToSentRecords = new ConcurrentHashMap<>();

    /**
     * 初始化队列
     */
    private void initQueues() {
        if (commandQueue == null) {
            int queueSize = taskDispatchProperties.getCommandQueueSize();
            commandQueue = new LinkedBlockingQueue<>(queueSize);
        }
    }
    
   @Override
public void addCommandToQueue(PrintCommand command) {
    initQueues();

    // 假设 command.getData() 是指令的唯一标识 (例如打印的SN码)
    // queuedCommandDataSet.add() 是一个原子操作，如果数据已存在，它会返回false
    if (queuedCommandDataSet.add(command.getData())) {
        // ---- 如果添加成功，说明这是新指令 ----
        try {
            // 尝试将指令对象放入队列
            boolean success = commandQueue.offer(command, 1, TimeUnit.SECONDS);

            if (!success) {
                // 如果队列已满导致添加失败，我们必须把刚刚添加到Set中的标识也移除，以保证数据一致性
                queuedCommandDataSet.remove(command.getData());
                System.err.println("指令队列已满，丢弃指令: " + command.getId());
            }
        } catch (InterruptedException e) {
            // 如果在等待入队时被中断，同样需要移除Set中的标识
            queuedCommandDataSet.remove(command.getData());
            Thread.currentThread().interrupt();
            System.err.println("添加指令到队列被中断");
        }
    } else {
        // ---- 如果添加失败，说明Set中已存在该数据，是重复指令 ----
        System.err.println("检测到重复指令，已忽略: " + command.getData());
    }
}
    
    @Override
    public PrintCommand getNextCommand(Long taskId){
        initQueues();
        PrintCommand command = commandQueue.stream().filter(command1 -> command1.getTaskId().equals(taskId))
                .findFirst().orElse( null);
         if (command != null) {
        // 关键步骤：一旦指令被成功取出，就将其唯一标识从追踪Set中移除
        // 这样，后续相同数据的新指令就可以被再次添加进来
        queuedCommandDataSet.remove(command.getData());
         //移除已发送缓存池数据
         removeCommand(command);
    }
          return command;
    }
    
    @Override
    public int getQueueSize() {
        initQueues();
        return commandQueue.size();
    }

    @Override
    public int getQueueSize(Long taskId) {
        return commandQueue.stream().filter(command -> command.getTaskId().equals(taskId)).toArray().length;
    }

    @Override
    public void clearQueue() {
        initQueues();
        commandQueue.clear();
    }

    @Override
    public void clearQueue(Long taskId) {
        initQueues();
        commandQueue.removeIf(command -> command.getTaskId().equals(taskId));
    }

    @Override
    public List<PrintCommand> getAllCommandsSnapshot() {
        initQueues();
        return new ArrayList<>(commandQueue);
    }
    
    @Override
    public boolean removeCommand(PrintCommand command) {
        if (command == null) {
            return false;
        }
        initQueues();
        return commandQueue.remove(command);
    }

    @Override
    public void addSentRecord(Long taskId, Long dataPoolItemId,String dataPoolItemData, String deviceId,Long poolId) {
        if (taskId == null || dataPoolItemId == null) {
            return;
        }
        int queueSize = taskDispatchProperties.getCommandQueueSize();
        BlockingQueue<SentRecord> q = taskIdToSentRecords.computeIfAbsent(taskId, k -> new LinkedBlockingQueue<>(queueSize));
        // 非阻塞插入，满则丢弃并记录
        boolean ok = q.offer(new SentRecord(taskId, dataPoolItemId,dataPoolItemData, deviceId,poolId));
        if (!ok) {
            System.err.println("已发送记录队列满，taskId=" + taskId + ", 丢弃ID=" + dataPoolItemId);
        }
    }

    @Override
    public void addSentRecord(Long taskId, Long dataPoolItemId) {
        addSentRecord(taskId, dataPoolItemId, null,null,null);
    }

    @Override
    public List<SentRecord> drainSentRecordsForTask(Long taskId) {
        BlockingQueue<SentRecord> q = taskIdToSentRecords.get(taskId);
        if (q == null || q.isEmpty()) return Collections.emptyList();
        List<SentRecord> list = new ArrayList<>(q.size());
        q.drainTo(list);
        return list;
    }
}

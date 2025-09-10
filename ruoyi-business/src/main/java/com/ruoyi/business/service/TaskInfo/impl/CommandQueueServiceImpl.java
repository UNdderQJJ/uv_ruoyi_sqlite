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
        try {
            boolean success = commandQueue.offer(command, 1, TimeUnit.SECONDS);
            if (!success) {
                // 队列已满，可以选择丢弃或者记录日志
                System.err.println("指令队列已满，丢弃指令: " + command.getId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("添加指令到队列被中断");
        }
    }
    
    @Override
    public PrintCommand getNextCommand() throws InterruptedException {
        initQueues();
        return commandQueue.take();
    }
    
    @Override
    public int getQueueSize() {
        initQueues();
        return commandQueue.size();
    }
    
    @Override
    public void clearQueue() {
        initQueues();
        commandQueue.clear();
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
    public void addSentRecord(Long taskId, Long dataPoolItemId, String deviceId) {
        if (taskId == null || dataPoolItemId == null) {
            return;
        }
        int queueSize = taskDispatchProperties.getCommandQueueSize();
        BlockingQueue<SentRecord> q = taskIdToSentRecords.computeIfAbsent(taskId, k -> new LinkedBlockingQueue<>(queueSize));
        // 非阻塞插入，满则丢弃并记录
        boolean ok = q.offer(new SentRecord(taskId, dataPoolItemId, deviceId));
        if (!ok) {
            System.err.println("已发送记录队列满，taskId=" + taskId + ", 丢弃ID=" + dataPoolItemId);
        }
    }

    @Override
    public void addSentRecord(Long taskId, Long dataPoolItemId) {
        addSentRecord(taskId, dataPoolItemId, null);
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

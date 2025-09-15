package com.ruoyi.business.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 任务调度配置属性
 */
@Data
@ConfigurationProperties(prefix = "task.dispatch")
public class TaskDispatchProperties {
    
    /** 批处理大小 */
    private Integer batchSize = 1000;
    
    /** 预加载数量 */
    private Integer preloadCount = 20;
    
    /** 心跳超时时间（毫秒） */
    private Long heartbeatTimeout = 6000L;
    
    /** 最大重试次数 */
    private Integer maxRetryCount = 3;
    
    /** 指令队列大小 */
    private Integer commandQueueSize = 1000;

    /** 计划打印数量 */
    private Integer planPrintCount = 0;

    /** 原已完成数量 */
    private Integer originalCount = 0;
    
    /** 线程池配置 */
    private ThreadPoolConfig threadPool = new ThreadPoolConfig();
    
    @Data
    public static class ThreadPoolConfig {
        
        /** 生成池线程池配置 */
        private PoolConfig producer = new PoolConfig();
        
        /** 发送者线程池配置 */
        private PoolConfig sender = new PoolConfig();
        
        /** 处理器线程池配置 */
        private PoolConfig handler = new PoolConfig();
        
        @Data
        public static class PoolConfig {
            /** 核心线程数 */
            private Integer coreSize = 1;
            
            /** 最大线程数 */
            private Integer maxSize = 1;
            
            /** 队列容量 */
            private Integer queueCapacity = 10;
            
            /** 线程存活时间（秒） */
            private Integer keepAliveSeconds = 60;
        }
    }
}

package com.ruoyi.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * 数据库操作重试工具类
 * 用于处理SQLite数据库锁定等临时性错误
 * 
 * @author ruoyi
 */
public class DatabaseRetryUtil {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseRetryUtil.class);
    
    /**
     * 默认重试次数
     */
    private static final int DEFAULT_MAX_RETRIES = 3;
    
    /**
     * 默认重试间隔（毫秒）
     */
    private static final long DEFAULT_RETRY_INTERVAL = 100;
    
    /**
     * 执行带重试的数据库操作
     * 
     * @param operation 数据库操作
     * @param <T> 返回类型
     * @return 操作结果
     * @throws Exception 操作失败异常
     */
    public static <T> T executeWithRetry(Supplier<T> operation) throws Exception {
        return executeWithRetry(operation, DEFAULT_MAX_RETRIES, DEFAULT_RETRY_INTERVAL);
    }
    
    /**
     * 执行带重试的数据库操作
     * 
     * @param operation 数据库操作
     * @param maxRetries 最大重试次数
     * @param retryInterval 重试间隔（毫秒）
     * @param <T> 返回类型
     * @return 操作结果
     * @throws Exception 操作失败异常
     */
    public static <T> T executeWithRetry(Supplier<T> operation, int maxRetries, long retryInterval) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                
                // 检查是否是可重试的异常
                if (isRetryableException(e) && attempt < maxRetries) {
                    log.warn("数据库操作失败，第{}次重试，异常: {}", attempt + 1, e.getMessage());
                    
                    try {
                        Thread.sleep(retryInterval * (attempt + 1)); // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试被中断", ie);
                    }
                } else {
                    break;
                }
            }
        }
        
        log.error("数据库操作重试{}次后仍然失败", maxRetries);
        throw lastException;
    }
    
    /**
     * 执行无返回值的带重试的数据库操作
     * 
     * @param operation 数据库操作
     * @throws Exception 操作失败异常
     */
    public static void executeWithRetry(Runnable operation) throws Exception {
        executeWithRetry(() -> {
            operation.run();
            return null;
        });
    }
    
    /**
     * 执行无返回值的带重试的数据库操作
     * 
     * @param operation 数据库操作
     * @param maxRetries 最大重试次数
     * @param retryInterval 重试间隔（毫秒）
     * @throws Exception 操作失败异常
     */
    public static void executeWithRetry(Runnable operation, int maxRetries, long retryInterval) throws Exception {
        executeWithRetry(() -> {
            operation.run();
            return null;
        }, maxRetries, retryInterval);
    }
    
    /**
     * 判断异常是否可重试
     * 
     * @param e 异常
     * @return 是否可重试
     */
    private static boolean isRetryableException(Exception e) {
        if (e instanceof SQLException) {
            SQLException sqlException = (SQLException) e;
            String message = sqlException.getMessage();
            
            // SQLite特定的可重试错误
            return message != null && (
                message.contains("database is locked") ||
                message.contains("database table is locked") ||
                message.contains("busy") ||
                message.contains("timeout") ||
                message.contains("SQLITE_BUSY") ||
                message.contains("SQLITE_LOCKED")
            );
        }
        
        // 其他可重试的异常类型
        return e instanceof RuntimeException && 
               e.getMessage() != null && 
               e.getMessage().contains("database is locked");
    }
}

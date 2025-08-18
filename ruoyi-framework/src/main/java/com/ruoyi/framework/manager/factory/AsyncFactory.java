package com.ruoyi.framework.manager.factory;

import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 异步工厂（简化版本）
 * 
 * @author ruoyi
 */
public class AsyncFactory
{
    private static final Logger sys_user_logger = LoggerFactory.getLogger("sys-user");

    /**
     * 记录日志信息
     * 
     * @param username 用户名
     * @param status 状态
     * @param message 消息
     * @return 任务task
     */
    public static TimerTask recordLogininfor(final String username, final String status, final String message)
    {
        return new TimerTask()
        {
            @Override
            public void run()
            {
                // 简化实现，只记录到日志
                sys_user_logger.info("用户: {}, 状态: {}, 消息: {}", username, status, message);
            }
        };
    }
}
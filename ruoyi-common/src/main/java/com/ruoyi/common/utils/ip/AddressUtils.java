package com.ruoyi.common.utils.ip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.config.RuoYiConfig;

/**
 * 获取地址类（简化版本）
 * 
 * @author ruoyi
 */
public class AddressUtils
{
    private static final Logger log = LoggerFactory.getLogger(AddressUtils.class);

    // 未知地址
    public static final String UNKNOWN = "XX XX";

    public static String getRealAddressByIP(String ip)
    {
        // 内网不查询
        if (IpUtils.internalIp(ip))
        {
            return "内网IP";
        }
        
        // 简化实现，直接返回未知地址
        // 因为我们现在使用TCP而不是HTTP，无法进行外部HTTP请求
        log.info("TCP模式下无法获取IP地址信息: {}", ip);
        return UNKNOWN;
    }
}

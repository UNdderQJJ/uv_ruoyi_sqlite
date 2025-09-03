package com.ruoyi.business.service.DeviceInfo;

import com.ruoyi.business.enums.DeviceConfigKey;

import java.util.Map;

/**
 * 将配置应用到设备（渲染指令并发送）。
 */
public interface DeviceCommandService {

    void applyParameters(Long deviceId, Map<DeviceConfigKey, Object> params);

    void applySingle(Long deviceId, DeviceConfigKey key, Object param);
    
    /**
     * 获取设备当前文件名称
     * 
     * @param deviceId 设备ID
     * @return 当前文件名称，如果获取失败返回null
     */
    String getCurrentFileName(Long deviceId);
}



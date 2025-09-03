package com.ruoyi.business.service.DeviceInfo;

import com.ruoyi.business.enums.DeviceConfigKey;

import java.util.Map;

/**
 * 设备参数配置解析与校验服务。
 */
public interface DeviceConfigService {

    /**
     * 解析并校验 parameters JSON，返回 key->typedParam 的映射。
     * 非法时抛出 ServiceException。
     */
    Map<DeviceConfigKey, Object> parseAndValidate(String parametersJson);
}



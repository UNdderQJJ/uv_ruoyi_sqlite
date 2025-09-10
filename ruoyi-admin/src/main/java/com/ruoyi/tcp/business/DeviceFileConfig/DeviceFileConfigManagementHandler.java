package com.ruoyi.tcp.business.DeviceFileConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.domain.DeviceFileConfig.DeviceFileConfig;
import com.ruoyi.business.service.DeviceFileConfig.IDeviceFileConfigService;
import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.utils.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 设备文件配置管理TCP处理器
 * 
 * @author ruoyi
 */
@Component
public class DeviceFileConfigManagementHandler {

    private static final Logger log = LoggerFactory.getLogger(DeviceFileConfigManagementHandler.class);

    @Autowired
    private IDeviceFileConfigService deviceFileConfigService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理设备文件配置相关请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @return 响应结果
     */
    public TcpResponse handleDeviceFileConfigRequest(String path, String body) {
        try {
            log.info("处理设备文件配置请求: path={}, body={}", path, body);

            if (path.equals("/business/deviceFileConfig/list")) {
                return listDeviceFileConfigs(body);
            } else if (path.equals("/business/deviceFileConfig/listByDeviceId")) {
                return listDeviceFileConfigsByDeviceId(body);
            } else if (path.equals("/business/deviceFileConfig/listByDeviceIdAndFileName")) {
                return listDeviceFileConfigsByDeviceIdAndFileName(body);
            } else if (path.equals("/business/deviceFileConfig/listDefaultByDeviceId")) {
                return listDefaultDeviceFileConfigsByDeviceId(body);
            } else if (path.equals("/business/deviceFileConfig/listByVariableType")) {
                return listDeviceFileConfigsByVariableType(body);
            } else if (path.equals("/business/deviceFileConfig/get")) {
                return getDeviceFileConfig(body);
            } else if (path.equals("/business/deviceFileConfig/create")) {
                return createDeviceFileConfig(body);
            } else if (path.equals("/business/deviceFileConfig/update")) {
                return updateDeviceFileConfig(body);
            } else if (path.equals("/business/deviceFileConfig/delete")) {
                return deleteDeviceFileConfig(body);
            } else if (path.equals("/business/deviceFileConfig/deleteByDeviceId")) {
                return deleteDeviceFileConfigByDeviceId(body);
            } else if (path.equals("/business/deviceFileConfig/deleteByDeviceIdAndFileName")) {
                return deleteDeviceFileConfigByDeviceIdAndFileName(body);
            } else if (path.equals("/business/deviceFileConfig/setDefault")) {
                return setDeviceDefaultConfig(body);
            } else if (path.equals("/business/deviceFileConfig/count")) {
                return countDeviceFileConfigs(body);
            } else if (path.equals("/business/deviceFileConfig/countByDevice")) {
                return countDeviceFileConfigsByDevice();
            }  else if (path.equals("/business/deviceFileConfig/copyToDevice")) {
                return copyDeviceFileConfigToDevice(body);
            } else {
                return TcpResponse.error("未知的设备文件配置请求路径: " + path);
            }
        } catch (Exception e) {
            log.error("处理设备文件配置请求异常: path={}, body={}", path, body, e);
            return TcpResponse.error("处理设备文件配置请求异常: " + e.getMessage());
        }
    }

    /**
     * 查询设备文件配置列表
     */
    private TcpResponse listDeviceFileConfigs(String body) {
        try {
            DeviceFileConfig deviceFileConfig = objectMapper.readValue(body, DeviceFileConfig.class);
            List<DeviceFileConfig> list = deviceFileConfigService.selectDeviceFileConfigList(deviceFileConfig);
            return TcpResponse.success(list);
        } catch (Exception e) {
            log.error("查询设备文件配置列表异常", e);
            return TcpResponse.error("查询设备文件配置列表异常: " + e.getMessage());
        }
    }

    /**
     * 根据设备ID查询文件配置列表
     */
    private TcpResponse listDeviceFileConfigsByDeviceId(String body) {
        try {
            DeviceFileConfig deviceFileConfig = objectMapper.readValue(body, DeviceFileConfig.class);
            Long deviceId = deviceFileConfig.getDeviceId();
            if (deviceId == null) {
                return TcpResponse.error("设备ID不能为空");
            }
            List<DeviceFileConfig> list = deviceFileConfigService.selectDeviceFileConfigListByDeviceId(deviceId);
            return TcpResponse.success(list);
        } catch (Exception e) {
            log.error("根据设备ID查询文件配置列表异常", e);
            return TcpResponse.error("根据设备ID查询文件配置列表异常: " + e.getMessage());
        }
    }

    /**
     * 根据设备ID和文件名查询文件配置列表
     */
    private TcpResponse listDeviceFileConfigsByDeviceIdAndFileName(String body) {
        try {
            DeviceFileConfig deviceFileConfig = objectMapper.readValue(body, DeviceFileConfig.class);
            Long deviceId = deviceFileConfig.getDeviceId();
            String variableName = deviceFileConfig.getVariableName();
            if (deviceId == null) {
                return TcpResponse.error("设备ID不能为空");
            }
            if (StringUtils.isEmpty(variableName)) {
                return TcpResponse.error("变量名不能为空");
            }
            List<DeviceFileConfig> list = deviceFileConfigService.selectDeviceFileConfigListByDeviceIdAndFileName(deviceId, variableName);
            return TcpResponse.success(list);
        } catch (Exception e) {
            log.error("根据设备ID和变量名查询文件配置列表异常", e);
            return TcpResponse.error("根据设备ID和变量名查询文件配置列表异常: " + e.getMessage());
        }
    }

    /**
     * 根据设备ID查询默认配置列表
     */
    private TcpResponse listDefaultDeviceFileConfigsByDeviceId(String body) {
        try {
            DeviceFileConfig deviceFileConfig = objectMapper.readValue(body, DeviceFileConfig.class);
            Long deviceId = deviceFileConfig.getDeviceId();
            if (deviceId == null) {
                return TcpResponse.error("设备ID不能为空");
            }
            List<DeviceFileConfig> list = deviceFileConfigService.selectDefaultDeviceFileConfigListByDeviceId(deviceId);
            return TcpResponse.success(list);
        } catch (Exception e) {
            log.error("根据设备ID查询默认配置列表异常", e);
            return TcpResponse.error("根据设备ID查询默认配置列表异常: " + e.getMessage());
        }
    }

    /**
     * 根据变量类型查询文件配置列表
     */
    private TcpResponse listDeviceFileConfigsByVariableType(String body) {
        try {
            DeviceFileConfig deviceFileConfig = objectMapper.readValue(body, DeviceFileConfig.class);
            String variableType = deviceFileConfig.getVariableType();
            if (StringUtils.isEmpty(variableType)) {
                return TcpResponse.error("变量类型不能为空");
            }
            List<DeviceFileConfig> list = deviceFileConfigService.selectDeviceFileConfigListByVariableType(variableType);
            return TcpResponse.success(list);
        } catch (Exception e) {
            log.error("根据变量类型查询文件配置列表异常", e);
            return TcpResponse.error("根据变量类型查询文件配置列表异常: " + e.getMessage());
        }
    }

    /**
     * 获取设备文件配置详情
     */
    private TcpResponse getDeviceFileConfig(String body) {
        try {
            DeviceFileConfig deviceFileConfig = objectMapper.readValue(body, DeviceFileConfig.class);
            Long id = deviceFileConfig.getId();
            if (id == null) {
                return TcpResponse.error("配置ID不能为空");
            }
            DeviceFileConfig result = deviceFileConfigService.selectDeviceFileConfigById(id);
            if (result == null) {
                return TcpResponse.error("设备文件配置不存在");
            }
            return TcpResponse.success(result);
        } catch (Exception e) {
            log.error("获取设备文件配置详情异常", e);
            return TcpResponse.error("获取设备文件配置详情异常: " + e.getMessage());
        }
    }

    /**
     * 创建设备文件配置
     */
    private TcpResponse createDeviceFileConfig(String body) {
        try {
            DeviceFileConfig deviceFileConfig = objectMapper.readValue(body, DeviceFileConfig.class);
            
            // 验证必填字段
            if (deviceFileConfig.getDeviceId() == null) {
                return TcpResponse.error("设备ID不能为空");
            }
            if (StringUtils.isEmpty(deviceFileConfig.getFileName())) {
                return TcpResponse.error("文件名不能为空");
            }
            if (StringUtils.isEmpty(deviceFileConfig.getVariableName())) {
                return TcpResponse.error("变量名不能为空");
            }
            //变量名不能重复
            if(!deviceFileConfigService.selectDeviceFileConfigListByDeviceIdAndFileName(deviceFileConfig.getDeviceId(), deviceFileConfig.getVariableName()).isEmpty()){
                return TcpResponse.error("变量名不能重复");
            }

            //如果设为默认
            if (deviceFileConfig.getIsDefault() == 1) {
                //清空该设备下的所有默认配置
                deviceFileConfigService.clearDefaultByDeviceId(deviceFileConfig.getDeviceId());
            }

            int result = deviceFileConfigService.insertDeviceFileConfig(deviceFileConfig);
            if (result > 0) {
                return TcpResponse.success("创建设备文件配置成功");
            } else {
                return TcpResponse.error("创建设备文件配置失败");
            }
        } catch (Exception e) {
            log.error("创建设备文件配置异常", e);
            return TcpResponse.error("创建设备文件配置异常: " + e.getMessage());
        }
    }

    /**
     * 更新设备文件配置
     */
    private TcpResponse updateDeviceFileConfig(String body) {
        try {
            DeviceFileConfig deviceFileConfig = objectMapper.readValue(body, DeviceFileConfig.class);
            
            if (deviceFileConfig.getId() == null) {
                return TcpResponse.error("配置ID不能为空");
            }

            //变量名不能重复
            List<DeviceFileConfig>  deviceFileConfigList = deviceFileConfigService.selectDeviceFileConfigListByDeviceIdAndFileName(deviceFileConfig.getDeviceId(), deviceFileConfig.getFileName());
            if(ObjectUtils.isNotEmpty(deviceFileConfigList) && deviceFileConfigList.size()>1){
                return TcpResponse.error("变量名不能重复");
            }

            //如果设为默认
            if (deviceFileConfig.getIsDefault() == 1) {
                deviceFileConfigService.setDeviceDefaultConfig(deviceFileConfig.getDeviceId(), deviceFileConfig.getId());
            }

            int result = deviceFileConfigService.updateDeviceFileConfig(deviceFileConfig);
            if (result > 0) {
                return TcpResponse.success("更新设备文件配置成功");
            } else {
                return TcpResponse.error("更新设备文件配置失败");
            }
        } catch (Exception e) {
            log.error("更新设备文件配置异常", e);
            return TcpResponse.error("更新设备文件配置异常: " + e.getMessage());
        }
    }

    /**
     * 删除设备文件配置
     */
    private TcpResponse deleteDeviceFileConfig(String body) {
        try {
            DeviceFileConfig deviceFileConfig = objectMapper.readValue(body, DeviceFileConfig.class);
            
            if (deviceFileConfig.getIds() != null && deviceFileConfig.getIds().length > 0) {
                // 批量删除
                int result = deviceFileConfigService.deleteDeviceFileConfigByIds(deviceFileConfig.getIds());
                if (result > 0) {
                    return TcpResponse.success("批量删除设备文件配置成功");
                } else {
                    return TcpResponse.error("批量删除设备文件配置失败");
                }
            } else {
                // 单个删除
                Long id = deviceFileConfig.getId();
                if (id == null) {
                    return TcpResponse.error("配置ID不能为空");
                }
                int result = deviceFileConfigService.deleteDeviceFileConfigById(id);
                if (result > 0) {
                    return TcpResponse.success("删除设备文件配置成功");
                } else {
                    return TcpResponse.error("删除设备文件配置失败");
                }
            }
        } catch (Exception e) {
            log.error("删除设备文件配置异常", e);
            return TcpResponse.error("删除设备文件配置异常: " + e.getMessage());
        }
    }

    /**
     * 根据设备ID删除文件配置
     */
    private TcpResponse deleteDeviceFileConfigByDeviceId(String body) {
        try {
            DeviceFileConfig deviceFileConfig = objectMapper.readValue(body, DeviceFileConfig.class);
            Long deviceId = deviceFileConfig.getDeviceId();
            
            if (deviceId == null) {
                return TcpResponse.error("设备ID不能为空");
            }

            int result = deviceFileConfigService.deleteDeviceFileConfigByDeviceId(deviceId);
            if (result > 0) {
                return TcpResponse.success("根据设备ID删除文件配置成功");
            } else {
                return TcpResponse.error("根据设备ID删除文件配置失败");
            }
        } catch (Exception e) {
            log.error("根据设备ID删除文件配置异常", e);
            return TcpResponse.error("根据设备ID删除文件配置异常: " + e.getMessage());
        }
    }

    /**
     * 根据设备ID和文件名删除文件配置
     */
    private TcpResponse deleteDeviceFileConfigByDeviceIdAndFileName(String body) {
        try {
            DeviceFileConfig deviceFileConfig = objectMapper.readValue(body, DeviceFileConfig.class);
            Long deviceId = deviceFileConfig.getDeviceId();
            String fileName = deviceFileConfig.getFileName();
            
            if (deviceId == null) {
                return TcpResponse.error("设备ID不能为空");
            }
            if (StringUtils.isEmpty(fileName)) {
                return TcpResponse.error("文件名不能为空");
            }

            int result = deviceFileConfigService.deleteDeviceFileConfigByDeviceIdAndFileName(deviceId, fileName);
            if (result > 0) {
                return TcpResponse.success("根据设备ID和文件名删除文件配置成功");
            } else {
                return TcpResponse.error("根据设备ID和文件名删除文件配置失败");
            }
        } catch (Exception e) {
            log.error("根据设备ID和文件名删除文件配置异常", e);
            return TcpResponse.error("根据设备ID和文件名删除文件配置异常: " + e.getMessage());
        }
    }

    /**
     * 设置设备默认配置
     */
    private TcpResponse setDeviceDefaultConfig(String body) {
        try {
            DeviceFileConfig deviceFileConfig = objectMapper.readValue(body, DeviceFileConfig.class);
            Long deviceId = deviceFileConfig.getDeviceId();

            if (deviceId == null) {
                return TcpResponse.error("设备ID不能为空");
            }

            int result = deviceFileConfigService.setDeviceDefaultConfig(deviceId, deviceFileConfig.getId());
            if (result > 0) {
                return TcpResponse.success("设置设备默认配置成功");
            } else {
                return TcpResponse.error("设置设备默认配置失败");
            }
        } catch (Exception e) {
            log.error("设置设备默认配置异常", e);
            return TcpResponse.error("设置设备默认配置异常: " + e.getMessage());
        }
    }

    /**
     * 统计设备文件配置数量
     */
    private TcpResponse countDeviceFileConfigs(String body) {
        try {
            DeviceFileConfig deviceFileConfig = objectMapper.readValue(body, DeviceFileConfig.class);
            int count = deviceFileConfigService.countDeviceFileConfig(deviceFileConfig);
            return TcpResponse.success(count);
        } catch (Exception e) {
            log.error("统计设备文件配置数量异常", e);
            return TcpResponse.error("统计设备文件配置数量异常: " + e.getMessage());
        }
    }

    /**
     * 统计各设备文件配置数量
     */
    private TcpResponse countDeviceFileConfigsByDevice() {
        try {
            List<DeviceFileConfig> list = deviceFileConfigService.countDeviceFileConfigByDevice();
            return TcpResponse.success(list);
        } catch (Exception e) {
            log.error("统计各设备文件配置数量异常", e);
            return TcpResponse.error("统计各设备文件配置数量异常: " + e.getMessage());
        }
    }

    /**
     * 复制设备文件配置到其他设备
     */
    private TcpResponse copyDeviceFileConfigToDevice(String body) {
        try {
            DeviceFileConfig deviceFileConfig = objectMapper.readValue(body, DeviceFileConfig.class);
            Long sourceDeviceId = deviceFileConfig.getDeviceId();
            Long targetDeviceId = deviceFileConfig.getId(); // 使用id字段存储目标设备ID
            
            if (sourceDeviceId == null) {
                return TcpResponse.error("源设备ID不能为空");
            }
            if (targetDeviceId == null) {
                return TcpResponse.error("目标设备ID不能为空");
            }

            int result = deviceFileConfigService.copyDeviceFileConfig(sourceDeviceId, targetDeviceId);
            if (result > 0) {
                return TcpResponse.success("复制设备文件配置成功，共复制" + result + "条配置");
            } else {
                return TcpResponse.error("复制设备文件配置失败");
            }
        } catch (Exception e) {
            log.error("复制设备文件配置异常", e);
            return TcpResponse.error("复制设备文件配置异常: " + e.getMessage());
        }
    }
}

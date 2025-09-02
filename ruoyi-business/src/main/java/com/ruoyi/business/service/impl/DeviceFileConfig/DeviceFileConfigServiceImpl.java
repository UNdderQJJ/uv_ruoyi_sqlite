package com.ruoyi.business.service.impl.DeviceFileConfig;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.business.mapper.DeviceFileConfig.DeviceFileConfigMapper;
import com.ruoyi.business.domain.DeviceFileConfig.DeviceFileConfig;
import com.ruoyi.business.service.DeviceFileConfig.IDeviceFileConfigService;

/**
 * 设备文件配置Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-01-30
 */
@Service
public class DeviceFileConfigServiceImpl implements IDeviceFileConfigService 
{
    @Autowired
    private DeviceFileConfigMapper deviceFileConfigMapper;

    /**
     * 查询设备文件配置
     * 
     * @param id 设备文件配置主键
     * @return 设备文件配置
     */
    @Override
    public DeviceFileConfig selectDeviceFileConfigById(Long id)
    {
        return deviceFileConfigMapper.selectDeviceFileConfigById(id);
    }

    /**
     * 查询设备文件配置列表
     * 
     * @param deviceFileConfig 设备文件配置
     * @return 设备文件配置
     */
    @Override
    public List<DeviceFileConfig> selectDeviceFileConfigList(DeviceFileConfig deviceFileConfig)
    {
        return deviceFileConfigMapper.selectDeviceFileConfigList(deviceFileConfig);
    }

    /**
     * 根据设备ID查询文件配置列表
     * 
     * @param deviceId 设备ID
     * @return 设备文件配置集合
     */
    @Override
    public List<DeviceFileConfig> selectDeviceFileConfigListByDeviceId(Long deviceId)
    {
        return deviceFileConfigMapper.selectDeviceFileConfigListByDeviceId(deviceId);
    }

    /**
     * 根据设备ID和文件名查询文件配置列表
     * 
     * @param deviceId 设备ID
     * @param fileName 文件名
     * @return 设备文件配置集合
     */
    @Override
    public List<DeviceFileConfig> selectDeviceFileConfigListByDeviceIdAndFileName(Long deviceId, String fileName)
    {
        return deviceFileConfigMapper.selectDeviceFileConfigListByDeviceIdAndFileName(deviceId, fileName);
    }

    /**
     * 根据设备ID查询默认配置列表
     * 
     * @param deviceId 设备ID
     * @return 设备文件配置集合
     */
    @Override
    public List<DeviceFileConfig> selectDefaultDeviceFileConfigListByDeviceId(Long deviceId)
    {
        return deviceFileConfigMapper.selectDefaultDeviceFileConfigListByDeviceId(deviceId);
    }

    /**
     * 根据变量类型查询文件配置列表
     * 
     * @param variableType 变量类型
     * @return 设备文件配置集合
     */
    @Override
    public List<DeviceFileConfig> selectDeviceFileConfigListByVariableType(String variableType)
    {
        return deviceFileConfigMapper.selectDeviceFileConfigListByVariableType(variableType);
    }

    /**
     * 新增设备文件配置
     * 
     * @param deviceFileConfig 设备文件配置
     * @return 结果
     */
    @Override
    public int insertDeviceFileConfig(DeviceFileConfig deviceFileConfig)
    {
        // 设置默认值
        if (StringUtils.isEmpty(deviceFileConfig.getVariableType())) {
            deviceFileConfig.setVariableType("TEXT");
        }
        if (deviceFileConfig.getIsDefault() == null) {
            deviceFileConfig.setIsDefault(0);
        }
        deviceFileConfig.setCreateTime(DateUtils.getNowDate());
        deviceFileConfig.setUpdateTime(DateUtils.getNowDate());
        return deviceFileConfigMapper.insertDeviceFileConfig(deviceFileConfig);
    }

    /**
     * 修改设备文件配置
     * 
     * @param deviceFileConfig 设备文件配置
     * @return 结果
     */
    @Override
    public int updateDeviceFileConfig(DeviceFileConfig deviceFileConfig)
    {
        deviceFileConfig.setUpdateTime(DateUtils.getNowDate());
        return deviceFileConfigMapper.updateDeviceFileConfig(deviceFileConfig);
    }

    /**
     * 批量删除设备文件配置
     * 
     * @param ids 需要删除的设备文件配置主键
     * @return 结果
     */
    @Override
    public int deleteDeviceFileConfigByIds(Long[] ids)
    {
        return deviceFileConfigMapper.deleteDeviceFileConfigByIds(ids);
    }

    /**
     * 删除设备文件配置信息
     * 
     * @param id 设备文件配置主键
     * @return 结果
     */
    @Override
    public int deleteDeviceFileConfigById(Long id)
    {
        return deviceFileConfigMapper.deleteDeviceFileConfigById(id);
    }

    /**
     * 根据设备ID删除文件配置
     * 
     * @param deviceId 设备ID
     * @return 结果
     */
    @Override
    public int deleteDeviceFileConfigByDeviceId(Long deviceId)
    {
        return deviceFileConfigMapper.deleteDeviceFileConfigByDeviceId(deviceId);
    }

    /**
     * 根据设备ID和文件名删除文件配置
     * 
     * @param deviceId 设备ID
     * @param fileName 文件名
     * @return 结果
     */
    @Override
    public int deleteDeviceFileConfigByDeviceIdAndFileName(Long deviceId, String fileName)
    {
        return deviceFileConfigMapper.deleteDeviceFileConfigByDeviceIdAndFileName(deviceId, fileName);
    }

    /**
     * 设置设备默认配置
     * 
     * @param deviceId 设备ID
     * @param fileName 文件名
     * @return 结果
     */
    @Override
    public int setDeviceDefaultConfig(Long deviceId, String fileName)
    {
        return deviceFileConfigMapper.setDeviceDefaultConfig(deviceId, fileName);
    }

    /**
     * 统计设备文件配置数量
     * 
     * @param deviceFileConfig 查询条件
     * @return 配置数量
     */
    @Override
    public int countDeviceFileConfig(DeviceFileConfig deviceFileConfig)
    {
        return deviceFileConfigMapper.countDeviceFileConfig(deviceFileConfig);
    }

    /**
     * 统计各设备文件配置数量
     * 
     * @return 设备文件配置统计结果
     */
    @Override
    public List<DeviceFileConfig> countDeviceFileConfigByDevice()
    {
        return deviceFileConfigMapper.countDeviceFileConfigByDevice();
    }

    /**
     * 批量创建设备文件配置
     * 
     * @param deviceFileConfigList 设备文件配置列表
     * @return 结果
     */
    @Override
    public int batchInsertDeviceFileConfig(List<DeviceFileConfig> deviceFileConfigList)
    {
        int result = 0;
        for (DeviceFileConfig config : deviceFileConfigList) {
            result += insertDeviceFileConfig(config);
        }
        return result;
    }

    /**
     * 复制设备文件配置到其他设备
     * 
     * @param sourceDeviceId 源设备ID
     * @param targetDeviceId 目标设备ID
     * @return 结果
     */
    @Override
    public int copyDeviceFileConfig(Long sourceDeviceId, Long targetDeviceId)
    {
        // 查询源设备的所有配置
        List<DeviceFileConfig> sourceConfigs = selectDeviceFileConfigListByDeviceId(sourceDeviceId);
        if (sourceConfigs.isEmpty()) {
            return 0;
        }

        // 复制配置到目标设备
        int result = 0;
        for (DeviceFileConfig sourceConfig : sourceConfigs) {
            DeviceFileConfig targetConfig = new DeviceFileConfig();
            targetConfig.setDeviceId(targetDeviceId);
            targetConfig.setFileName(sourceConfig.getFileName());
            targetConfig.setVariableName(sourceConfig.getVariableName());
            targetConfig.setVariableType(sourceConfig.getVariableType());
            targetConfig.setFixedContent(sourceConfig.getFixedContent());
            targetConfig.setIsDefault(0); // 新复制的配置不是默认配置
            targetConfig.setDescription("从设备" + sourceDeviceId + "复制: " + sourceConfig.getDescription());
            
            result += insertDeviceFileConfig(targetConfig);
        }
        return result;
    }
}

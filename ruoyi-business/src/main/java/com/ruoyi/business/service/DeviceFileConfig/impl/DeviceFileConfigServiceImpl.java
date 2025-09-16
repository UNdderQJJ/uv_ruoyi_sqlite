package com.ruoyi.business.service.DeviceFileConfig.impl;

import java.util.List;

import com.ruoyi.business.domain.DeviceInfo.DeviceInfo;
import com.ruoyi.business.service.DeviceInfo.IDeviceInfoService;
import com.ruoyi.business.service.DeviceInfo.impl.DeviceInfoServiceImpl;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
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
    @Resource
    private DeviceFileConfigMapper deviceFileConfigMapper;

    @Resource
    private IDeviceInfoService deviceInfoService;

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
     * 根据设备ID和变量名查询文件配置列表
     * 
     * @param deviceId 设备ID
     * @param variableName 变量名
     * @return 设备文件配置集合
     */
    @Override
    public List<DeviceFileConfig> selectDeviceFileConfigListByDeviceIdAndFileName(Long deviceId, String variableName)
    {
        return deviceFileConfigMapper.selectDeviceFileConfigListByDeviceIdAndFileName(deviceId, variableName);
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
     * @param id 文件id
     * @return 结果
     */
    @Override
    public int setDeviceDefaultConfig(Long deviceId, Long id)
    {
        // 先清除该设备下所有默认标记，再将目标文件名设置为默认
        deviceFileConfigMapper.clearDefaultByDeviceId(deviceId);
        return deviceFileConfigMapper.setDeviceDefaultConfig(deviceId, id);
    }

    /**
     *清除该设备下所有默认标记
     * @param deviceId 设备ID
     */
    @Override
    public void clearDefaultByDeviceId(Long deviceId)
        {
        deviceFileConfigMapper.clearDefaultByDeviceId(deviceId);
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
     * 复制设备文件配置
     *
     * @param configId 设备文件配置ID
     * @param deviceIds 设备ID列表
     */
    @Override
    public boolean copyDeviceFileConfig(Long configId, List<Long> deviceIds) {
        //查询需要复制的设备文件配置
        DeviceFileConfig config = selectDeviceFileConfigById(configId);
        if (ObjectUtils.isNotEmpty( config)) {
            //遍历设备ID列表，复制设备文件配置
            for (Long deviceId : deviceIds) {
                DeviceFileConfig configOne;
                configOne = config;
                configOne.setId(null);
                configOne.setDeviceId(deviceId);
                //清除其他设备的默认配置
                clearDefaultByDeviceId(deviceId);
                //设置该设备为默认配置
                configOne.setIsDefault(1);
                insertDeviceFileConfig(configOne);
            }
            return true;
        }else {
            return false;
        }
    }

    /**
     * 检查设备默认文件配置是否一致
     *
     * @param deviceIds 设备ID列表
     * @return true表示配置一致，false表示配置不一致
     */
    @Override
    public boolean checkDeviceFileConfig(List<Long> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return true; // 空列表认为配置一致
        }
        
        if (deviceIds.size() == 1) {
            List<DeviceFileConfig> firstDeviceConfigs = selectDefaultDeviceFileConfigListByDeviceId(deviceIds.get(0));
            //如果有默认配置，则认为配置一致
            if (!firstDeviceConfigs.isEmpty()) {
                return true;
            }else {
                DeviceInfo deviceInfo = deviceInfoService.selectDeviceInfoById(deviceIds.get(0));
               throw  new RuntimeException("请先设置"+deviceInfo.getName()+"设备默认文件配置");
            }
        }
        
        // 获取第一个设备的默认配置作为基准
        List<DeviceFileConfig> firstDeviceConfigs = selectDefaultDeviceFileConfigListByDeviceId(deviceIds.get(0));

        // 比较其他设备的默认配置与第一个设备是否一致
        for (int i = 1; i < deviceIds.size(); i++) {
            List<DeviceFileConfig> otherConfigs = selectDefaultDeviceFileConfigListByDeviceId(deviceIds.get(i));

            // 比较每个配置的详细信息
            for (DeviceFileConfig firstConfig : firstDeviceConfigs) {
                boolean foundMatch = false;
                for (DeviceFileConfig otherConfig : otherConfigs) {
                    if (isConfigEqual(firstConfig, otherConfig)) {
                        foundMatch = true;
                        break;
                    }
                }
                if (!foundMatch) {
                    DeviceInfo deviceInfo = deviceInfoService.selectDeviceInfoById(deviceIds.get(i));
                    throw new RuntimeException("设备" + deviceInfo.getName() + "的默认文件配置与其他的不一致！");
                }
            }
        }
        
        return true; // 所有设备的默认配置都一致
    }

    /**
     * 比较两个设备文件配置是否相等（排除设备ID和主键ID）
     *
     * @param config1 配置1
     * @param config2 配置2
     * @return true表示相等，false表示不相等
     */
    private boolean isConfigEqual(DeviceFileConfig config1, DeviceFileConfig config2) {
        if (config1 == null || config2 == null) {
            return config1 == config2;
        }
        
        return StringUtils.equals(config1.getVariableName(), config2.getVariableName()) && // 变量名称和变量类型相等
               StringUtils.equals(config1.getVariableType(), config2.getVariableType());
    }
}

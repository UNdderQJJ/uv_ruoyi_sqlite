package com.ruoyi.business.service.DeviceFileConfig;

import java.util.List;
import com.ruoyi.business.domain.DeviceFileConfig.DeviceFileConfig;

/**
 * 设备文件配置Service接口
 * 
 * @author ruoyi
 * @date 2025-01-30
 */
public interface IDeviceFileConfigService 
{
    /**
     * 查询设备文件配置
     * 
     * @param id 设备文件配置主键
     * @return 设备文件配置
     */
    public DeviceFileConfig selectDeviceFileConfigById(Long id);

    /**
     * 查询设备文件配置列表
     * 
     * @param deviceFileConfig 设备文件配置
     * @return 设备文件配置集合
     */
    public List<DeviceFileConfig> selectDeviceFileConfigList(DeviceFileConfig deviceFileConfig);

    /**
     * 根据设备ID查询文件配置列表
     * 
     * @param deviceId 设备ID
     * @return 设备文件配置集合
     */
    public List<DeviceFileConfig> selectDeviceFileConfigListByDeviceId(Long deviceId);

    /**
     * 根据设备ID和变量名查询文件配置列表
     * 
     * @param deviceId 设备ID
     * @param  variableName 变量名
     * @return 设备文件配置集合
     */
    public List<DeviceFileConfig> selectDeviceFileConfigListByDeviceIdAndFileName(Long deviceId, String variableName);

    /**
     * 根据设备ID查询默认配置列表
     * 
     * @param deviceId 设备ID
     * @return 设备文件配置集合
     */
    public List<DeviceFileConfig> selectDefaultDeviceFileConfigListByDeviceId(Long deviceId);

    /**
     * 根据变量类型查询文件配置列表
     * 
     * @param variableType 变量类型
     * @return 设备文件配置集合
     */
    public List<DeviceFileConfig> selectDeviceFileConfigListByVariableType(String variableType);

    /**
     * 新增设备文件配置
     * 
     * @param deviceFileConfig 设备文件配置
     * @return 结果
     */
    public int insertDeviceFileConfig(DeviceFileConfig deviceFileConfig);

    /**
     * 修改设备文件配置
     * 
     * @param deviceFileConfig 设备文件配置
     * @return 结果
     */
    public int updateDeviceFileConfig(DeviceFileConfig deviceFileConfig);

    /**
     * 批量删除设备文件配置
     * 
     * @param ids 需要删除的设备文件配置主键集合
     * @return 结果
     */
    public int deleteDeviceFileConfigByIds(Long[] ids);

    /**
     * 删除设备文件配置信息
     * 
     * @param id 设备文件配置主键
     * @return 结果
     */
    public int deleteDeviceFileConfigById(Long id);

    /**
     * 根据设备ID删除文件配置
     * 
     * @param deviceId 设备ID
     * @return 结果
     */
    public int deleteDeviceFileConfigByDeviceId(Long deviceId);

    /**
     * 根据设备ID和文件名删除文件配置
     * 
     * @param deviceId 设备ID
     * @param fileName 文件名
     * @return 结果
     */
    public int deleteDeviceFileConfigByDeviceIdAndFileName(Long deviceId, String fileName);

    /**
     * 设置设备默认配置
     * 
     * @param deviceId 设备ID
     * @param id 文件id
     * @return 结果
     */
    public int setDeviceDefaultConfig(Long deviceId, Long id);

    /**
     * 根据设备ID清除默认配置
     *
     * @param deviceId 设备ID
     * @return 结果
     */
    public void clearDefaultByDeviceId(Long deviceId);

    /**
     * 统计设备文件配置数量
     * 
     * @param deviceFileConfig 查询条件
     * @return 配置数量
     */
    public int countDeviceFileConfig(DeviceFileConfig deviceFileConfig);

    /**
     * 统计各设备文件配置数量
     * 
     * @return 设备文件配置统计结果
     */
    public List<DeviceFileConfig> countDeviceFileConfigByDevice();

    /**
     * 批量创建设备文件配置
     * 
     * @param deviceFileConfigList 设备文件配置列表
     * @return 结果
     */
    public int batchInsertDeviceFileConfig(List<DeviceFileConfig> deviceFileConfigList);


    /**
     *  复制配置文件到其他多设备
     * @param configId 配置ID
     * @param deviceIds 设备ID列表
     */
    public boolean  copyDeviceFileConfig(Long configId,List<Long> deviceIds);

    /**
     * 检查设备文件配置是否一致
     * @param deviceIds 设备ID列表
     * @return true表示配置一致，false表示配置不一致
     */
    boolean checkDeviceFileConfig(List<Long> deviceIds);
}

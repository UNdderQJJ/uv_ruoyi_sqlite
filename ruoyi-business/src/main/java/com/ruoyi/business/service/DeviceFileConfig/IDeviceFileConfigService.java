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
     * 根据设备ID和文件名查询文件配置列表
     * 
     * @param deviceId 设备ID
     * @param fileName 文件名
     * @return 设备文件配置集合
     */
    public List<DeviceFileConfig> selectDeviceFileConfigListByDeviceIdAndFileName(Long deviceId, String fileName);

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
     * @param fileName 文件名
     * @return 结果
     */
    public int setDeviceDefaultConfig(Long deviceId, String fileName);

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
     * 复制设备文件配置到其他设备
     * 
     * @param sourceDeviceId 源设备ID
     * @param targetDeviceId 目标设备ID
     * @return 结果
     */
    public int copyDeviceFileConfig(Long sourceDeviceId, Long targetDeviceId);
}

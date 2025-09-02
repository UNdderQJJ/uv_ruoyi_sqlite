package com.ruoyi.business.mapper.DeviceInfo;

import java.util.List;
import com.ruoyi.business.domain.DeviceInfo.DeviceInfo;

/**
 * 设备信息Mapper接口
 * 
 * @author ruoyi
 * @date 2025-01-30
 */
public interface DeviceInfoMapper 
{
    /**
     * 查询设备信息
     * 
     * @param id 设备信息主键
     * @return 设备信息
     */
    public DeviceInfo selectDeviceInfoById(Long id);

    /**
     * 根据设备UUID查询设备信息
     * 
     * @param deviceUuid 设备UUID
     * @return 设备信息
     */
    public DeviceInfo selectDeviceInfoByUuid(String deviceUuid);

    /**
     * 查询设备信息列表
     * 
     * @param deviceInfo 设备信息
     * @return 设备信息集合
     */
    public List<DeviceInfo> selectDeviceInfoList(DeviceInfo deviceInfo);

    /**
     * 根据设备类型查询设备列表
     * 
     * @param deviceType 设备类型
     * @return 设备信息集合
     */
    public List<DeviceInfo> selectDeviceInfoListByType(String deviceType);

    /**
     * 根据设备状态查询设备列表
     * 
     * @param status 设备状态
     * @return 设备信息集合
     */
    public List<DeviceInfo> selectDeviceInfoListByStatus(String status);

    /**
     * 查询启用的设备列表
     * 
     * @return 设备信息集合
     */
    public List<DeviceInfo> selectEnabledDeviceInfoList();

    /**
     * 查询在线设备列表
     * 
     * @return 设备信息集合
     */
    public List<DeviceInfo> selectOnlineDeviceInfoList();

    /**
     * 根据任务ID查询设备列表
     * 
     * @param taskId 任务ID
     * @return 设备信息集合
     */
    public List<DeviceInfo> selectDeviceInfoListByTaskId(Long taskId);

    /**
     * 新增设备信息
     * 
     * @param deviceInfo 设备信息
     * @return 结果
     */
    public int insertDeviceInfo(DeviceInfo deviceInfo);

    /**
     * 修改设备信息
     * 
     * @param deviceInfo 设备信息
     * @return 结果
     */
    public int updateDeviceInfo(DeviceInfo deviceInfo);

    /**
     * 更新设备状态
     * 
     * @param deviceInfo 设备信息(包含id和status)
     * @return 结果
     */
    public int updateDeviceStatus(DeviceInfo deviceInfo);

    /**
     * 更新设备心跳时间
     * 
     * @param deviceInfo 设备信息(包含id和lastHeartbeatTime)
     * @return 结果
     */
    public int updateDeviceHeartbeat(DeviceInfo deviceInfo);

    /**
     * 更新设备当前任务
     * 
     * @param deviceInfo 设备信息(包含id和currentTaskId)
     * @return 结果
     */
    public int updateDeviceCurrentTask(DeviceInfo deviceInfo);

    /**
     * 删除设备信息
     * 
     * @param id 设备信息主键
     * @return 结果
     */
    public int deleteDeviceInfoById(Long id);

    /**
     * 批量删除设备信息
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDeviceInfoByIds(Long[] ids);

    /**
     * 检查设备UUID是否存在
     * 
     * @param deviceUuid 设备UUID
     * @return 结果
     */
    public int checkDeviceUuidUnique(String deviceUuid);

    /**
     * 统计设备数量
     * 
     * @param deviceInfo 查询条件
     * @return 设备数量
     */
    public int countDeviceInfo(DeviceInfo deviceInfo);

    /**
     * 统计各类型设备数量
     * 
     * @return 设备类型统计结果
     */
    public List<DeviceInfo> countDeviceInfoByType();
}

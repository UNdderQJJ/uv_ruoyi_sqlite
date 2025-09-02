package com.ruoyi.business.service.DeviceInfo.impl;


import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.business.mapper.DeviceInfo.DeviceInfoMapper;
import com.ruoyi.business.domain.DeviceInfo.DeviceInfo;
import com.ruoyi.business.service.DeviceInfo.IDeviceInfoService;

/**
 * 设备信息Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-01-30
 */
@Service
public class DeviceInfoServiceImpl implements IDeviceInfoService 
{
    @Resource
    private DeviceInfoMapper deviceInfoMapper;

    /**
     * 查询设备信息
     * 
     * @param id 设备信息主键
     * @return 设备信息
     */
    @Override
    public DeviceInfo selectDeviceInfoById(Long id)
    {
        return deviceInfoMapper.selectDeviceInfoById(id);
    }

    /**
     * 根据设备UUID查询设备信息
     * 
     * @param deviceUuid 设备UUID
     * @return 设备信息
     */
    @Override
    public DeviceInfo selectDeviceInfoByUuid(String deviceUuid)
    {
        return deviceInfoMapper.selectDeviceInfoByUuid(deviceUuid);
    }

    /**
     * 查询设备信息列表
     * 
     * @param deviceInfo 设备信息
     * @return 设备信息
     */
    @Override
    public List<DeviceInfo> selectDeviceInfoList(DeviceInfo deviceInfo)
    {
        return deviceInfoMapper.selectDeviceInfoList(deviceInfo);
    }

    /**
     * 根据设备类型查询设备列表
     * 
     * @param deviceType 设备类型
     * @return 设备信息集合
     */
    @Override
    public List<DeviceInfo> selectDeviceInfoListByType(String deviceType)
    {
        return deviceInfoMapper.selectDeviceInfoListByType(deviceType);
    }

    /**
     * 根据设备状态查询设备列表
     * 
     * @param status 设备状态
     * @return 设备信息集合
     */
    @Override
    public List<DeviceInfo> selectDeviceInfoListByStatus(String status)
    {
        return deviceInfoMapper.selectDeviceInfoListByStatus(status);
    }

    /**
     * 查询启用的设备列表
     * 
     * @return 设备信息集合
     */
    @Override
    public List<DeviceInfo> selectEnabledDeviceInfoList()
    {
        return deviceInfoMapper.selectEnabledDeviceInfoList();
    }

    /**
     * 查询在线设备列表
     * 
     * @return 设备信息集合
     */
    @Override
    public List<DeviceInfo> selectOnlineDeviceInfoList()
    {
        return deviceInfoMapper.selectOnlineDeviceInfoList();
    }

    /**
     * 根据任务ID查询设备列表
     * 
     * @param taskId 任务ID
     * @return 设备信息集合
     */
    @Override
    public List<DeviceInfo> selectDeviceInfoListByTaskId(Long taskId)
    {
        return deviceInfoMapper.selectDeviceInfoListByTaskId(taskId);
    }

    /**
     * 新增设备信息
     * 
     * @param deviceInfo 设备信息
     * @return 结果
     */
    @Override
    public int insertDeviceInfo(DeviceInfo deviceInfo)
    {
        // 设置默认值
        if (StringUtils.isEmpty(deviceInfo.getStatus())) {
            deviceInfo.setStatus("OFFLINE");
        }
        if (deviceInfo.getIsEnabled() == null) {
            deviceInfo.setIsEnabled(1);
        }
        if (deviceInfo.getDataBits() == null) {
            deviceInfo.setDataBits(8);
        }
        if (deviceInfo.getStopBits() == null) {
            deviceInfo.setStopBits(1);
        }
        if (StringUtils.isEmpty(deviceInfo.getParity())) {
            deviceInfo.setParity("NONE");
        }
        deviceInfo.setCreateTime(DateUtils.getNowDate());
        deviceInfo.setUpdateTime(DateUtils.getNowDate());
        return deviceInfoMapper.insertDeviceInfo(deviceInfo);
    }

    /**
     * 修改设备信息
     * 
     * @param deviceInfo 设备信息
     * @return 结果
     */
    @Override
    public int updateDeviceInfo(DeviceInfo deviceInfo)
    {
        deviceInfo.setUpdateTime(DateUtils.getNowDate());
        return deviceInfoMapper.updateDeviceInfo(deviceInfo);
    }

    /**
     * 更新设备状态
     * 
     * @param id 设备ID
     * @param status 设备状态
     * @return 结果
     */
    @Override
    public int updateDeviceStatus(Long id, String status)
    {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setId(id);
        deviceInfo.setStatus(status);
        deviceInfo.setUpdateTime(DateUtils.getNowDate());
        return deviceInfoMapper.updateDeviceStatus(deviceInfo);
    }

    /**
     * 更新设备心跳时间
     * 
     * @param id 设备ID
     * @return 结果
     */
    @Override
    public int updateDeviceHeartbeat(Long id)
    {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setId(id);
        deviceInfo.setLastHeartbeatTime(DateUtils.getNowDate());
        return deviceInfoMapper.updateDeviceHeartbeat(deviceInfo);
    }

    /**
     * 更新设备当前任务
     * 
     * @param id 设备ID
     * @param taskId 任务ID
     * @return 结果
     */
    @Override
    public int updateDeviceCurrentTask(Long id, Long taskId)
    {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setId(id);
        deviceInfo.setCurrentTaskId(taskId);
        deviceInfo.setUpdateTime(DateUtils.getNowDate());
        return deviceInfoMapper.updateDeviceCurrentTask(deviceInfo);
    }

    /**
     * 批量删除设备信息
     * 
     * @param ids 需要删除的设备信息主键
     * @return 结果
     */
    @Override
    public int deleteDeviceInfoByIds(Long[] ids)
    {
        return deviceInfoMapper.deleteDeviceInfoByIds(ids);
    }

    /**
     * 删除设备信息信息
     * 
     * @param id 设备信息主键
     * @return 结果
     */
    @Override
    public int deleteDeviceInfoById(Long id)
    {
        return deviceInfoMapper.deleteDeviceInfoById(id);
    }

    /**
     * 检查设备UUID是否存在
     * 
     * @param deviceUuid 设备UUID
     * @return 结果
     */
    @Override
    public boolean checkDeviceUuidUnique(String deviceUuid)
    {
        return deviceInfoMapper.checkDeviceUuidUnique(deviceUuid) == 0;
    }

    /**
     * 统计设备数量
     * 
     * @param deviceInfo 查询条件
     * @return 设备数量
     */
    @Override
    public int countDeviceInfo(DeviceInfo deviceInfo)
    {
        return deviceInfoMapper.countDeviceInfo(deviceInfo);
    }

    /**
     * 统计各类型设备数量
     * 
     * @return 设备类型统计结果
     */
    @Override
    public List<DeviceInfo> countDeviceInfoByType()
    {
        return deviceInfoMapper.countDeviceInfoByType();
    }

    /**
     * 启用/禁用设备
     * 
     * @param id 设备ID
     * @param isEnabled 是否启用(0:禁用, 1:启用)
     * @return 结果
     */
    @Override
    public int enableDevice(Long id, Integer isEnabled)
    {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setId(id);
        deviceInfo.setIsEnabled(isEnabled);
        deviceInfo.setUpdateTime(DateUtils.getNowDate());
        return deviceInfoMapper.updateDeviceInfo(deviceInfo);
    }

    /**
     * 批量启用/禁用设备
     * 
     * @param ids 设备ID数组
     * @param isEnabled 是否启用(0:禁用, 1:启用)
     * @return 结果
     */
    @Override
    public int batchEnableDevice(Long[] ids, Integer isEnabled)
    {
        int result = 0;
        for (Long id : ids) {
            result += enableDevice(id, isEnabled);
        }
        return result;
    }
}

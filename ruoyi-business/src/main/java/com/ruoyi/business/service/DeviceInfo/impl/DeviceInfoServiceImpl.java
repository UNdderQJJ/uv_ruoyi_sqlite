package com.ruoyi.business.service.DeviceInfo.impl;


import java.util.List;
import com.github.pagehelper.Page;
import com.ruoyi.business.domain.TaskInfo.TaskInfo;
import com.ruoyi.business.enums.DeviceStatus;
import com.ruoyi.business.enums.DeviceType;
import com.ruoyi.business.service.TaskInfo.ITaskInfoService;
import com.ruoyi.common.core.page.PageQuery;
import com.ruoyi.common.core.page.PageResult;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.PageQueryUtils;
import com.ruoyi.common.utils.StringUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
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

    @Resource
    private ITaskInfoService taskInfoService;

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
     * 分页查询设备信息列表
     */
    @Override
    public PageResult<DeviceInfo> selectDeviceInfoPageList(DeviceInfo deviceInfo, PageQuery pageQuery) {
        long startTime = System.currentTimeMillis();
        try {
            // 启动分页
            PageQueryUtils.startPage(pageQuery);

            // 执行查询
             List<DeviceInfo> list = deviceInfoMapper.selectDeviceInfoList(deviceInfo);

            // 获取分页信息
            Page<DeviceInfo> page = (Page<DeviceInfo>) list;

            // 构建分页结果
            return PageResult.of(list, page.getTotal(), pageQuery);
        } finally {
            // 清理分页
            PageQueryUtils.clearPage();

            // 性能监控
            long duration = System.currentTimeMillis() - startTime;
            if (duration > 3000) { // 超过3秒记录警告
                System.out.println("设备列表分页查询耗时: " + duration + "ms");
            }
        }
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
        TaskInfo taskInfo = taskInfoService.selectTaskInfoById(taskId);
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setId(id);
        deviceInfo.setCurrentTaskId(taskId);
        deviceInfo.setCurrentTaskName(taskInfo.getName());
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
     * 统计各状态设备数量
     *
     * @return 设备状态统计结果
     */
    @Override
    public List<DeviceInfo> countDeviceInfoByStatus()
    {
        return deviceInfoMapper.countDeviceInfoByStatus();
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

    /**
     * 更新设备当前任务
     *
     * @param deviceIds 设备ID数组
     * @param taskId 任务ID
     */
    @Override
    public void updateCurrentTask(List<String> deviceIds, Long taskId) {
        for(String deviceId : deviceIds){
            TaskInfo taskInfo = taskInfoService.selectTaskInfoById(taskId);
            DeviceInfo deviceInfo = selectDeviceInfoById(Long.parseLong(deviceId));
            //为设备绑定任务
            if (deviceInfo.getDeviceType().equals(DeviceType.PRINTER.getCode())) {
                deviceInfo.setStatus(DeviceStatus.ONLINE_PRINTING.getCode());
            }else if (deviceInfo.getDeviceType().equals(DeviceType.SCANNER.getCode())) {
                deviceInfo.setStatus(DeviceStatus.ONLINE_SCANNING.getCode());
            }
            deviceInfo.setCurrentTaskId(taskId);
            deviceInfo.setCurrentTaskName(taskInfo.getName());
            deviceInfoMapper.updateDeviceCurrentTask(deviceInfo);
        }
    }

    /**
     * 为相应设备移除当前任务
     *
     * @param taskId 任务ID
     */
    @Override
    public void removeCurrentTask(Long taskId) {
        deviceInfoMapper.removeCurrentTask(taskId);
    }

    /**
     * 为相应设备移除当前任务
     *
     * @param deviceId 设备ID
     * @param status 设备状态
     */
    @Override
    public void removeCurrentTask(Long deviceId, String status) {
        deviceInfoMapper.removeCurrentTaskStatus(deviceId, status);
    }

    /**
     * 更新设备扫描器
     *
     * @param scannerId 扫描器ID
     * @param deviceId 设备ID
     * @param deviceName 设备名称
     */
    @Override
    public void updateScanner(Long scannerId, Long deviceId, String deviceName) {
        deviceInfoMapper.updateScanner(scannerId, deviceId, deviceName);
    }

    /**
     * 移除设备扫描器
     *
     * @param scannerId 扫描器ID
     */
    @Override
    public void removeScanner(Long scannerId) {
        deviceInfoMapper.removeScanner(scannerId);
    }

    /**
     * 移除设备打印机
     *
     * @param printerId 打印机ID
     */
    @Override
    public void removePrinter(Long printerId) {
        deviceInfoMapper.removePrinter(printerId);
    }

    /**
     * 设备是否已经绑定了扫描器
     *
     * @param deviceIdList 设备ID
     * @return 是否已经绑定了扫描器
     */
    @Override
    public String checkDeviceHasScanner(List<Long> deviceIdList) {
        for(Long deviceId : deviceIdList){
            //查询设备信息
            DeviceInfo deviceInfo = deviceInfoMapper.selectDeviceInfoById(deviceId);
            if(ObjectUtils.isNotEmpty(deviceInfo) && ObjectUtils.isEmpty(deviceInfo.getScannerId())){
                return "设备'" + deviceInfo.getName() + "'未绑定扫描器";
            }
        }
        return "true";
    }
}

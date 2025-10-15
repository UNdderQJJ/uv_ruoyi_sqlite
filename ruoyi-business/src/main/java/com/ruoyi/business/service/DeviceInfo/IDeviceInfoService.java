package com.ruoyi.business.service.DeviceInfo;

import java.util.List;
import com.ruoyi.business.domain.DeviceInfo.DeviceInfo;
import com.ruoyi.common.core.page.PageQuery;
import com.ruoyi.common.core.page.PageResult;

/**
 * 设备信息Service接口
 * 
 * @author ruoyi
 * @date 2025-01-30
 */
public interface IDeviceInfoService 
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
     * 分页查询设备信息列表
     * @param deviceInfo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    public PageResult<DeviceInfo> selectDeviceInfoPageList(DeviceInfo deviceInfo, PageQuery pageQuery);

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
     * @param id 设备ID
     * @param status 设备状态
     * @return 结果
     */
    public int updateDeviceStatus(Long id, String status);

    /**
     * 更新设备心跳时间
     * 
     * @param id 设备ID
     * @return 结果
     */
    public int updateDeviceHeartbeat(Long id);

    /**
     * 更新设备当前任务
     * 
     * @param id 设备ID
     * @param taskId 任务ID
     * @return 结果
     */
    public int updateDeviceCurrentTask(Long id, Long taskId);

    /**
     * 批量删除设备信息
     * 
     * @param ids 需要删除的设备信息主键集合
     * @return 结果
     */
    public int deleteDeviceInfoByIds(Long[] ids);

    /**
     * 删除设备信息信息
     * 
     * @param id 设备信息主键
     * @return 结果
     */
    public int deleteDeviceInfoById(Long id);

    /**
     * 检查设备UUID是否存在
     * 
     * @param deviceUuid 设备UUID
     * @return 结果
     */
    public boolean checkDeviceUuidUnique(String deviceUuid);

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

    /**
     * 统计各状态设备数量
     *
     * @return 设备状态统计结果
     */
    public List<DeviceInfo> countDeviceInfoByStatus();

    /**
     * 启用/禁用设备
     * 
     * @param id 设备ID
     * @param isEnabled 是否启用(0:禁用, 1:启用)
     * @return 结果
     */
    public int enableDevice(Long id, Integer isEnabled);

    /**
     * 批量启用/禁用设备
     * 
     * @param ids 设备ID数组
     * @param isEnabled 是否启用(0:禁用, 1:启用)
     * @return 结果
     */
    public int batchEnableDevice(Long[] ids, Integer isEnabled);

    /**
     * 更新设备当前任务
     *
     * @param deviceIds 设备ID数组
     * @param id 任务ID
     * @return 结果
     */
    void updateCurrentTask(List<String> deviceIds, Long id);

    /**
     * 为相应设备移除当前任务
     * @param taskId 任务id
     */
    void removeCurrentTask(Long taskId);

    /**
     * 为相应设备移除当前任务
     * @param deviceId 设备id
     * @param status 设备状态
     */
    void removeCurrentTask(Long deviceId,String status);

    /**
     * 更新扫描器绑定信息
     * @param scannerId 扫描器id
     * @param deviceId 设备id
     * @param deviceName 设备名称
     */
    void updateScanner(Long scannerId, Long deviceId, String deviceName);

    /**
     * 移除扫描器绑定信息
     * @param scannerId 扫描器id
     */
    void removeScanner(Long scannerId);

    /**
     * 移除打印机绑定信息
     * @param printerId 打印机id
     */
    void removePrinter(Long printerId);

    /**
     * 检查设备是否绑定了扫描器
     * @param deviceIdList 设备id
     * @return true:已绑定
     */
    String checkDeviceHasScanner(List<Long> deviceIdList);
}

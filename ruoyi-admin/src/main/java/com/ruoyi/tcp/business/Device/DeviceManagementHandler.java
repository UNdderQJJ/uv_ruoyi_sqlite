package com.ruoyi.tcp.business.Device;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.domain.DeviceInfo.DeviceInfo;
import com.ruoyi.business.service.DeviceInfo.IDeviceInfoService;
import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.utils.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 设备管理TCP处理器
 * 
 * @author ruoyi
 */
@Component
public class DeviceManagementHandler {

    private static final Logger log = LoggerFactory.getLogger(DeviceManagementHandler.class);

    @Autowired
    private IDeviceInfoService deviceInfoService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理设备管理相关请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @return 响应结果
     */
    public TcpResponse handleDeviceRequest(String path, String body) {
        try {
            log.info("处理设备管理请求: path={}, body={}", path, body);

            if (path.equals("/business/device/list")) {
                return listDevices(body);
            } else if (path.equals("/business/device/listByType")) {
                return listDevicesByType(body);
            } else if (path.equals("/business/device/listByStatus")) {
                return listDevicesByStatus(body);
            } else if (path.equals("/business/device/listEnabled")) {
                return listEnabledDevices();
            } else if (path.equals("/business/device/listOnline")) {
                return listOnlineDevices();
            } else if (path.equals("/business/device/get")) {
                return getDevice(body);
            } else if (path.equals("/business/device/getByUuid")) {
                return getDeviceByUuid(body);
            } else if (path.equals("/business/device/create")) {
                return createDevice(body);
            } else if (path.equals("/business/device/update")) {
                return updateDevice(body);
            } else if (path.equals("/business/device/delete")) {
                return deleteDevice(body);
            } else if (path.equals("/business/device/updateStatus")) {
                return updateDeviceStatus(body);
            } else if (path.equals("/business/device/updateHeartbeat")) {
                return updateDeviceHeartbeat(body);
            } else if (path.equals("/business/device/updateCurrentTask")) {
                return updateDeviceCurrentTask(body);
            } else if (path.equals("/business/device/enable")) {
                return enableDevice(body);
            } else if (path.equals("/business/device/batchEnable")) {
                return batchEnableDevice(body);
            } else if (path.equals("/business/device/count")) {
                return countDevices(body);
            } else if (path.equals("/business/device/countByType")) {
                return countDevicesByType();
            } else if (path.equals("/business/device/tree")) {
                return getDeviceTree();
            } else {
                return TcpResponse.error("未知的设备管理请求路径: " + path);
            }
        } catch (Exception e) {
            log.error("处理设备管理请求异常: path={}, body={}", path, body, e);
            return TcpResponse.error("处理设备管理请求异常: " + e.getMessage());
        }
    }

    /**
     * 查询设备列表
     */
    private TcpResponse listDevices(String body) {
        try {
            DeviceInfo deviceInfo = null;
            if (body != null && !body.trim().isEmpty()) {
                // 如果有请求体，则解析查询条件
                deviceInfo = objectMapper.readValue(body, DeviceInfo.class);
            }
            List<DeviceInfo> list = deviceInfoService.selectDeviceInfoList(deviceInfo);
            return TcpResponse.success(list);
        } catch (Exception e) {
            log.error("查询设备列表异常", e);
            return TcpResponse.error("查询设备列表异常: " + e.getMessage());
        }
    }

    /**
     * 根据设备类型查询设备列表
     */
    private TcpResponse listDevicesByType(String body) {
        try {
            if (body == null || body.trim().isEmpty()) {
                return TcpResponse.error("请求体不能为空，需要指定设备类型");
            }
            DeviceInfo deviceInfo = objectMapper.readValue(body, DeviceInfo.class);
            String deviceType = deviceInfo.getDeviceType();
            if (StringUtils.isEmpty(deviceType)) {
                return TcpResponse.error("设备类型不能为空");
            }
            List<DeviceInfo> list = deviceInfoService.selectDeviceInfoListByType(deviceType);
            return TcpResponse.success(list);
        } catch (Exception e) {
            log.error("根据设备类型查询设备列表异常", e);
            return TcpResponse.error("根据设备类型查询设备列表异常: " + e.getMessage());
        }
    }

    /**
     * 根据设备状态查询设备列表
     */
    private TcpResponse listDevicesByStatus(String body) {
        try {
            if (body == null || body.trim().isEmpty()) {
                return TcpResponse.error("请求体不能为空，需要指定设备状态");
            }
            DeviceInfo deviceInfo = objectMapper.readValue(body, DeviceInfo.class);
            String status = deviceInfo.getStatus();
            if (StringUtils.isEmpty(status)) {
                return TcpResponse.error("设备状态不能为空");
            }
            List<DeviceInfo> list = deviceInfoService.selectDeviceInfoListByStatus(status);
            return TcpResponse.success(list);
        } catch (Exception e) {
            log.error("根据设备状态查询设备列表异常", e);
            return TcpResponse.error("根据设备状态查询设备列表异常: " + e.getMessage());
        }
    }

    /**
     * 查询启用的设备列表
     */
    private TcpResponse listEnabledDevices() {
        try {
            List<DeviceInfo> list = deviceInfoService.selectEnabledDeviceInfoList();
            return TcpResponse.success(list);
        } catch (Exception e) {
            log.error("查询启用的设备列表异常", e);
            return TcpResponse.error("查询启用的设备列表异常: " + e.getMessage());
        }
    }

    /**
     * 查询在线设备列表
     */
    private TcpResponse listOnlineDevices() {
        try {
            List<DeviceInfo> list = deviceInfoService.selectOnlineDeviceInfoList();
            return TcpResponse.success(list);
        } catch (Exception e) {
            log.error("查询在线设备列表异常", e);
            return TcpResponse.error("查询在线设备列表异常: " + e.getMessage());
        }
    }

    /**
     * 获取设备详情
     */
    private TcpResponse getDevice(String body) {
        try {
            DeviceInfo deviceInfo = objectMapper.readValue(body, DeviceInfo.class);
            Long id = deviceInfo.getId();
            if (id == null) {
                return TcpResponse.error("设备ID不能为空");
            }
            DeviceInfo result = deviceInfoService.selectDeviceInfoById(id);
            if (result == null) {
                return TcpResponse.error("设备不存在");
            }
            return TcpResponse.success(result);
        } catch (Exception e) {
            log.error("获取设备详情异常", e);
            return TcpResponse.error("获取设备详情异常: " + e.getMessage());
        }
    }

    /**
     * 根据设备UUID获取设备详情
     */
    private TcpResponse getDeviceByUuid(String body) {
        try {
            if (body == null || body.trim().isEmpty()) {
                return TcpResponse.error("请求体不能为空，需要指定设备UUID");
            }
            DeviceInfo deviceInfo = objectMapper.readValue(body, DeviceInfo.class);
            String deviceUuid = deviceInfo.getDeviceUuid();
            if (StringUtils.isEmpty(deviceUuid)) {
                return TcpResponse.error("设备UUID不能为空");
            }
            DeviceInfo result = deviceInfoService.selectDeviceInfoByUuid(deviceUuid);
            if (result == null) {
                return TcpResponse.error("设备不存在");
            }
            return TcpResponse.success(result);
        } catch (Exception e) {
            log.error("根据设备UUID获取设备详情异常", e);
            return TcpResponse.error("根据设备UUID获取设备详情异常: " + e.getMessage());
        }
    }

    /**
     * 创建设备
     */
    private TcpResponse createDevice(String body) {
        try {
            if (body == null || body.trim().isEmpty()) {
                return TcpResponse.error("请求体不能为空，需要提供设备信息");
            }
            DeviceInfo deviceInfo = objectMapper.readValue(body, DeviceInfo.class);
            
            // 验证必填字段
            if (StringUtils.isEmpty(deviceInfo.getName())) {
                return TcpResponse.error("设备名称不能为空");
            }
            if (StringUtils.isEmpty(deviceInfo.getDeviceType())) {
                return TcpResponse.error("设备类型不能为空");
            }
            if (StringUtils.isEmpty(deviceInfo.getConnectionType())) {
                return TcpResponse.error("连接类型不能为空");
            }

            // UUID由内部自动生成，无需验证
            // 在setDeviceUuid方法中会自动生成UUID

            int result = deviceInfoService.insertDeviceInfo(deviceInfo);
            if (result > 0) {
                return TcpResponse.success("创建设备成功，设备UUID: " + deviceInfo.getDeviceUuid());
            } else {
                return TcpResponse.error("创建设备失败");
            }
        } catch (Exception e) {
            log.error("创建设备异常", e);
            return TcpResponse.error("创建设备异常: " + e.getMessage());
        }
    }

    /**
     * 更新设备
     */
    private TcpResponse updateDevice(String body) {
        try {
            if (body == null || body.trim().isEmpty()) {
                return TcpResponse.error("请求体不能为空，需要提供设备信息");
            }
            DeviceInfo deviceInfo = objectMapper.readValue(body, DeviceInfo.class);
            
            if (deviceInfo.getId() == null) {
                return TcpResponse.error("设备ID不能为空");
            }

            int result = deviceInfoService.updateDeviceInfo(deviceInfo);
            if (result > 0) {
                return TcpResponse.success("更新设备成功");
            } else {
                return TcpResponse.error("更新设备失败");
            }
        } catch (Exception e) {
            log.error("更新设备异常", e);
            return TcpResponse.error("更新设备异常: " + e.getMessage());
        }
    }

    /**
     * 删除设备
     */
    private TcpResponse deleteDevice(String body) {
        try {
            if (body == null || body.trim().isEmpty()) {
                return TcpResponse.error("请求体不能为空，需要提供设备ID");
            }
            DeviceInfo deviceInfo = objectMapper.readValue(body, DeviceInfo.class);
            
            if (deviceInfo.getIds() != null && deviceInfo.getIds().length > 0) {
                // 批量删除
                int result = deviceInfoService.deleteDeviceInfoByIds(deviceInfo.getIds());
                if (result > 0) {
                    return TcpResponse.success("批量删除设备成功");
                } else {
                    return TcpResponse.error("批量删除设备失败");
                }
            } else {
                // 单个删除
                Long id = deviceInfo.getId();
                if (id == null) {
                    return TcpResponse.error("设备ID不能为空");
                }
                int result = deviceInfoService.deleteDeviceInfoById(id);
                if (result > 0) {
                    return TcpResponse.success("删除设备成功");
                } else {
                    return TcpResponse.error("删除设备失败");
                }
            }
        } catch (Exception e) {
            log.error("删除设备异常", e);
            return TcpResponse.error("删除设备异常: " + e.getMessage());
        }
    }

    /**
     * 更新设备状态
     */
    private TcpResponse updateDeviceStatus(String body) {
        try {
            if (body == null || body.trim().isEmpty()) {
                return TcpResponse.error("请求体不能为空，需要提供设备ID和状态");
            }
            DeviceInfo deviceInfo = objectMapper.readValue(body, DeviceInfo.class);
            Long id = deviceInfo.getId();
            String status = deviceInfo.getStatus();
            
            if (id == null) {
                return TcpResponse.error("设备ID不能为空");
            }
            if (StringUtils.isEmpty(status)) {
                return TcpResponse.error("设备状态不能为空");
            }

            int result = deviceInfoService.updateDeviceStatus(id, status);
            if (result > 0) {
                return TcpResponse.success("更新设备状态成功");
            } else {
                return TcpResponse.error("更新设备状态失败");
            }
        } catch (Exception e) {
            log.error("更新设备状态异常", e);
            return TcpResponse.error("更新设备状态异常: " + e.getMessage());
        }
    }

    /**
     * 更新设备心跳时间
     */
    private TcpResponse updateDeviceHeartbeat(String body) {
        try {
            if (body == null || body.trim().isEmpty()) {
                return TcpResponse.error("请求体不能为空，需要提供设备ID");
            }
            DeviceInfo deviceInfo = objectMapper.readValue(body, DeviceInfo.class);
            Long id = deviceInfo.getId();
            
            if (id == null) {
                return TcpResponse.error("设备ID不能为空");
            }

            int result = deviceInfoService.updateDeviceHeartbeat(id);
            if (result > 0) {
                return TcpResponse.success("更新设备心跳成功");
            } else {
                return TcpResponse.error("更新设备心跳失败");
            }
        } catch (Exception e) {
            log.error("更新设备心跳异常", e);
            return TcpResponse.error("更新设备心跳异常: " + e.getMessage());
        }
    }

    /**
     * 更新设备当前任务
     */
    private TcpResponse updateDeviceCurrentTask(String body) {
        try {
            if (body == null || body.trim().isEmpty()) {
                return TcpResponse.error("请求体不能为空，需要提供设备ID和任务ID");
            }
            DeviceInfo deviceInfo = objectMapper.readValue(body, DeviceInfo.class);
            Long id = deviceInfo.getId();
            Long taskId = deviceInfo.getCurrentTaskId();
            
            if (id == null) {
                return TcpResponse.error("设备ID不能为空");
            }

            int result = deviceInfoService.updateDeviceCurrentTask(id, taskId);
            if (result > 0) {
                return TcpResponse.success("更新设备当前任务成功");
            } else {
                return TcpResponse.error("更新设备当前任务失败");
            }
        } catch (Exception e) {
            log.error("更新设备当前任务异常", e);
            return TcpResponse.error("更新设备当前任务异常: " + e.getMessage());
        }
    }

    /**
     * 启用/禁用设备
     */
    private TcpResponse enableDevice(String body) {
        try {
            if (body == null || body.trim().isEmpty()) {
                return TcpResponse.error("请求体不能为空，需要提供设备ID和启用状态");
            }
            DeviceInfo deviceInfo = objectMapper.readValue(body, DeviceInfo.class);
            Long id = deviceInfo.getId();
            Integer isEnabled = deviceInfo.getIsEnabled();
            
            if (id == null) {
                return TcpResponse.error("设备ID不能为空");
            }
            if (isEnabled == null) {
                return TcpResponse.error("启用状态不能为空");
            }

            int result = deviceInfoService.enableDevice(id, isEnabled);
            if (result > 0) {
                return TcpResponse.success("更新设备启用状态成功");
            } else {
                return TcpResponse.error("更新设备启用状态失败");
            }
        } catch (Exception e) {
            log.error("更新设备启用状态异常", e);
            return TcpResponse.error("更新设备启用状态异常: " + e.getMessage());
        }
    }

    /**
     * 批量启用/禁用设备
     */
    private TcpResponse batchEnableDevice(String body) {
        try {
            if (body == null || body.trim().isEmpty()) {
                return TcpResponse.error("请求体不能为空，需要提供设备ID列表和启用状态");
            }
            DeviceInfo deviceInfo = objectMapper.readValue(body, DeviceInfo.class);
            Long[] ids = deviceInfo.getIds();
            Integer isEnabled = deviceInfo.getIsEnabled();
            
            if (ids == null || ids.length == 0) {
                return TcpResponse.error("设备ID列表不能为空");
            }
            if (isEnabled == null) {
                return TcpResponse.error("启用状态不能为空");
            }

            int result = deviceInfoService.batchEnableDevice(ids, isEnabled);
            if (result > 0) {
                return TcpResponse.success("批量更新设备启用状态成功");
            } else {
                return TcpResponse.error("批量更新设备启用状态失败");
            }
        } catch (Exception e) {
            log.error("批量更新设备启用状态异常", e);
            return TcpResponse.error("批量更新设备启用状态异常: " + e.getMessage());
        }
    }

    /**
     * 统计设备数量
     */
    private TcpResponse countDevices(String body) {
        try {
            DeviceInfo deviceInfo = null;
            if (body != null && !body.trim().isEmpty()) {
                // 如果有请求体，则解析查询条件
                deviceInfo = objectMapper.readValue(body, DeviceInfo.class);
            }
            int count = deviceInfoService.countDeviceInfo(deviceInfo);
            return TcpResponse.success(count);
        } catch (Exception e) {
            log.error("统计设备数量异常", e);
            return TcpResponse.error("统计设备数量异常: " + e.getMessage());
        }
    }

    /**
     * 统计各类型设备数量
     */
    private TcpResponse countDevicesByType() {
        try {
            List<DeviceInfo> list = deviceInfoService.countDeviceInfoByType();
            return TcpResponse.success(list);
        } catch (Exception e) {
            log.error("统计各类型设备数量异常", e);
            return TcpResponse.error("统计设备数量异常: " + e.getMessage());
        }
    }

    /**
     * 获取设备树状图
     * 按设备类型分组，返回树状结构
     */
    private TcpResponse getDeviceTree() {
        try {
            // 获取所有启用的设备
            List<DeviceInfo> allDevices = deviceInfoService.selectEnabledDeviceInfoList();
            
            // 按设备类型分组
            Map<String, List<DeviceInfo>> deviceTypeMap = new HashMap<>();
            for (DeviceInfo device : allDevices) {
                String deviceType = device.getDeviceType();
                if (deviceType != null) {
                    deviceTypeMap.computeIfAbsent(deviceType, k -> new ArrayList<>()).add(device);
                }
            }
            
            // 构建树状结构
            List<Map<String, Object>> deviceTree = new ArrayList<>();
            for (Map.Entry<String, List<DeviceInfo>> entry : deviceTypeMap.entrySet()) {
                String deviceType = entry.getKey();
                List<DeviceInfo> devices = entry.getValue();
                
                Map<String, Object> categoryNode = new HashMap<>();
                categoryNode.put("categoryName", getDeviceTypeDisplayName(deviceType));
                categoryNode.put("deviceType", deviceType);
                categoryNode.put("deviceCount", devices.size());
                categoryNode.put("devices", devices);
                
                deviceTree.add(categoryNode);
            }
            
            return TcpResponse.success(deviceTree);
        } catch (Exception e) {
            log.error("获取设备树状图异常", e);
            return TcpResponse.error("获取设备树状图异常: " + e.getMessage());
        }
    }

    /**
     * 获取设备类型的中文显示名称
     */
    private String getDeviceTypeDisplayName(String deviceType) {
        if (deviceType == null) {
            return "未知类型";
        }
        
        switch (deviceType.toUpperCase()) {
            case "PRINTER":
                return "UV打印机";
            case "CODER":
                return "油墨喷码机";
            case "SCANNER":
                return "扫码枪";
            default:
                return deviceType;
        }
    }
}

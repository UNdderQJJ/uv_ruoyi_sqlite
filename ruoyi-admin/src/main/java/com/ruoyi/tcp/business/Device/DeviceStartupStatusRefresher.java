package com.ruoyi.tcp.business.Device;

import com.ruoyi.business.domain.DeviceInfo.DeviceInfo;
import com.ruoyi.business.enums.DeviceStatus;
import com.ruoyi.business.service.DeviceInfo.DeviceConnectionService;
import com.ruoyi.business.service.DeviceInfo.IDeviceInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 应用启动后遍历设备，刷新一次当前状态。
 */
@Component
@Order(20)
public class DeviceStartupStatusRefresher implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DeviceStartupStatusRefresher.class);

    @Autowired
    private IDeviceInfoService deviceInfoService;

    @Autowired
    private DeviceConnectionService deviceConnectionService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<DeviceInfo> enabledDevices = deviceInfoService.selectEnabledDeviceInfoList();
            if (enabledDevices == null || enabledDevices.isEmpty()) {
                return;
            }
            for (DeviceInfo device : enabledDevices) {
                try {
                    // 仅处理TCP类型
                    if (!"TCP".equalsIgnoreCase(device.getConnectionType())) {
                        continue;
                    }
                    boolean ok = deviceConnectionService.testTcpReachable(device.getIpAddress(), device.getPort(), 2000);
                    String newStatus = ok ? DeviceStatus.ONLINE_IDLE.getCode() : DeviceStatus.OFFLINE.getCode();
                    deviceInfoService.updateDeviceStatus(device.getId(), newStatus);
                    log.info("[StartupStatus] device={} ip={}:{} -> {}", device.getName(), device.getIpAddress(), device.getPort(), newStatus);
                } catch (Exception inner) {
                    log.warn("[StartupStatus] refresh failed for deviceId={} cause={} ", device.getId(), inner.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("设备启动状态刷新失败", e);
        }
    }
}



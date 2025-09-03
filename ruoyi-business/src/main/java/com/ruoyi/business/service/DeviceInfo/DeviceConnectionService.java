package com.ruoyi.business.service.DeviceInfo;

/**
 * 设备连接检测/操作服务（轻量，仅做TCP连通性探测）。
 */
public interface DeviceConnectionService {

    /**
     * 探测 TCP 是否可达。
     */
    boolean testTcpReachable(String ip, Integer port, int timeoutMs);
}



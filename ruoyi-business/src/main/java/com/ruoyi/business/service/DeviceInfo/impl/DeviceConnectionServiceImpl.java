package com.ruoyi.business.service.DeviceInfo.impl;

import com.ruoyi.business.service.DeviceInfo.DeviceConnectionService;
import com.ruoyi.business.utils.StxEtxProtocolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * 设备连接服务实现类
 */
@Service
public class DeviceConnectionServiceImpl implements DeviceConnectionService {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceConnectionServiceImpl.class);
    private static final int CONNECT_TIMEOUT = 3000; // 3秒连接超时
    private static final int READ_TIMEOUT = 2000;    // 2秒读取超时
    
    @Override
    public boolean testTcpReachable(String ip, Integer port, int timeoutMs) {
        if (ip == null || ip.isEmpty() || port == null || port <= 0) {
            return false;
        }
        
        Socket socket = null;
        try {
            // 建立连接
            socket = new Socket();
            int connectTimeout = Math.max(timeoutMs, CONNECT_TIMEOUT);
            socket.connect(new java.net.InetSocketAddress(ip, port), connectTimeout);
            socket.setSoTimeout(Math.max(timeoutMs / 2, READ_TIMEOUT));
            
            // 发送get_currfile命令,获取当前文件名
            String command = "get_currfile";
            byte[] commandBytes = StxEtxProtocolUtil.buildCommand(command);
            
            log.debug("发送命令到 {}:{} - {}", ip, port, StxEtxProtocolUtil.toHexString(commandBytes));
            
            // 发送命令
            socket.getOutputStream().write(commandBytes);
            socket.getOutputStream().flush();
            
            // 读取响应
            byte[] buffer = new byte[1024];
            int bytesRead = socket.getInputStream().read(buffer);
            
            if (bytesRead > 0) {
                byte[] response = new byte[bytesRead];
                System.arraycopy(buffer, 0, response, 0, bytesRead);
                
                log.debug("收到响应 - {}", StxEtxProtocolUtil.toHexString(response));
                log.debug("收到响应utf-8 - {}", new String(response, StandardCharsets.UTF_8));
                
                // 检查响应格式是否正确
                if (StxEtxProtocolUtil.isValidResponse(response)) {
                    String data = StxEtxProtocolUtil.parseResponse(response);
                    log.debug("解析响应数据: {}", data);
                    
                    // 如果响应包含文件名，说明设备正常
                    if (data != null && !data.trim().isEmpty()) {
                        log.info("设备 {}:{} 连接正常，当前文件: {}", ip, port, data);
                        return true;
                    }
                }
            }
            
            log.warn("设备 {}:{} 响应格式不正确或为空", ip, port);
            return false;
            
        } catch (SocketTimeoutException e) {
            log.warn("设备 {}:{} 连接超时", ip, port);
            return false;
        } catch (IOException e) {
            log.warn("设备 {}:{} 连接失败: {}", ip, port, e.getMessage());
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.debug("关闭socket异常: {}", e.getMessage());
                }
            }
        }
    }
}



package com.ruoyi.business.utils;

import java.nio.charset.StandardCharsets;

/**
 * STX/ETX协议工具类
 * 协议定义：
 * 发送数据：STX (0x02) + 0x05 + 命令 + ETX (0x03)
 * 返回数据：STX (0x02) + 0x06 + 数据 + ETX (0x03)
 */
public class StxEtxProtocolUtil {
    
    private static final byte STX = 0x02;
    private static final byte ETX = 0x03;
    private static final byte SEND_MARKER = 0x05;  // 发送数据标记
    private static final byte RECV_MARKER = 0x06;   // 接收数据标记
    
    /**
     * 构建发送命令
     * @param command 命令字符串
     * @return 协议字节数组
     */
    public static byte[] buildCommand(String command) {
        byte[] commandBytes = command.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[commandBytes.length + 3]; // STX + 0x05 + command + ETX
        
        int index = 0;
        result[index++] = STX;
        result[index++] = SEND_MARKER;
        System.arraycopy(commandBytes, 0, result, index, commandBytes.length);
        index += commandBytes.length;
        result[index] = ETX;
        
        return result;
    }
    
    /**
     * 解析返回数据
     * @param response 原始响应字节数组
     * @return 解析后的数据字符串，如果格式不正确返回null
     */
    public static String parseResponse(byte[] response) {
        if (response == null || response.length < 4) {
            return null;
        }
        
        // 检查开始和结束标记
        if (response[0] != STX || response[1] != RECV_MARKER || response[response.length - 1] != ETX) {
            return null;
        }
        
        // 提取数据部分（去掉STX + 0x06 + ETX）
        byte[] dataBytes = new byte[response.length - 3];
        System.arraycopy(response, 2, dataBytes, 0, dataBytes.length);
        
        return new String(dataBytes, StandardCharsets.UTF_8);
    }
    
    /**
     * 检查响应是否为有效的STX/ETX格式
     * @param response 响应字节数组
     * @return 是否为有效格式
     */
    public static boolean isValidResponse(byte[] response) {
        if (response == null || response.length < 4) {
            return false;
        }
        
        return response[0] == STX && response[1] == RECV_MARKER && response[response.length - 1] == ETX;
    }
    
    /**
     * 将字节数组转换为十六进制字符串（用于调试）
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    public static String toHexString(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }
        
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}

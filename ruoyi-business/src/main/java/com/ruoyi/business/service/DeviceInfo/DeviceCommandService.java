package com.ruoyi.business.service.DeviceInfo;

import com.ruoyi.business.enums.DeviceConfigKey;

import java.util.Map;

/**
 * 将配置应用到设备（渲染指令并发送）。
 */
public interface DeviceCommandService {

    void applyParameters(Long deviceId, Map<DeviceConfigKey, Object> params);

    void applySingle(Long deviceId, DeviceConfigKey key, Object param);

    boolean sendCommand(String ip, Integer port, String command);

    /**
     * 获取设备当前文件名称
     * 
     * @param deviceId 设备ID
     * @return 当前文件名称，如果获取失败返回null
     */
    String getCurrentFileName(Long deviceId);

    /**
     * 发送一次打印数据（seta）
     */
    void sendPrintData(Long deviceId, Object setaParam);

    /** 启动加工（start） */
    void start(Long deviceId);

    /** 停止加工（stop） */
    void stop(Long deviceId);

    /** 软件触发一次打印（trimark） */
    void triggerMark(Long deviceId);

    /** 获取系统状态（sys_sta 或 sys_sta:errsta） */
    String getSystemStatus(Long deviceId, boolean includeErrWarn);

    /** 获取缓冲区数量（geta） */
    Integer getBufferCount(Long deviceId);

    /** 获取序列号当前值（snum_index） */
    Integer getSerialCurrent(String serialName, boolean editMode, Long deviceId);

    /** 获取对象内容（get_textdata） */
    String getObjectText(String objectName, boolean editMode, Long deviceId);

    /** 获取加工次数（getcount） */
    Integer getProcessedCount(Long deviceId);

    /** 获取当前打印内容（get_currtext） */
    String getCurrentText(Long deviceId);

    /** 加载模板文件（load） */
    boolean loadFile(Long deviceId, String filePath);

    /** 获取文件列表（get_filelist） */
    String getFileList(Long deviceId);

    /** 清空数据缓冲区（clearbuf） */
    boolean clearBuffer(Long deviceId);

    /** 一键关机（pi_closeuv） */
    boolean powerOffPi(Long deviceId);

     /**
     * 将模版与实际数据合成为 SETA 指令参数结构。
     * 统一返回 Map 结构，便于直接传给 DeviceCommandServiceImpl 的 SETA 构建逻辑。
     *
     * @param text 文本内容（用于 v1）
     * @param width 宽度
     * @param height 高度
     * @param x X 坐标
     * @param y Y 坐标
     * @param r 旋转角度
     * @param ng 负向标志
     */
    Map<String, Object> buildSetaParam(String text, Integer width, Integer height, Integer x, Integer y, Integer r, Integer ng);
}



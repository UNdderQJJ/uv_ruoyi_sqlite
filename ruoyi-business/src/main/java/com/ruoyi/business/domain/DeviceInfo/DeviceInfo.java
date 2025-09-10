package com.ruoyi.business.domain.DeviceInfo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.utils.uuid.IdUtils;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;
import java.util.UUID;

/**
 * 设备信息对象 device_info
 * 
 * @author ruoyi
 * @date 2025-01-30
 */
@Data
public class DeviceInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 设备唯一标识符 */
    @Excel(name = "设备UUID")
    private String deviceUuid;

    /** 设备名称 */
    @Excel(name = "设备名称")
    private String name;

    /** 设备类型 */
    @Excel(name = "设备类型", readConverterExp = "PRINTER=打印机,CODER=喷码机,SCANNER=扫码枪")
    private String deviceType;

    /** 设备型号 */
    @Excel(name = "设备型号")
    private String model;

    /** 设备物理位置描述 */
    @Excel(name = "设备位置")
    private String location;

    /** 连接类型 */
    @Excel(name = "连接类型", readConverterExp = "TCP=TCP网络,SERIAL=串口")
    private String connectionType;

    /** IP地址 */
    @Excel(name = "IP地址")
    private String ipAddress;

    /** 端口号 */
    @Excel(name = "端口号")
    private Integer port;

    /** 串口号 */
    @Excel(name = "串口号")
    private String serialPort;

    /** 波特率 */
    @Excel(name = "波特率")
    private Integer baudRate;

    /** 数据位 */
    @Excel(name = "数据位")
    private Integer dataBits;

    /** 停止位 */
    @Excel(name = "停止位")
    private Integer stopBits;

    /** 校验位 */
    @Excel(name = "校验位", readConverterExp = "NONE=无校验,ODD=奇校验,EVEN=偶校验,MARK=标记校验,SPACE=空格校验")
    private String parity;

    /** 设备实时状态 */
    @Excel(name = "设备状态", readConverterExp = "OFFLINE=离线,ONLINE_IDLE=在线空闲,ONLINE_PRINTING=在线打印,ONLINE_SCANNING=在线扫描,ERROR=故障,MAINTENANCE=维护")
    private String status;

    /** 当前正在执行的任务ID */
    @Excel(name = "当前任务ID")
    private Long currentTaskId;

    /** 是否启用该设备 */
    @Excel(name = "是否启用", readConverterExp = "0=禁用,1=启用")
    private Integer isEnabled;

    /** 最后心跳时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最后心跳时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastHeartbeatTime;

    /** 设备特定参数(JSON格式) */
    @Excel(name = "设备参数")
    private String parameters;

    /** 设备备注信息 */
    @Excel(name = "设备备注")
    private String description;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 批量操作时的ID数组 */
    private Long[] ids;

    /**
     * 设置设备UUID，如果为空则自动生成
     */
    public void setDeviceUuid(String deviceUuid) {
        if (deviceUuid == null || deviceUuid.trim().isEmpty()) {
            // 自动生成UUID，使用简化版本（去掉横线）
            this.deviceUuid = UUID.randomUUID().toString();
        } else {
            this.deviceUuid = deviceUuid;
        }
    }

}

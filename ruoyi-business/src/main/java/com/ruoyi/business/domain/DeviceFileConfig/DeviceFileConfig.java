package com.ruoyi.business.domain.DeviceFileConfig;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Data;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 设备文件配置对象 device_file_config
 * 
 * @author ruoyi
 * @date 2025-01-30
 */
@Data
public class DeviceFileConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 所属设备ID */
    @Excel(name = "设备ID")
    private Long deviceId;

    /** 模板文件名称 */
    @Excel(name = "模板文件名称")
    private String fileName;

    /** 变量名称 */
    @Excel(name = "变量名称")
    private String variableName;

    /** 变量的数据类型 */
    @Excel(name = "变量类型", readConverterExp = "TEXT=文本,NUMBER=数字,DATE=日期,SERIAL=序列号,QR_CODE=二维码")
    private String variableType;

    /** 固定数据内容 */
    @Excel(name = "固定数据内容")
    private String fixedContent;

    /** 是否为该设备默认配置 */
    @Excel(name = "是否默认配置", readConverterExp = "0=否,1=是")
    private Integer isDefault;

    /** 配置描述 */
    @Excel(name = "配置描述")
    private String description;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 批量操作时的ID数组 */
    private Long[] ids;

}

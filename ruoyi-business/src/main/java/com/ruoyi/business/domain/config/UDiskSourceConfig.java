package com.ruoyi.business.domain.config;

import lombok.Data;

/**
 * U盘导入配置
 * 
 * @author ruoyi
 */
@Data
public class UDiskSourceConfig extends SourceConfig {
    
    /** 文件路径 */
    private String filePath;
    
    /** 工作表名称或索引 */
    private String sheetNameOrIndex;
    
    /** 开始行号 */
    private Integer startRow;
    
    /** 数据列号 */
    private Integer dataColumn;
    
    /** 是否自动监控 */
    private Boolean autoMonitor;

}

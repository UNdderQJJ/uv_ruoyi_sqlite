package com.ruoyi.business.domain.config;

/**
 * U盘导入配置
 * 
 * @author ruoyi
 */
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getSheetNameOrIndex() {
        return sheetNameOrIndex;
    }

    public void setSheetNameOrIndex(String sheetNameOrIndex) {
        this.sheetNameOrIndex = sheetNameOrIndex;
    }

    public Integer getStartRow() {
        return startRow;
    }

    public void setStartRow(Integer startRow) {
        this.startRow = startRow;
    }

    public Integer getDataColumn() {
        return dataColumn;
    }

    public void setDataColumn(Integer dataColumn) {
        this.dataColumn = dataColumn;
    }

    public Boolean getAutoMonitor() {
        return autoMonitor;
    }

    public void setAutoMonitor(Boolean autoMonitor) {
        this.autoMonitor = autoMonitor;
    }
}

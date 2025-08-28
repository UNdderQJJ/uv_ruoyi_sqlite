package com.ruoyi.business.service.DataPool.UDisk;

import com.ruoyi.business.domain.DataPool;
import com.ruoyi.business.domain.config.UDiskSourceConfig;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.business.enums.PoolStatus;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.business.service.common.DataIngestionService;
import com.ruoyi.business.enums.ConnectionState;
import com.ruoyi.common.utils.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * U盘文件读取服务
 * 支持Excel和TXT文件的读取，并确保不会重复读取数据
 * 
 * @author ruoyi
 */
@Service
public class UDiskFileReaderService {
    private static final Logger log = LoggerFactory.getLogger(UDiskFileReaderService.class);

    @Autowired
    private IDataPoolService dataPoolService;
    
    @Autowired
    private DataIngestionService dataIngestionService;
    
    /**
     * 根据阈值读取U盘文件数据
     * 
     * @param dataPool 数据池对象
     * @param threshold 阈值，当待打印数据少于此值时触发读取
     * @param batchSize 每次读取的最大数据量
     * @return 新读取的数据量
     */
    public int readDataIfBelowThreshold(DataPool dataPool, int threshold, int batchSize) {
        // 检查数据池类型是否为U盘类型
        if (!"U_DISK".equals(dataPool.getSourceType())) {
            log.error("数据池类型不是U盘类型: {}", dataPool.getSourceType());
            return 0;
        }
        
        // 检查文件是否已经读取完成
        if ("1".equals(dataPool.getFileReadCompleted())) {
            log.debug("数据池 {} 的文件已经读取完成，无需再次读取", dataPool.getPoolName());
            return 0;
        }
        
        // 检查待打印数据量是否低于阈值
        if (dataPool.getPendingCount() > threshold) {
            log.debug("数据池 {} 待打印数据量 {} 未低于阈值 {}, 无需读取", 
                    dataPool.getPoolName(), dataPool.getPendingCount(), threshold);
            return 0;
        }
        
        // 解析U盘配置
        UDiskSourceConfig config;
        try {
            config = JSON.parseObject(dataPool.getSourceConfigJson(), UDiskSourceConfig.class);
        } catch (Exception e) {
            log.error("解析U盘配置失败: {}", e.getMessage());
            return 0;
        }
        
        // 获取文件路径
        String filePath = config.getFilePath();
        if (StringUtils.isEmpty(filePath)) {
            log.error("文件路径为空");
            return 0;
        }
        
        // 检查文件是否存在
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            log.error("文件不存在或不是一个文件: {}", filePath);
            // 更新连接状态为断开
            dataPoolService.updateConnectionState(dataPool.getId(), ConnectionState.DISCONNECTED.getCode());
            return 0;
        }
        
        // 根据文件类型调用不同的读取方法
        int readCount = 0;
        if (filePath.toLowerCase().endsWith(".xlsx") || filePath.toLowerCase().endsWith(".xls")) {
            // Excel文件读取
            readCount = readExcelFile(dataPool, config, batchSize);
        } else if (filePath.toLowerCase().endsWith(".txt") || filePath.toLowerCase().endsWith(".csv")) {
            // 文本文件读取
            readCount = readTextFile(dataPool, config, batchSize);
        } else {
            log.error("不支持的文件类型: {}", filePath);
            return 0;
        }
        
        // 统一入库服务已在内部处理计数更新，这里只记录日志
        if (readCount > 0) {
            log.info("数据池 {} 成功读取 {} 条数据", dataPool.getPoolName(), readCount);
        }
        
        return readCount;
    }
    
    /**
     * 读取Excel文件
     * 
     * @param dataPool 数据池对象
     * @param config U盘配置
     * @param batchSize 每次读取的最大数据量
     * @return 读取的数据量
     */
    private int readExcelFile(DataPool dataPool, UDiskSourceConfig config, int batchSize) {
        String filePath = config.getFilePath();
        int startRow = config.getStartRow() != null ? config.getStartRow() : 1;
        int dataColumn = config.getDataColumn() != null ? config.getDataColumn() : 1;
        String sheetNameOrIndex = config.getSheetNameOrIndex();
        
        // 获取上次读取位置
        int lastReadRow = getLastReadPosition(dataPool);
        int currentRow = Math.max(startRow, lastReadRow + 1);
        
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            // 获取工作表
            Sheet sheet;
            if (StringUtils.isNumeric(sheetNameOrIndex)) {
                int sheetIndex = Integer.parseInt(sheetNameOrIndex);
                if (sheetIndex >= 0 && sheetIndex < workbook.getNumberOfSheets()) {
                    sheet = workbook.getSheetAt(sheetIndex);
                } else {
                    sheet = workbook.getSheetAt(0);
                }
            } else {
                sheet = workbook.getSheet(sheetNameOrIndex);
                if (sheet == null) {
                    sheet = workbook.getSheetAt(0);
                }
            }
            
            // 读取数据
            List<String> dataList = new ArrayList<>();
            int rowCount = 0;
            
            for (int i = currentRow - 1; i < sheet.getPhysicalNumberOfRows() && rowCount < batchSize; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Cell cell = row.getCell(dataColumn - 1);
                if (cell == null) continue;
                
                String cellValue = getCellValueAsString(cell);
                if (StringUtils.isNotEmpty(cellValue)) {
                    dataList.add(cellValue);
                    rowCount++;
                    currentRow = i + 1;
                }
            }
            
            // 检查是否已经读取到文件末尾
            boolean isFileCompleted = currentRow >= sheet.getPhysicalNumberOfRows();
            
            // 保存数据到数据池
            if (!dataList.isEmpty()) {
                // 统一走数据入库服务，内部负责统计更新
                dataIngestionService.ingestItems(dataPool.getId(), dataList);
                int insertCount = dataList.size();
                
                // 更新最后读取位置
                updateLastReadPosition(dataPool, currentRow);
                
                // 如果文件读取完成，更新标志
                if (isFileCompleted) {
                    updateFileReadCompleted(dataPool, true);
                }
                
                return insertCount;
            }
            
            // 即使没有新数据，如果已经到达文件末尾，也要标记为完成
            if (isFileCompleted) {
                updateFileReadCompleted(dataPool, true);
            }
            
        } catch (Exception e) {
            log.error("读取Excel文件失败: {}", e.getMessage(), e);
        }
        
        return 0;
    }
    
    /**
     * 读取文本文件
     * 
     * @param dataPool 数据池对象
     * @param config U盘配置
     * @param batchSize 每次读取的最大数据量
     * @return 读取的数据量
     */
    private int readTextFile(DataPool dataPool, UDiskSourceConfig config, int batchSize) {
        String filePath = config.getFilePath();
        int startRow = config.getStartRow() != null ? config.getStartRow() : 1;
        
        // 获取上次读取位置
        int lastReadRow = getLastReadPosition(dataPool);
        int currentRow = Math.max(startRow, lastReadRow + 1);
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            
            // 跳过已读取的行
            for (int i = 1; i < currentRow; i++) {
                reader.readLine();
            }
            
            // 读取数据
            List<String> dataList = new ArrayList<>();
            String line;
            int rowCount = 0;
            
            while ((line = reader.readLine()) != null && rowCount < batchSize) {
                if (StringUtils.isNotEmpty(line)) {
                    dataList.add(line);
                    rowCount++;
                    currentRow++;
                }
            }
            
            // 检查是否已经读取到文件末尾
            boolean isFileCompleted = line == null; // 如果line为null，说明已经读到文件末尾
            
            // 保存数据到数据池
            if (!dataList.isEmpty()) {
                // 统一走数据入库服务，内部负责统计更新
                dataIngestionService.ingestItems(dataPool.getId(), dataList);
                int insertCount = dataList.size();
                
                // 更新最后读取位置
                updateLastReadPosition(dataPool, currentRow - 1);
                
                // 如果文件读取完成，更新标志
                if (isFileCompleted) {
                    updateFileReadCompleted(dataPool, true);
                }
                
                return insertCount;
            }
            
            // 即使没有新数据，如果已经到达文件末尾，也要标记为完成
            if (isFileCompleted) {
                updateFileReadCompleted(dataPool, true);
            }
            
        } catch (Exception e) {
            log.error("读取文本文件失败: {}", e.getMessage(), e);
        }
        
        return 0;
    }
    
    /**
     * 获取单元格的值作为字符串
     * 
     * @param cell Excel单元格
     * @return 单元格值的字符串表示
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return DateUtils.parseDateToStr("yyyy-MM-dd HH:mm:ss", cell.getDateCellValue());
                } else {
                    // 避免科学计数法
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception ex) {
                        return "";
                    }
                }
            default:
                return "";
        }
    }
    
    /**
     * 获取数据池的上次读取位置
     * 
     * @param dataPool 数据池对象
     * @return 上次读取的行号
     */
    private int getLastReadPosition(DataPool dataPool) {
        String positionStr = dataPool.getRemark();
        if (StringUtils.isEmpty(positionStr)) {
            return 0;
        }
        
        try {
            return Integer.parseInt(positionStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * 更新数据池的上次读取位置
     * 
     * @param dataPool 数据池对象
     * @param position 当前读取位置
     */
    private void updateLastReadPosition(DataPool dataPool, int position) {
        DataPool dataPoolNew = new DataPool();
        dataPoolNew.setId(dataPool.getId());
        dataPoolNew.setRemark(String.valueOf(position));
        dataPoolService.updateDataPool(dataPoolNew);
    }
    
    /**
     * 更新数据池的文件读取完成标志
     * 
     * @param dataPool 数据池对象
     * @param completed 是否完成
     */
    private void updateFileReadCompleted(DataPool dataPool, boolean completed) {
        dataPool.setFileReadCompleted(completed ? "1" : "0");
        dataPoolService.updateDataPoolStatus(dataPool.getId(),PoolStatus.WINING.getCode());
        dataPool.setStatus(PoolStatus.WINING.getCode());
        dataPoolService.updateDataPool(dataPool);
        log.info("数据池 {} 的文件读取完成标志已更新为 {}", dataPool.getPoolName(), completed);
    }
    
    /**
     * 重置数据池的文件读取位置
     * 
     * @param poolId 数据池ID
     * @param position 重置的位置（可选，为null时重置到配置的起始行或默认的1）
     * @return 是否重置成功
     */
    public boolean resetReadPosition(Long poolId, Integer position) {
        // 查询数据池
        DataPool dataPool = dataPoolService.selectDataPoolById(poolId);
        if (dataPool == null) {
            log.error("数据池不存在: {}", poolId);
            return false;
        }
        
        // 检查数据池类型
        if (!"U_DISK".equals(dataPool.getSourceType())) {
            log.error("数据池类型不是U盘类型: {}", dataPool.getSourceType());
            return false;
        }
        
        int resetPosition;
        if (position != null) {
            // 使用指定位置
            resetPosition = position;
        } else {
            // 从配置中获取起始行
            try {
                UDiskSourceConfig config = JSON.parseObject(dataPool.getSourceConfigJson(), UDiskSourceConfig.class);
                resetPosition = config.getStartRow() != null ? config.getStartRow() : 1;
            } catch (Exception e) {
                log.error("解析U盘配置失败: {}", e.getMessage());
                resetPosition = 1; // 默认重置到第1行
            }
        }
        
        // 更新位置
        dataPool.setRemark(String.valueOf(resetPosition - 1)); // 减1是因为下次读取会从这个位置的下一行开始
        // 重置文件读取完成标志
        dataPool.setFileReadCompleted("0");
        dataPoolService.updateDataPool(dataPool);
        
        log.info("数据池 {} 的文件读取位置已重置为 {}, 文件读取完成标志已重置", dataPool.getPoolName(), resetPosition);
        return true;
    }
}

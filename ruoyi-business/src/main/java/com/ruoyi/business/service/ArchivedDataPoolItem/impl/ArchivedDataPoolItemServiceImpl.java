package com.ruoyi.business.service.ArchivedDataPoolItem.impl;

import com.ruoyi.business.domain.ArchivedDataPoolItem;
import com.ruoyi.business.domain.DataPoolItem;
import com.ruoyi.business.enums.ItemStatus;
import com.ruoyi.business.enums.VerificationStatus;
import com.ruoyi.business.mapper.ArchivedDataPoolItem.ArchivedDataPoolItemMapper;
import com.ruoyi.business.mapper.DataPoolItem.DataPoolItemMapper;
import com.ruoyi.business.service.ArchivedDataPoolItem.IArchivedDataPoolItemService;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.common.utils.DateUtils;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 归档数据池项目Service业务层处理
 * 
 * @author ruoyi
 */
@Service
public class ArchivedDataPoolItemServiceImpl implements IArchivedDataPoolItemService {
    private static final Logger log = LoggerFactory.getLogger(ArchivedDataPoolItemServiceImpl.class);

    @Resource
    private ArchivedDataPoolItemMapper archivedDataPoolItemMapper;

    @Resource
    private DataPoolItemMapper dataPoolItemMapper;

    @Resource
    private IDataPoolService dataPoolService;

    /**
     * 查询归档数据池项目
     * 
     * @param id 归档数据池项目主键
     * @return 归档数据池项目
     */
    @Override
    public ArchivedDataPoolItem selectArchivedDataPoolItemById(Long id) {
        return archivedDataPoolItemMapper.selectArchivedDataPoolItemById(id);
    }

    /**
     * 查询归档数据池项目列表
     * 
     * @param archivedDataPoolItem 归档数据池项目
     * @return 归档数据池项目
     */
    @Override
    public List<ArchivedDataPoolItem> selectArchivedDataPoolItemList(ArchivedDataPoolItem archivedDataPoolItem) {
        return archivedDataPoolItemMapper.selectArchivedDataPoolItemList(archivedDataPoolItem);
    }

    /**
     * 新增归档数据池项目
     * 
     * @param archivedDataPoolItem 归档数据池项目
     * @return 结果
     */
    @Override
    public int insertArchivedDataPoolItem(ArchivedDataPoolItem archivedDataPoolItem) {
        archivedDataPoolItem.setCreateTime(DateUtils.getNowDate());
        return archivedDataPoolItemMapper.insertArchivedDataPoolItem(archivedDataPoolItem);
    }

    /**
     * 修改归档数据池项目
     * 
     * @param archivedDataPoolItem 归档数据池项目
     * @return 结果
     */
    @Override
    public int updateArchivedDataPoolItem(ArchivedDataPoolItem archivedDataPoolItem) {
        archivedDataPoolItem.setUpdateTime(DateUtils.getNowDate());
        return archivedDataPoolItemMapper.updateArchivedDataPoolItem(archivedDataPoolItem);
    }

    /**
     * 批量删除归档数据池项目
     * 
     * @param ids 需要删除的归档数据池项目主键
     * @return 结果
     */
    @Override
    public int deleteArchivedDataPoolItemByIds(Long[] ids) {
        return archivedDataPoolItemMapper.deleteArchivedDataPoolItemByIds(ids);
    }

    /**
     * 删除归档数据池项目信息
     * 
     * @param id 归档数据池项目主键
     * @return 结果
     */
    @Override
    public int deleteArchivedDataPoolItemById(Long id) {
        return archivedDataPoolItemMapper.deleteArchivedDataPoolItemById(id);
    }

    /**
     * 根据数据池ID查询归档数据
     * 
     * @param poolId 数据池ID
     * @return 归档数据列表
     */
    @Override
    public List<ArchivedDataPoolItem> selectArchivedDataPoolItemByPoolId(Long poolId) {
        return archivedDataPoolItemMapper.selectArchivedDataPoolItemByPoolId(poolId);
    }

    /**
     * 根据时间范围查询归档数据
     * 
     * @param poolId 数据池ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 归档数据列表
     */
    @Override
    public List<ArchivedDataPoolItem> selectArchivedDataPoolItemByTimeRange(Long poolId, Date startTime, Date endTime) {
        return archivedDataPoolItemMapper.selectArchivedDataPoolItemByTimeRange(poolId, startTime, endTime);
    }

    /**
     * 根据状态查询归档数据
     * 
     * @param poolId 数据池ID
     * @param finalStatus 最终状态
     * @return 归档数据列表
     */
    @Override
    public List<ArchivedDataPoolItem> selectArchivedDataPoolItemByStatus(Long poolId, String finalStatus) {
        return archivedDataPoolItemMapper.selectArchivedDataPoolItemByStatus(poolId, finalStatus);
    }

    /**
     * 根据设备ID查询归档数据
     * 
     * @param deviceId 设备ID
     * @return 归档数据列表
     */
    @Override
    public List<ArchivedDataPoolItem> selectArchivedDataPoolItemByDeviceId(String deviceId) {
        return archivedDataPoolItemMapper.selectArchivedDataPoolItemByDeviceId(deviceId);
    }

    /**
     * 根据校验状态查询归档数据
     * 
     * @param poolId 数据池ID
     * @param verificationStatus 校验状态
     * @return 归档数据列表
     */
    @Override
    public List<ArchivedDataPoolItem> selectArchivedDataPoolItemByVerificationStatus(Long poolId, String verificationStatus) {
        return archivedDataPoolItemMapper.selectArchivedDataPoolItemByVerificationStatus(poolId, verificationStatus);
    }

    /**
     * 获取归档数据统计信息
     * 
     * @param poolId 数据池ID
     * @return 统计信息Map
     */
    @Override
    public Map<String, Object> getArchivedDataStatistics(Long poolId) {
        List<ArchivedDataPoolItem> archivedItems = archivedDataPoolItemMapper.getArchivedDataStatistics(poolId);
        
        Map<String, Object> statistics = new HashMap<>();
        int totalCount = archivedItems.size();
        int printedCount = 0;
        int failedCount = 0;
        int successVerificationCount = 0;
        int failVerificationCount = 0;
        
        for (ArchivedDataPoolItem item : archivedItems) {
            if (ItemStatus.PRINTED.getCode().equals(item.getFinalStatus())) {
                printedCount++;
            } else if (ItemStatus.FAILED.getCode().equals(item.getFinalStatus())) {
                failedCount++;
            }
            
            if (VerificationStatus.SUCCESS.getCode().equals(item.getVerificationStatus())) {
                successVerificationCount++;
            } else if (VerificationStatus.FAIL.getCode().equals(item.getVerificationStatus())) {
                failVerificationCount++;
            }
        }
        
        statistics.put("totalCount", totalCount);
        statistics.put("printedCount", printedCount);
        statistics.put("failedCount", failedCount);
        statistics.put("successVerificationCount", successVerificationCount);
        statistics.put("failVerificationCount", failVerificationCount);
        
        return statistics;
    }

    /**
     * 清理指定时间之前的归档数据
     * 
     * @param poolId 数据池ID
     * @param beforeTime 指定时间
     * @return 清理的数据量
     */
    @Override
    public int cleanArchivedDataBeforeTime(Long poolId, Date beforeTime) {
        return archivedDataPoolItemMapper.cleanArchivedDataBeforeTime(poolId, beforeTime);
    }

    /**
     * 根据ID检查归档数据是否存在
     * 
     * @param id 归档数据ID
     * @return 存在返回true，不存在返回false
     */
    @Override
    public boolean checkArchivedDataExists(Long id) {
        return archivedDataPoolItemMapper.checkArchivedDataExists(id) > 0;
    }

    /**
     * 归档热数据项
     * 将已完成的热数据项移动到归档表
     * 
     * @param dataPoolItem 热数据项
     * @param verificationData 校验数据
     * @param verificationStatus 校验状态
     * @return 归档结果
     */
    @Override
    @Transactional
    public boolean archiveDataPoolItem(DataPoolItem dataPoolItem, String verificationData, String verificationStatus) {
        try {
            // 检查是否已经归档
            if (checkArchivedDataExists(dataPoolItem.getId())) {
                log.warn("热数据项 {} 已经归档，跳过重复归档", dataPoolItem.getId());
                return true;
            }

            // 获取数据池信息
//            DataPool dataPool = dataPoolService.selectDataPoolById(dataPoolItem.getPoolId());
//            if (dataPool == null) {
//                log.error("数据池 {} 不存在，无法归档数据项 {}", dataPoolItem.getPoolId(), dataPoolItem.getId());
//                return false;
//            }

            // 创建归档数据项
            ArchivedDataPoolItem archivedItem = new ArchivedDataPoolItem();
            archivedItem.setId(dataPoolItem.getId()); // 保持原ID
            archivedItem.setItemData(dataPoolItem.getItemData());
            archivedItem.setFinalStatus(dataPoolItem.getStatus());
            archivedItem.setPrintCount(dataPoolItem.getPrintCount());
            archivedItem.setPoolId(dataPoolItem.getPoolId());
//            archivedItem.setPoolName(dataPool.getPoolName());
            archivedItem.setDeviceId(dataPoolItem.getDeviceId());
            archivedItem.setReceivedTime(dataPoolItem.getReceivedTime());
            archivedItem.setPrintedTime(DateUtils.getNowDate());
            archivedItem.setArchivedTime(DateUtils.getNowDate());
            archivedItem.setVerificationData(verificationData);
            archivedItem.setVerificationStatus(verificationStatus);
            archivedItem.setDelFlag("0");

            // 插入归档表
            int result = insertArchivedDataPoolItem(archivedItem);
            if (result > 0) {
                log.info("成功归档热数据项 {} 到归档表", dataPoolItem.getId());
                return true;
            } else {
                log.error("归档热数据项 {} 失败", dataPoolItem.getId());
                return false;
            }
        } catch (Exception e) {
            log.error("归档热数据项 {} 时发生异常", dataPoolItem.getId(), e);
            return false;
        }
    }

    /**
     * 批量归档热数据项
     * 
     * @param dataPoolItems 热数据项列表
     * @return 归档成功的数量
     */
    @Override
    @Transactional
    public int batchArchiveDataPoolItems(List<DataPoolItem> dataPoolItems) {
        int successCount = 0;
        
        for (DataPoolItem item : dataPoolItems) {
            if (archiveDataPoolItem(item, null, VerificationStatus.NOT_REQUIRED.getCode())) {
                successCount++;
            }
        }
        
        log.info("批量归档完成，成功归档 {} 条数据，总数 {} 条", successCount, dataPoolItems.size());
        return successCount;
    }

    /**
     * 根据打印状态归档数据
     * 自动归档所有已打印或失败的热数据项
     * 
     * @param poolId 数据池ID（可选，为null时处理所有数据池）
     * @return 归档的数据量
     */
    @Override
    @Transactional
    public int archiveByPrintStatus(Long poolId) {
        try {
            // 查询需要归档的热数据项（已打印或失败状态）
            DataPoolItem queryItem = new DataPoolItem();
            if (poolId != null) {
                queryItem.setPoolId(poolId);
            }

            List<DataPoolItem> itemsToArchive = new ArrayList<>();

            // 查询已打印的数据项
            queryItem.setStatus(ItemStatus.PRINTED.getCode());
            List<DataPoolItem> printedItems = dataPoolItemMapper.selectDataPoolItemList(queryItem);
            itemsToArchive.addAll(printedItems);
            
            // 查询失败的数据项
            queryItem.setStatus(ItemStatus.FAILED.getCode());
            List<DataPoolItem> failedItems = dataPoolItemMapper.selectDataPoolItemList(queryItem);
            itemsToArchive.addAll(failedItems);
            
            if (itemsToArchive.isEmpty()) {
                log.info("没有需要归档的数据项");
                return 0;
            }
            
            // 批量归档
            int archivedCount = batchArchiveDataPoolItems(itemsToArchive);
            
            log.info("自动归档完成，成功归档 {} 条数据", archivedCount);
            return archivedCount;
            
        } catch (Exception e) {
            log.error("自动归档数据时发生异常", e);
            return 0;
        }
    }

    /**
     * 获取归档数据的导出信息
     * 
     * @param poolId 数据池ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 导出数据列表
     */
    @Override
    public List<ArchivedDataPoolItem> getExportData(Long poolId, Date startTime, Date endTime) {
        return archivedDataPoolItemMapper.selectArchivedDataPoolItemByTimeRange(poolId, startTime, endTime);
    }

    /**
     * 更新校验信息
     * 
     * @param id 归档数据ID
     * @param verificationData 校验数据
     * @param verificationStatus 校验状态
     * @return 更新结果
     */
    @Override
    public boolean updateVerificationInfo(Long id, String verificationData, String verificationStatus) {
        try {
            ArchivedDataPoolItem item = new ArchivedDataPoolItem();
            item.setId(id);
            item.setVerificationData(verificationData);
            item.setVerificationStatus(verificationStatus);
            
            int result = updateArchivedDataPoolItem(item);
            if (result > 0) {
                log.info("成功更新归档数据 {} 的校验信息", id);
                return true;
            } else {
                log.error("更新归档数据 {} 的校验信息失败", id);
                return false;
            }
        } catch (Exception e) {
            log.error("更新归档数据 {} 的校验信息时发生异常", id, e);
            return false;
        }
    }

    /**
     * 根据设备ID批量更新校验状态
     * 
     * @param deviceId 设备ID
     * @param verificationStatus 校验状态
     * @return 更新的数据量
     */
    @Override
    public int updateVerificationStatusByDeviceId(String deviceId, String verificationStatus) {
        try {
            // 查询该设备的所有归档数据
            List<ArchivedDataPoolItem> items = selectArchivedDataPoolItemByDeviceId(deviceId);
            
            int updateCount = 0;
            for (ArchivedDataPoolItem item : items) {
                if (updateVerificationInfo(item.getId(), item.getVerificationData(), verificationStatus)) {
                    updateCount++;
                }
            }
            
            log.info("批量更新设备 {} 的校验状态完成，成功更新 {} 条数据", deviceId, updateCount);
            return updateCount;
            
        } catch (Exception e) {
            log.error("批量更新设备 {} 的校验状态时发生异常", deviceId, e);
            return 0;
        }
    }
}

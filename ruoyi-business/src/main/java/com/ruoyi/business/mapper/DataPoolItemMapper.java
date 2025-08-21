package com.ruoyi.business.mapper;

import com.ruoyi.business.domain.DataPoolItem;
import java.util.List;

/**
 * 数据池热数据Mapper接口
 * 
 * @author ruoyi
 */
public interface DataPoolItemMapper {
    
    /**
     * 查询数据池热数据
     * 
     * @param id 数据池热数据主键
     * @return 数据池热数据
     */
    public DataPoolItem selectDataPoolItemById(Long id);

    /**
     * 查询数据池热数据列表
     * 
     * @param dataPoolItem 数据池热数据
     * @return 数据池热数据集合
     */
    public List<DataPoolItem> selectDataPoolItemList(DataPoolItem dataPoolItem);

    /**
     * 根据数据池ID查询热数据列表
     * 
     * @param poolId 数据池ID
     * @return 数据池热数据集合
     */
    public List<DataPoolItem> selectDataPoolItemByPoolId(Long poolId);

    /**
     * 根据状态查询热数据列表
     * 
     * @param status 数据状态
     * @return 数据池热数据集合
     */
    public List<DataPoolItem> selectDataPoolItemByStatus(String status);

    /**
     * 获取待打印的数据项（按接收时间排序）
     * 
     * @param poolId 数据池ID（可选）
     * @param limit 限制数量
     * @return 待打印数据项列表
     */
    public List<DataPoolItem> selectPendingItems(Long poolId, Integer limit);

    /**
     * 锁定数据项（设置状态为PRINTING并设置deviceId）
     * 
     * @param id 数据项ID
     * @param deviceId 设备ID
     * @return 影响行数
     */
    public int lockDataPoolItem(Long id, String deviceId);

    /**
     * 批量锁定数据项
     * 
     * @param ids 数据项ID列表
     * @param deviceId 设备ID
     * @return 影响行数
     */
    public int batchLockDataPoolItems(List<Long> ids, String deviceId);

    /**
     * 更新数据项状态
     * 
     * @param id 数据项ID
     * @param status 新状态
     * @param printCount 打印次数
     * @return 影响行数
     */
    public int updateDataPoolItemStatus(Long id, String status, Integer printCount);

    /**
     * 释放锁定（清空deviceId，状态改为PENDING）
     * 
     * @param id 数据项ID
     * @return 影响行数
     */
    public int releaseLock(Long id);

    /**
     * 根据设备ID释放锁定
     * 
     * @param deviceId 设备ID
     * @return 影响行数
     */
    public int releaseLockByLockId(String deviceId);

    /**
     * 新增数据池热数据
     * 
     * @param dataPoolItem 数据池热数据
     * @return 结果
     */
    public int insertDataPoolItem(DataPoolItem dataPoolItem);

    /**
     * 批量新增数据池热数据
     * 
     * @param dataPoolItems 数据池热数据列表
     * @return 结果
     */
    public int batchInsertDataPoolItems(List<DataPoolItem> dataPoolItems);

    /**
     * 修改数据池热数据
     * 
     * @param dataPoolItem 数据池热数据
     * @return 结果
     */
    public int updateDataPoolItem(DataPoolItem dataPoolItem);

    /**
     * 删除数据池热数据
     * 
     * @param id 数据池热数据主键
     * @return 结果
     */
    public int deleteDataPoolItemById(Long id);

    /**
     * 批量删除数据池热数据
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDataPoolItemByIds(Long[] ids);

    /**
     * 根据数据池ID删除热数据
     * 
     * @param poolId 数据池ID
     * @return 结果
     */
    public int deleteDataPoolItemByPoolId(Long poolId);

    /**
     * 统计各状态的数据量
     * 
     * @param poolId 数据池ID（可选）
     * @return 状态统计结果
     */
    public List<java.util.Map<String, Object>> countByStatus(Long poolId);

    /**
     * 清理已打印成功的数据（可选功能）
     * 
     * @param poolId 数据池ID（可选）
     * @param beforeTime 时间限制（清理此时间之前的数据）
     * @return 清理的数据量
     */
    public int cleanPrintedData(Long poolId, java.util.Date beforeTime);
}

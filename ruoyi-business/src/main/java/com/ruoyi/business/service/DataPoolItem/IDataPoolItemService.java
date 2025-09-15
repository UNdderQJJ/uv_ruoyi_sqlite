package com.ruoyi.business.service.DataPoolItem;

import com.ruoyi.business.domain.DataPoolItem.DataPoolItem;
import com.ruoyi.common.core.page.PageQuery;
import com.ruoyi.common.core.page.PageResult;

import java.util.List;
import java.util.Map;
import java.util.Date;

/**
 * 数据池热数据Service接口
 * 
 * @author ruoyi
 */
public interface IDataPoolItemService {
    
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
     * 获取待打印的数据项（分页格式）
     * 
     * @param queryItem 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    public PageResult<DataPoolItem> selectPendingItemsPage(DataPoolItem queryItem, PageQuery pageQuery);

    /**
     * 批量更新数据项状态于关联设备
     *
     * @param itemList 数据项列表
     * @param status   新状态
     */
    public void updateDataPoolItemsStatus(List<DataPoolItem> itemList, String status);


    /**
     *  批量更新数据项状态
     * @param itemList 数据项列表
     * @param status   新状态
     */
    public void updateItemsStatus(List<DataPoolItem> itemList, String status);

    /**
     * 锁定数据项（设置状态为PRINTING并设置deviceId）
     * 
     * @param id 数据项ID
     * @param deviceId 设备ID
     * @return 是否成功
     */
    public boolean lockDataPoolItem(Long id, String deviceId);

    /**
     * 批量锁定数据项
     * 
     * @param ids 数据项ID列表
     * @param deviceId 设备ID
     * @return 成功锁定的数量
     */
    public int batchLockDataPoolItems(List<Long> ids, String deviceId);

    /**
     * 更新数据项状态
     * 
     * @param id 数据项ID
     * @param status 新状态
     * @param printCount 打印次数
     * @return 是否成功
     */
    public boolean updateDataPoolItemStatus(Long id, String status, Integer printCount);

    /**
     * 标记打印成功
     * 
     * @param id 数据项ID
     * @return 是否成功
     */
    public boolean markAsPrinted(Long id);

    /**
     * 标记打印失败（增加打印次数）
     * 
     * @param id 数据项ID
     * @return 是否成功
     */
    public boolean markAsFailed(Long id);

    /**
     * 释放锁定（清空deviceId，状态改为PENDING）
     * 
     * @param id 数据项ID
     * @return 是否成功
     */
    public boolean releaseLock(Long id);

    /**
     * 根据设备ID释放锁定
     * 
     * @param deviceId 设备ID
     * @return 影响的数据项数量
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
     * 添加新的数据项（从数据源获取后调用）
     * 
     * @param poolId 数据池ID
     * @param itemData 数据内容
     * @return 新增的数据项
     */
    public DataPoolItem addDataItem(Long poolId, String itemData);

    /**
     * 批量添加数据项
     * 
     * @param poolId 数据池ID
     * @param itemDataList 数据内容列表
     * @return 新增的数据项数量
     */
    public int batchAddDataItems(Long poolId, List<String> itemDataList);

    /**
     * 修改数据池热数据
     * 
     * @param dataPoolItem 数据池热数据
     * @return 结果
     */
    public int updateDataPoolItem(DataPoolItem dataPoolItem);

    /**
     * 批量删除数据池热数据
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDataPoolItemByIds(Long[] ids);

    /**
     * 删除数据池热数据信息
     * 
     * @param id 数据池热数据主键
     * @return 结果
     */
    public int deleteDataPoolItemById(Long id);

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
    public List<Map<String, Object>> countByStatus(Long poolId);

     /**
     * 统计各状态的数据量
     *
     * @param poolId 数据池ID（可选）
      * @param status 状态
     * @return 状态统计结果
     */
    public int countByStatus(Long poolId,String status);


    /**
     * 获取数据池统计信息
     * 
     * @param poolId 数据池ID
     * @return 统计信息
     */
    public Map<String, Object> getDataPoolStatistics(Long poolId);

    /**
     * 清理已打印成功的数据（可选功能）
     * 
     * @param poolId 数据池ID（可选）
     * @param beforeTime 时间限制（清理此时间之前的数据）
     * @return 清理的数据量
     */
    public int cleanPrintedData(Long poolId, Date beforeTime);

    /**
     * 重置失败的数据项（将FAILED状态改为PENDING，清空deviceId）
     * 
     * @param poolId 数据池ID（可选）
     * @return 重置的数据项数量
     */
    public int resetFailedItems(Long poolId);

    /**
     * 获取打印队列信息
     * 
     * @param poolId 数据池ID（可选）
     * @return 队列信息
     */
    public Map<String, Object> getPrintQueueInfo(Long poolId);

    /**
     * 获取设备已处理的数据项数量
     *
     * @param deviceId 设备ID
     * @param PoolId 任务ID
     * @param code 数据项状态码
     * @return 已处理的数据项数量
     */
    Long getCompletedCount(String deviceId, Long PoolId, String code);

    /**
     * 更新数据项为待打印状态
     *
     * @param poolId 数据池ID
     */
    void updateToPendingItem(Long poolId);

    /**
     * 统计非待打印的数据项数量
     *
     * @param id 数据项ID
     * @return 非待打印的数据项数量
     */
    int countByNotPending(Long id);
}

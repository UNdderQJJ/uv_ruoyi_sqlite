package com.ruoyi.business.service.DataPool;

import com.ruoyi.business.domain.DataPool.DataPool;
import java.util.List;

/**
 * 数据池Service接口
 * 
 * @author ruoyi
 */
public interface IDataPoolService 
{
    /**
     * 查询数据池列表
     * 
     * @param dataPool 数据池信息
     * @return 数据池集合
     */
    public List<DataPool> selectDataPoolList(DataPool dataPool);

    /**
     * 查询数据池详细
     * 
     * @param id 数据池主键
     * @return 数据池
     */
    public DataPool selectDataPoolById(Long id);

    /**
     * 新增数据池
     * 
     * @param dataPool 数据池
     * @return 结果
     */
    public int insertDataPool(DataPool dataPool);

    /**
     * 修改数据池
     * 
     * @param dataPool 数据池
     * @return 结果
     */
    public int updateDataPool(DataPool dataPool);

    /**
     * 批量删除数据池
     * 
     * @param ids 需要删除的数据池主键集合
     * @return 结果
     */
    public int deleteDataPoolByIds(Long[] ids);

    /**
     * 删除数据池信息
     * 
     * @param id 数据池主键
     * @return 结果
     */
    public int deleteDataPoolById(Long id);

    /**
     * 启动数据池
     * 
     * @param id 数据池主键
     * @return 结果
     */
    public int startDataPool(Long id);

    /**
     * 停止数据池
     * 
     * @param id 数据池主键
     * @return 结果
     */
    public int stopDataPool(Long id);

    /**
     * 更新数据池状态
     * 
     * @param id 数据池主键
     * @param status 状态
     * @return 结果
     */
    public int updateDataPoolStatus(Long id, String status);

    /**
     * 更新数据池计数
     * 
     * @param id 数据池主键
     * @param totalCount 总数据量
     * @param pendingCount 待打印数量
     * @return 结果
     */
    public int updateDataPoolCount(Long id, Long totalCount, Long pendingCount);

        /**
     * 更新数据池计数加1
     *
     * @param id 数据池主键
     * @param totalCount 总数据量
     * @param pendingCount 待打印数量
     * @return 结果
     */
    public void updateDataPoolCountNumber(Long id, int totalCount, int pendingCount);

    /**
     * 更新数据池连接状态
     * @param id 数据池主键
     * @param connectionState 连接状态
     * @return 结果
     */
    public int updateConnectionState(Long id, String connectionState);

    /**
     * 更新数据池待待打印id
     * @param poolId
     */
    void updateDataPendingCount(Long poolId);

    /**
     * 刷新待打印数据量
     */
    void refreshPendingCount();
}

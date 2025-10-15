package com.ruoyi.business.service.DataPool.impl;

import com.ruoyi.business.domain.DataPool.DataPool;
import com.ruoyi.business.domain.DataPoolItem.DataPoolItem;
import com.ruoyi.business.enums.ConnectionState;
import com.ruoyi.business.enums.PoolStatus;
import com.ruoyi.business.mapper.DataPool.DataPoolMapper;
import com.ruoyi.business.mapper.DataPoolItem.DataPoolItemMapper;
import com.ruoyi.business.service.DataPool.DataPoolSchedulerService;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.common.utils.DatabaseRetryUtil;
import com.ruoyi.common.utils.DateUtils;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 数据池Service业务层处理
 * 
 * @author ruoyi
 */
@Service
public class DataPoolServiceImpl implements IDataPoolService 
{
    @Resource
    private DataPoolMapper dataPoolMapper;

    @Resource
    private DataPoolItemMapper dataPoolItemMapper;


    /**
     * 查询数据池列表
     * 
     * @param dataPool 数据池信息
     * @return 数据池集合
     */
    @Override
    public List<DataPool> selectDataPoolList(DataPool dataPool)
    {
        return dataPoolMapper.selectDataPoolList(dataPool);
    }

    /**
     * 查询数据池详细
     * 
     * @param id 数据池主键
     * @return 数据池
     */
    @Override
    public DataPool selectDataPoolById(Long id)
    {
        return dataPoolMapper.selectDataPoolById(id);
    }

    /**
     * 新增数据池
     * 
     * @param dataPool 数据池
     * @return 结果
     */
    @Override
    public int insertDataPool(DataPool dataPool)
    {
        dataPool.setCreateTime(DateUtils.getNowDate());
        // 设置初始状态为闲置
        dataPool.setStatus(PoolStatus.IDLE.getCode());
        // 设置初始计数为0
        dataPool.setTotalCount(0L);
        dataPool.setPendingCount(0L);
        // 设置文件读取完成标志为未完成
        dataPool.setFileReadCompleted("0");
        return dataPoolMapper.insertDataPool(dataPool);
    }

    /**
     * 修改数据池
     * 
     * @param dataPool 数据池
     * @return 结果
     */
    @Override
    public int updateDataPool(DataPool dataPool)
    {
        dataPool.setUpdateTime(DateUtils.getNowDate());
        return dataPoolMapper.updateDataPool(dataPool);
    }

    /**
     * 批量删除数据池
     * 
     * @param ids 需要删除的数据池主键集合
     * @return 结果
     */
    @Override
    public int deleteDataPoolByIds(Long[] ids)
    {
        return dataPoolMapper.deleteDataPoolByIds(ids);
    }

    /**
     * 删除数据池信息
     * 
     * @param id 数据池主键
     * @return 结果
     */
    @Override
    public int deleteDataPoolById(Long id)
    {
        return dataPoolMapper.deleteDataPoolById(id);
    }

    /**
     * 启动数据池
     * 
     * @param id 数据池主键
     * @return 结果
     */
    @Override
    public int startDataPool(Long id)
    {
        // 获取当前状态
        DataPool currentPool = selectDataPoolById(id);
        if (currentPool == null) {
            return 0;
        }
        
        PoolStatus oldStatus = PoolStatus.fromCode(currentPool.getStatus());
        PoolStatus newStatus = PoolStatus.RUNNING;
        
        DataPool dataPool = new DataPool();
        dataPool.setId(id);
        dataPool.setStatus(newStatus.getCode());
        dataPool.setUpdateTime(DateUtils.getNowDate());

        return dataPoolMapper.updateDataPool(dataPool);
    }

    /**
     * 停止数据池
     * 
     * @param id 数据池主键
     * @return 结果
     */
    @Override
    public int stopDataPool(Long id)
    {
        // 获取当前状态
        DataPool currentPool = selectDataPoolById(id);
        if (currentPool == null) {
            return 0;
        }
        
        PoolStatus oldStatus = PoolStatus.fromCode(currentPool.getStatus());
        PoolStatus newStatus = PoolStatus.IDLE;
        
        DataPool dataPool = new DataPool();
        dataPool.setId(id);
        dataPool.setStatus(newStatus.getCode());
        dataPool.setUpdateTime(DateUtils.getNowDate());

        return dataPoolMapper.updateDataPool(dataPool);
    }

    /**
     * 更新数据池状态
     * 
     * @param id 数据池主键
     * @param status 状态
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public int updateDataPoolStatus(Long id, String status)
    {
        // 获取当前状态
        DataPool currentPool = selectDataPoolById(id);
        if (currentPool == null) {
            return 0;
        }

        DataPool dataPool = new DataPool();
        dataPool.setId(id);
        dataPool.setStatus(status);
        dataPool.setUpdateTime(DateUtils.getNowDate());


        return dataPoolMapper.updateDataPool(dataPool);
    }

    /**
     * 更新数据池计数
     * 
     * @param id 数据池主键
     * @param totalCount 总数据量
     * @param pendingCount 待打印数量
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public int updateDataPoolCount(Long id, Long totalCount, Long pendingCount)
    {
        DataPool dataPool = new DataPool();
        dataPool.setId(id);
        dataPool.setTotalCount(totalCount);
        dataPool.setPendingCount(pendingCount);
        dataPool.setUpdateTime(DateUtils.getNowDate());

         try {
            return DatabaseRetryUtil.executeWithRetry(() -> dataPoolMapper.updateDataPool(dataPool));
        } catch (Exception e) {
            throw new RuntimeException("更新数据池计数失败", e);
        }
    }

     /**
     * 更新数据池计数加1
     *
     * @param id 数据池主键
     * @param totalCount 总数据量
     * @param pendingCount 待打印数量
     * @return 结果
     */
    @Override
    public void updateDataPoolCountNumber(Long id, int totalCount, int pendingCount) {
       try {
             dataPoolMapper.updateDataPoolCountNumber(id, totalCount, pendingCount);
        } catch (Exception e) {
            throw new RuntimeException("更新数据池计数失败", e);
        }
    }

    /**
     * 更新数据池连接状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public int updateConnectionState(Long id, String connectionState)
    {
        // 获取当前状态
        DataPool currentPool = selectDataPoolById(id);
        if (currentPool == null) {
            return 0;
        }
        
        DataPool dataPool = new DataPool();
        dataPool.setId(id);
        dataPool.setConnectionState(connectionState);
        dataPool.setUpdateTime(DateUtils.getNowDate());

        return dataPoolMapper.updateDataPool(dataPool);
    }

    @Override
    public void updateDataPendingCount(Long poolId) {
         //查询数据池的待打印数量
            int pendingCount = dataPoolItemMapper.countByPending(poolId);
            //查询数据池的所有数量
            int totalCount = dataPoolItemMapper.countByAll(poolId);
        dataPoolMapper.updateDataPendingCount(poolId, pendingCount,totalCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void refreshPendingCount() {
        List<DataPool> dataPools = dataPoolMapper.selectDataPoolList(new DataPool());
        for (DataPool dataPool : dataPools) {
            //查询数据池的待打印数量
            int pendingCount = dataPoolItemMapper.countByPending(dataPool.getId());
            //查询数据池的所有数量
            int totalCount = dataPoolItemMapper.countByAll(dataPool.getId());
            //更新数据池的待打印数量
            dataPoolMapper.updateDataPendingCount(dataPool.getId(), pendingCount,totalCount);
        }
    }
}

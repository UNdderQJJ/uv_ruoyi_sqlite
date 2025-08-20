package com.ruoyi.business.service.impl;

import com.ruoyi.business.domain.DataPool;
import com.ruoyi.business.enums.PoolStatus;
import com.ruoyi.business.mapper.DataPoolMapper;
import com.ruoyi.business.service.IDataPoolService;
import com.ruoyi.common.utils.DateUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        DataPool dataPool = new DataPool();
        dataPool.setId(id);
        dataPool.setStatus(PoolStatus.RUNNING.getCode());
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
        DataPool dataPool = new DataPool();
        dataPool.setId(id);
        dataPool.setStatus(PoolStatus.IDLE.getCode());
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
    public int updateDataPoolStatus(Long id, String status)
    {
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
    public int updateDataPoolCount(Long id, Long totalCount, Long pendingCount)
    {
        DataPool dataPool = new DataPool();
        dataPool.setId(id);
        dataPool.setTotalCount(totalCount);
        dataPool.setPendingCount(pendingCount);
        dataPool.setUpdateTime(DateUtils.getNowDate());
        return dataPoolMapper.updateDataPool(dataPool);
    }
}

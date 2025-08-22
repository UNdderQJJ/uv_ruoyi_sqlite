package com.ruoyi.business.mapper.DataPool;

import com.ruoyi.business.domain.DataPool;
import java.util.List;

/**
 * 数据池Mapper接口
 * 
 * @author ruoyi
 */
public interface DataPoolMapper 
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
     * 删除数据池
     * 
     * @param id 数据池主键
     * @return 结果
     */
    public int deleteDataPoolById(Long id);

    /**
     * 批量删除数据池
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDataPoolByIds(Long[] ids);
}

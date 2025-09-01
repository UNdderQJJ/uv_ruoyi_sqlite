package com.ruoyi.business.mapper.ArchivedDataPoolItem;

import com.ruoyi.business.domain.ArchivedDataPoolItem.ArchivedDataPoolItem;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 归档数据池项目Mapper接口
 * 
 * @author ruoyi
 */
public interface ArchivedDataPoolItemMapper {
    
    /**
     * 查询归档数据池项目
     * 
     * @param id 归档数据池项目主键
     * @return 归档数据池项目
     */
    public ArchivedDataPoolItem selectArchivedDataPoolItemById(Long id);

    /**
     * 查询归档数据池项目列表
     * 
     * @param archivedDataPoolItem 归档数据池项目
     * @return 归档数据池项目集合
     */
    public List<ArchivedDataPoolItem> selectArchivedDataPoolItemList(ArchivedDataPoolItem archivedDataPoolItem);

    /**
     * 新增归档数据池项目
     * 
     * @param archivedDataPoolItem 归档数据池项目
     * @return 结果
     */
    public int insertArchivedDataPoolItem(ArchivedDataPoolItem archivedDataPoolItem);

    /**
     * 修改归档数据池项目
     * 
     * @param archivedDataPoolItem 归档数据池项目
     * @return 结果
     */
    public int updateArchivedDataPoolItem(ArchivedDataPoolItem archivedDataPoolItem);

    /**
     * 删除归档数据池项目
     * 
     * @param id 归档数据池项目主键
     * @return 结果
     */
    public int deleteArchivedDataPoolItemById(Long id);

    /**
     * 批量删除归档数据池项目
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteArchivedDataPoolItemByIds(Long[] ids);

    /**
     * 根据数据池ID查询归档数据统计
     * 
     * @param poolId 数据池ID
     * @return 统计信息
     */
    public List<ArchivedDataPoolItem> selectArchivedDataPoolItemByPoolId(Long poolId);

    /**
     * 根据时间范围查询归档数据
     * 
     * @param poolId 数据池ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 归档数据列表
     */
    public List<ArchivedDataPoolItem> selectArchivedDataPoolItemByTimeRange(
            @Param("poolId") Long poolId, 
            @Param("startTime") Date startTime, 
            @Param("endTime") Date endTime);

    /**
     * 根据状态查询归档数据
     * 
     * @param poolId 数据池ID
     * @param finalStatus 最终状态
     * @return 归档数据列表
     */
    public List<ArchivedDataPoolItem> selectArchivedDataPoolItemByStatus(
            @Param("poolId") Long poolId, 
            @Param("finalStatus") String finalStatus);

    /**
     * 根据设备ID查询归档数据
     * 
     * @param deviceId 设备ID
     * @return 归档数据列表
     */
    public List<ArchivedDataPoolItem> selectArchivedDataPoolItemByDeviceId(String deviceId);

    /**
     * 根据校验状态查询归档数据
     * 
     * @param poolId 数据池ID
     * @param verificationStatus 校验状态
     * @return 归档数据列表
     */
    public List<ArchivedDataPoolItem> selectArchivedDataPoolItemByVerificationStatus(
            @Param("poolId") Long poolId, 
            @Param("verificationStatus") String verificationStatus);

    /**
     * 获取归档数据统计信息
     * 
     * @param poolId 数据池ID
     * @return 统计信息Map
     */
    public List<ArchivedDataPoolItem> getArchivedDataStatistics(Long poolId);

    /**
     * 清理指定时间之前的归档数据
     * 
     * @param poolId 数据池ID
     * @param beforeTime 指定时间
     * @return 清理的数据量
     */
    public int cleanArchivedDataBeforeTime(@Param("poolId") Long poolId, @Param("beforeTime") Date beforeTime);

    /**
     * 根据ID检查归档数据是否存在
     * 
     * @param id 归档数据ID
     * @return 存在返回1，不存在返回0
     */
    public int checkArchivedDataExists(Long id);
}

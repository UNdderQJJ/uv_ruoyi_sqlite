package com.ruoyi.business.service;

import com.ruoyi.business.domain.ArchivedDataPoolItem;
import com.ruoyi.business.domain.DataPoolItem;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 归档数据池项目Service接口
 * 
 * @author ruoyi
 */
public interface IArchivedDataPoolItemService {
    
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
     * 批量删除归档数据池项目
     * 
     * @param ids 需要删除的归档数据池项目主键集合
     * @return 结果
     */
    public int deleteArchivedDataPoolItemByIds(Long[] ids);

    /**
     * 删除归档数据池项目信息
     * 
     * @param id 归档数据池项目主键
     * @return 结果
     */
    public int deleteArchivedDataPoolItemById(Long id);

    /**
     * 根据数据池ID查询归档数据
     * 
     * @param poolId 数据池ID
     * @return 归档数据列表
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
    public List<ArchivedDataPoolItem> selectArchivedDataPoolItemByTimeRange(Long poolId, Date startTime, Date endTime);

    /**
     * 根据状态查询归档数据
     * 
     * @param poolId 数据池ID
     * @param finalStatus 最终状态
     * @return 归档数据列表
     */
    public List<ArchivedDataPoolItem> selectArchivedDataPoolItemByStatus(Long poolId, String finalStatus);

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
    public List<ArchivedDataPoolItem> selectArchivedDataPoolItemByVerificationStatus(Long poolId, String verificationStatus);

    /**
     * 获取归档数据统计信息
     * 
     * @param poolId 数据池ID
     * @return 统计信息Map
     */
    public Map<String, Object> getArchivedDataStatistics(Long poolId);

    /**
     * 清理指定时间之前的归档数据
     * 
     * @param poolId 数据池ID
     * @param beforeTime 指定时间
     * @return 清理的数据量
     */
    public int cleanArchivedDataBeforeTime(Long poolId, Date beforeTime);

    /**
     * 根据ID检查归档数据是否存在
     * 
     * @param id 归档数据ID
     * @return 存在返回true，不存在返回false
     */
    public boolean checkArchivedDataExists(Long id);

    /**
     * 归档热数据项
     * 将已完成的热数据项移动到归档表
     * 
     * @param dataPoolItem 热数据项
     * @param verificationData 校验数据
     * @param verificationStatus 校验状态
     * @return 归档结果
     */
    public boolean archiveDataPoolItem(DataPoolItem dataPoolItem, String verificationData, String verificationStatus);

    /**
     * 批量归档热数据项
     * 
     * @param dataPoolItems 热数据项列表
     * @return 归档成功的数量
     */
    public int batchArchiveDataPoolItems(List<DataPoolItem> dataPoolItems);

    /**
     * 根据打印状态归档数据
     * 自动归档所有已打印或失败的热数据项
     * 
     * @param poolId 数据池ID（可选，为null时处理所有数据池）
     * @return 归档的数据量
     */
    public int archiveByPrintStatus(Long poolId);

    /**
     * 获取归档数据的导出信息
     * 
     * @param poolId 数据池ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 导出数据列表
     */
    public List<ArchivedDataPoolItem> getExportData(Long poolId, Date startTime, Date endTime);

    /**
     * 更新校验信息
     * 
     * @param id 归档数据ID
     * @param verificationData 校验数据
     * @param verificationStatus 校验状态
     * @return 更新结果
     */
    public boolean updateVerificationInfo(Long id, String verificationData, String verificationStatus);

    /**
     * 根据设备ID批量更新校验状态
     * 
     * @param deviceId 设备ID
     * @param verificationStatus 校验状态
     * @return 更新的数据量
     */
    public int updateVerificationStatusByDeviceId(String deviceId, String verificationStatus);
}

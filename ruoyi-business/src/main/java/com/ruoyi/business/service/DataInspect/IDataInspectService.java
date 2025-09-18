package com.ruoyi.business.service.DataInspect;

import com.ruoyi.business.domain.DataInspect.DataInspect;
import com.ruoyi.common.core.page.PageQuery;
import com.ruoyi.common.core.page.PageResult;

import java.util.List;

/**
 * 产品质检记录 服务接口
 */
public interface IDataInspectService {

    /**
     * 根据ID查询
     */
    DataInspect selectById(Long id);

    /**
     * 根据数据查询
     */
    DataInspect selectByItemData(String itemData);

    /**
     * 查询列表
     */
    List<DataInspect> selectList(DataInspect query);

    /**
     * 分页查询
     */
    PageResult<DataInspect> selectPageList(DataInspect query, PageQuery pageQuery);

    /**
     * 新增
     */
    int insert(DataInspect entity);

    /**
     * 修改
     */
    int update(DataInspect entity);

    /**
     * 删除
     */
    int deleteById(Long id);
}

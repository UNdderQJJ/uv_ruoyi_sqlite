package com.ruoyi.business.mapper.DataInspect;

import com.ruoyi.business.domain.DataInspect.DataInspect;
import com.ruoyi.business.domain.DataPoolItem.DataPoolItem;

import java.util.List;

/**
 * 产品质检记录 Mapper
 */
public interface DataInspectMapper {

    DataInspect selectById(Long id);

    DataInspect selectByItemData(String itemData);

    List<DataInspect> selectList(DataInspect query);

    int insert(DataInspect entity);

    int update(DataInspect entity);

    int deleteById(Long id);

    void batchInsertDataInspect(List<DataInspect> toInsert);

    void deleteByIdList(List<Long> idList);
}



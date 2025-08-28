package com.ruoyi.business.mapper;

import com.ruoyi.business.domain.DataPoolTemplate;

import java.util.List;

/**
 * 数据池模板 Mapper
 */
public interface DataPoolTemplateMapper {

    DataPoolTemplate selectDataPoolTemplateById(Long id);

    List<DataPoolTemplate> selectDataPoolTemplateList(DataPoolTemplate query);

    List<DataPoolTemplate> selectDataPoolTemplateListByPoolId(Long poolId);

    int insertDataPoolTemplate(DataPoolTemplate template);

    int updateDataPoolTemplate(DataPoolTemplate template);

    int deleteDataPoolTemplateById(Long id);

    int deleteDataPoolTemplateByIds(Long[] ids);
}



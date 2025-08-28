package com.ruoyi.business.service;

import com.ruoyi.business.domain.DataPoolTemplate;

import java.util.List;

public interface IDataPoolTemplateService {

    DataPoolTemplate selectDataPoolTemplateById(Long id);

    List<DataPoolTemplate> selectDataPoolTemplateList(DataPoolTemplate query);

    List<DataPoolTemplate> selectDataPoolTemplateListByPoolId(Long poolId);

    int insertDataPoolTemplate(DataPoolTemplate template);

    int updateDataPoolTemplate(DataPoolTemplate template);

    int deleteDataPoolTemplateById(Long id);

    int deleteDataPoolTemplateByIds(Long[] ids);
}



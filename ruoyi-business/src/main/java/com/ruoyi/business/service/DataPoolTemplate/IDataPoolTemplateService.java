package com.ruoyi.business.service.DataPoolTemplate;

import com.ruoyi.business.domain.DataPoolTemplate.DataPoolTemplate;

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



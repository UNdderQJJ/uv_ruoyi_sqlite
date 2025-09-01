package com.ruoyi.business.service.DataPoolTemplate.impl;

import com.ruoyi.business.domain.DataPoolTemplate.DataPoolTemplate;
import com.ruoyi.business.mapper.DataPoolTemplate.DataPoolTemplateMapper;
import com.ruoyi.business.service.DataPoolTemplate.IDataPoolTemplateService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataPoolTemplateServiceImpl implements IDataPoolTemplateService {

    @Resource
    private DataPoolTemplateMapper dataPoolTemplateMapper;

    @Override
    public DataPoolTemplate selectDataPoolTemplateById(Long id) {
        return dataPoolTemplateMapper.selectDataPoolTemplateById(id);
    }

    @Override
    public List<DataPoolTemplate> selectDataPoolTemplateList(DataPoolTemplate query) {
        return dataPoolTemplateMapper.selectDataPoolTemplateList(query);
    }

    @Override
    public List<DataPoolTemplate> selectDataPoolTemplateListByPoolId(Long poolId) {
        return dataPoolTemplateMapper.selectDataPoolTemplateListByPoolId(poolId);
    }

    @Override
    public int insertDataPoolTemplate(DataPoolTemplate template) {
        return dataPoolTemplateMapper.insertDataPoolTemplate(template);
    }

    @Override
    public int updateDataPoolTemplate(DataPoolTemplate template) {
        return dataPoolTemplateMapper.updateDataPoolTemplate(template);
    }

    @Override
    public int deleteDataPoolTemplateById(Long id) {
        return dataPoolTemplateMapper.deleteDataPoolTemplateById(id);
    }

    @Override
    public int deleteDataPoolTemplateByIds(Long[] ids) {
        return dataPoolTemplateMapper.deleteDataPoolTemplateByIds(ids);
    }
}



package com.ruoyi.business.service.DataInspect.impl;

import com.github.pagehelper.Page;
import com.ruoyi.business.domain.DataInspect.DataInspect;
import com.ruoyi.business.domain.DataPoolItem.DataPoolItem;
import com.ruoyi.business.mapper.DataInspect.DataInspectMapper;
import com.ruoyi.business.service.DataInspect.IDataInspectService;
import com.ruoyi.common.core.page.PageQuery;
import com.ruoyi.common.core.page.PageResult;
import com.ruoyi.common.utils.PageQueryUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 产品质检记录服务实现
 */
@Service
public class DataInspectServiceImpl implements IDataInspectService {

    @Resource
    private DataInspectMapper dataInspectMapper;

    @Override
    public DataInspect selectById(Long id) {
        return dataInspectMapper.selectById(id);
    }

    @Override
    public DataInspect selectByItemData(String itemData) {
        return dataInspectMapper.selectByItemData(itemData);
    }

    @Override
    public List<DataInspect> selectList(DataInspect query) {
        return dataInspectMapper.selectList(query);
    }

    @Override
    public PageResult<DataInspect> selectPageList(DataInspect query, PageQuery pageQuery) {
        // 启动分页
        PageQueryUtils.startPage(pageQuery);
        try {
            List<DataInspect> list = dataInspectMapper.selectList(query);
            Page<DataInspect> page = (Page<DataInspect>) list;
            return PageResult.of(list, page.getTotal(), pageQuery);
        } finally {
            PageQueryUtils.clearPage();
        }
    }

    @Override
    public int insert(DataInspect entity) {
        return dataInspectMapper.insert(entity);
    }

    @Override
    public int update(DataInspect entity) {
        return dataInspectMapper.update(entity);
    }

    @Override
    public int deleteById(Long id) {
        return dataInspectMapper.deleteById(id);
    }

    @Override
    public void batchInsertDataInspect(List<DataInspect> toInsert) {
        dataInspectMapper.batchInsertDataInspect(toInsert);
    }

    @Override
    public void deleteByIdList(List<Long> IdList) {
        dataInspectMapper.deleteByIdList(IdList);
    }
}



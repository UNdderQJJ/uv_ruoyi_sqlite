package com.ruoyi.tcp.business.DataInspect;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.domain.DataInspect.DataInspect;
import com.ruoyi.business.service.DataInspect.IDataInspectService;
import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.core.page.PageQuery;
import com.ruoyi.common.core.page.PageResult;
import com.ruoyi.common.utils.StringUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 产品质检 TCP 处理器
 * 路径前缀: /business/dataInspect/*
 * 提供质检记录的增删改查接口，入参出参均为 JSON。
 */
@Component
public class DataInspectManagementHandler {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private IDataInspectService dataInspectService;

    /**
     * 质检接口统一入口，根据 path 分派到具体方法。
     */
    public TcpResponse handleRequest(String path, String body) {
        try {
            if (path.endsWith("/create")) {
                return create(body);
            } else if (path.endsWith("/update")) {
                return update(body);
            } else if (path.endsWith("/delete")) {
                return delete(body);
            } else if (path.endsWith("/get")) {
                return get(body);
            } else if (path.endsWith("/getByItemData")) {
                return getByItemData(body);
            } else if (path.endsWith("/list")) {
                return list(body);
            }
            return TcpResponse.error("未知的质检接口: " + path);
        } catch (Exception e) {
            return TcpResponse.error(e.getMessage());
        }
    }

    /** 新增质检记录 */
    private TcpResponse create(String body) throws Exception {
        DataInspect entity = objectMapper.readValue(StringUtils.isEmpty(body) ? "{}" : body, DataInspect.class);
        int rows = dataInspectService.insert(entity);
        Map<String, Object> res = new HashMap<>();
        res.put("rows", rows);
        res.put("id", entity.getId());
        return TcpResponse.success(res);
    }

    /** 更新质检记录（根据主键 Id） */
    private TcpResponse update(String body) throws Exception {
        DataInspect entity = objectMapper.readValue(StringUtils.isEmpty(body) ? "{}" : body, DataInspect.class);
        int rows = dataInspectService.update(entity);
        return TcpResponse.success(rows);
    }

    /** 删除质检记录（根据主键 Id） */
    @SuppressWarnings("unchecked")
    private TcpResponse delete(String body) throws Exception {
        Map<String, Object> map = objectMapper.readValue(body, Map.class);
        Long id = map.get("id") == null ? null : Long.valueOf(map.get("id").toString());
        int rows = dataInspectService.deleteById(id);
        return TcpResponse.success(rows);
    }

    /** 根据 Id 查询质检记录 */
    @SuppressWarnings("unchecked")
    private TcpResponse get(String body) throws Exception {
        if (StringUtils.isNumeric(body)) {
            DataInspect data = dataInspectService.selectById(Long.valueOf(body));
            return TcpResponse.success(data);
        }
        Map<String, Object> map = objectMapper.readValue(body, Map.class);
        Long id = map.get("id") == null ? null : Long.valueOf(map.get("id").toString());
        DataInspect data = dataInspectService.selectById(id);
        return TcpResponse.success(data);
    }

    /** 根据打印唯一数据 itemData 查询质检记录 */
    @SuppressWarnings("unchecked")
    private TcpResponse getByItemData(String body) throws Exception {
        Map<String, Object> map = objectMapper.readValue(body, Map.class);
        String itemData = map.get("itemData") == null ? null : map.get("itemData").toString();
        DataInspect data = dataInspectService.selectByItemData(itemData);
        return TcpResponse.success(data);
    }

    /** 列表查询（分页，支持多条件过滤） */
    private TcpResponse list(String body) throws Exception {
        if (StringUtils.isEmpty(body)) {
            return TcpResponse.error("请求体不能为空");
        }

        // 解析参数
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});

        // 构建查询条件
        DataInspect query = objectMapper.convertValue(params, DataInspect.class);

        // 分页参数
        PageQuery pageQuery = new PageQuery();
        if (params.containsKey("pageNum")) pageQuery.setPageNum((Integer) params.get("pageNum"));
        if (params.containsKey("pageSize")) pageQuery.setPageSize((Integer) params.get("pageSize"));
        if (params.containsKey("orderByColumn")) pageQuery.setOrderByColumn((String) params.get("orderByColumn"));
        if (params.containsKey("isAsc")) pageQuery.setIsAsc((String) params.get("isAsc"));
        if (params.containsKey("reasonable")) pageQuery.setReasonable((Boolean) params.get("reasonable"));

        PageResult<DataInspect> result = dataInspectService.selectPageList(query, pageQuery);
        return TcpResponse.success(result);
    }
}


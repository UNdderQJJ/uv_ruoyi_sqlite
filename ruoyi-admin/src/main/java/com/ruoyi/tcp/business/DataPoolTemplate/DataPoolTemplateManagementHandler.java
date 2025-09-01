package com.ruoyi.tcp.business.DataPoolTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.domain.DataPoolTemplate.DataPoolTemplate;
import com.ruoyi.business.service.DataPoolTemplate.IDataPoolTemplateService;
import com.ruoyi.common.core.TcpResponse;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataPoolTemplateManagementHandler {

    private static final Logger log = LoggerFactory.getLogger(DataPoolTemplateManagementHandler.class);

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private IDataPoolTemplateService dataPoolTemplateService;

    public TcpResponse handleDataPoolTemplateRequest(String path, String body) {
        try {
            switch (path) {
                case "/business/dataPoolTemplate/list":
                    return listTemplates(body);
                case "/business/dataPoolTemplate/listByPool":
                    return listTemplatesByPool(body);
                case "/business/dataPoolTemplate/get":
                    return getTemplate(body);
                case "/business/dataPoolTemplate/create":
                    return createTemplate(body);
                case "/business/dataPoolTemplate/update":
                    return updateTemplate(body);
                case "/business/dataPoolTemplate/delete":
                    return deleteTemplate(body);
                default:
                    log.warn("[DataPoolTemplate] 未知的模板操作路径: {}", path);
                    return TcpResponse.error("未知的模板操作: " + path);
            }
        } catch (Exception e) {
            log.error("[DataPoolTemplate] 处理模板请求时发生异常: {}", path, e);
            return TcpResponse.error("模板操作失败: " + e.getMessage());
        }
    }

    private TcpResponse listTemplates(String body) throws JsonProcessingException {
        DataPoolTemplate query = new DataPoolTemplate();
        if (body != null && !body.isEmpty()) {
            query = objectMapper.readValue(body, DataPoolTemplate.class);
        }
        List<DataPoolTemplate> list = dataPoolTemplateService.selectDataPoolTemplateList(query);
        return TcpResponse.success("查询成功", list);
    }

    private TcpResponse listTemplatesByPool(String body) throws JsonProcessingException {
        Map<String, Object> map = objectMapper.readValue(body, new TypeReference<>(){});
        Long poolId = map.get("poolId") == null ? null : Long.valueOf(map.get("poolId").toString());
        if (poolId == null) {
            return TcpResponse.error("poolId 不能为空");
        }
        List<DataPoolTemplate> list = dataPoolTemplateService.selectDataPoolTemplateListByPoolId(poolId);
        return TcpResponse.success("查询成功", list);
    }

    private TcpResponse getTemplate(String body) throws JsonProcessingException {
        Map<String, Object> map = objectMapper.readValue(body, new TypeReference<>(){});
        Long id = map.get("id") == null ? null : Long.valueOf(map.get("id").toString());
        if (id == null) {
            return TcpResponse.error("id 不能为空");
        }
        DataPoolTemplate tpl = dataPoolTemplateService.selectDataPoolTemplateById(id);
        return TcpResponse.success("查询成功", tpl);
    }

    private TcpResponse createTemplate(String body) throws JsonProcessingException {
        DataPoolTemplate tpl = objectMapper.readValue(body, DataPoolTemplate.class);
        int rows = dataPoolTemplateService.insertDataPoolTemplate(tpl);
        Map<String, Object> res = new HashMap<>();
        res.put("rows", rows);
        return TcpResponse.success("创建成功", res);
    }

    private TcpResponse updateTemplate(String body) throws JsonProcessingException {
        DataPoolTemplate tpl = objectMapper.readValue(body, DataPoolTemplate.class);
        int rows = dataPoolTemplateService.updateDataPoolTemplate(tpl);
        Map<String, Object> res = new HashMap<>();
        res.put("rows", rows);
        return TcpResponse.success("更新成功", res);
    }

    private TcpResponse deleteTemplate(String body) throws JsonProcessingException {
        Map<String, Object> map = objectMapper.readValue(body, new TypeReference<>(){});
        Object idsObj = map.get("ids");
        if (idsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> idsList = (List<Object>) idsObj;
            Long[] ids = idsList.stream().map(o -> Long.valueOf(o.toString())).toArray(Long[]::new);
            int rows = dataPoolTemplateService.deleteDataPoolTemplateByIds(ids);
            Map<String, Object> res = new HashMap<>();
            res.put("rows", rows);
            return TcpResponse.success("批量删除成功", res);
        } else {
            Long id = map.get("id") == null ? null : Long.valueOf(map.get("id").toString());
            if (id == null) {
                return TcpResponse.error("id 或 ids 不能为空");
            }
            int rows = dataPoolTemplateService.deleteDataPoolTemplateById(id);
            Map<String, Object> res = new HashMap<>();
            res.put("rows", rows);
            return TcpResponse.success("删除成功", res);
        }
    }
}



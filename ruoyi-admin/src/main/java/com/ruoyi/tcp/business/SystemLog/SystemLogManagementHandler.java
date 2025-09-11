package com.ruoyi.tcp.business.SystemLog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.domain.SystemLog.SystemLog;
import com.ruoyi.business.service.SystemLog.ISystemLogService;
import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.utils.StringUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 系统日志 TCP 处理器
 * 路径前缀: /business/systemLog/*
 * 提供日志增删查与计数接口，入参出参均为 JSON。
 */
@Component
public class SystemLogManagementHandler {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ISystemLogService systemLogService;

    /**
     * 系统日志统一入口，根据 path 分派到具体方法。
     */
    public TcpResponse handleSystemLogRequest(String path, String body) {
        try {
            if (path.endsWith("/create")) {
                return create(body);
            } else if (path.endsWith("/batchCreate")) {
                return batchCreate(body);
            } else if (path.endsWith("/delete")) {
                return delete(body);
            } else if (path.endsWith("/get")) {
                return get(body);
            } else if (path.endsWith("/list")) {
                return list(body);
            } else if (path.endsWith("/count")) {
                return count(body);
            }
            return TcpResponse.error("未知的系统日志接口: " + path);
        } catch (Exception e) {
            return TcpResponse.error(e.getMessage());
        }
    }

    /** 新增单条日志 */
    private TcpResponse create(String body) throws Exception {
        SystemLog log = objectMapper.readValue(StringUtils.isEmpty(body) ? "{}" : body, SystemLog.class);
        int rows = systemLogService.insert(log);
        return rows > 0 ? TcpResponse.success(log) : TcpResponse.error("新增日志失败");
    }

    /** 批量新增日志 */
    private TcpResponse batchCreate(String body) throws Exception {
        List<SystemLog> list = objectMapper.readValue(StringUtils.isEmpty(body) ? "[]" : body, objectMapper.getTypeFactory().constructCollectionType(List.class, SystemLog.class));
        int rows = systemLogService.batchInsert(list);
        return TcpResponse.success(rows);
    }

    /** 按ID数组批量删除日志 */
    private TcpResponse delete(String body) throws Exception {
        Long[] ids = objectMapper.readValue(body, Long[].class);
        int rows = systemLogService.deleteByIds(ids);
        return TcpResponse.success(rows);
    }

    /** 根据ID查询日志 */
    private TcpResponse get(String body) throws Exception {
        Long id;
        if (StringUtils.isNumeric(body)) {
            id = Long.valueOf(body);
        } else {
            SystemLog req = objectMapper.readValue(body, SystemLog.class);
            id = req.getId();
        }
        SystemLog data = systemLogService.selectById(id);
        return TcpResponse.success(data);
    }

    /** 列表查询日志（支持多条件过滤） */
    private TcpResponse list(String body) throws Exception {
        SystemLog query = StringUtils.isEmpty(body) ? new SystemLog() : objectMapper.readValue(body, SystemLog.class);
        List<SystemLog> list = systemLogService.selectList(query);
        return TcpResponse.success(list);
    }

    /** 统计日志数量（支持多条件过滤） */
    private TcpResponse count(String body) throws Exception {
        SystemLog query = StringUtils.isEmpty(body) ? new SystemLog() : objectMapper.readValue(body, SystemLog.class);
        Integer cnt = systemLogService.count(query);
        return TcpResponse.success(cnt);
    }
}



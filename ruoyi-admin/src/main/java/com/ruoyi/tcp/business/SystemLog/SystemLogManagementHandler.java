package com.ruoyi.tcp.business.SystemLog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.domain.SystemLog.SystemLog;
import com.ruoyi.business.service.SystemLog.ISystemLogService;
import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.core.page.CursorPageQuery;
import com.ruoyi.common.core.page.CursorPageResult;
import com.ruoyi.common.core.page.PageQuery;
import com.ruoyi.common.core.page.PageResult;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

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

    @Value("${app.logging.path:${user.home}/UVControlSystem/logs}")
    private String configuredLogDir;

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
            } else if (path.endsWith("/pageList")) {
                return pageList(body);
            } else if (path.endsWith("/cursorPageList")) {
                return cursorPageList(body);
            } else if (path.endsWith("/logDir")) {
                return logDir();
            }
            return TcpResponse.error("未知的系统日志接口: " + path);
        } catch (Exception e) {
            return TcpResponse.error(e.getMessage());
        }
    }

    /**
     * 返回当前操作系统信息与日志存储目录
     * 路径: /business/systemLog/logDir
     */
    private TcpResponse logDir() {
        String osName = System.getProperty("os.name", "");// 操作系统名称
        String osLower = osName.toLowerCase();// 操作系统名称
        String osType;// 操作系统类型
        if (osLower.contains("win")) {
            osType = "Windows";
        } else if (osLower.contains("mac")) {
            osType = "macOS";
        } else if (osLower.contains("nix") || osLower.contains("nux") || osLower.contains("aix")) {
            osType = "Linux";
        } else {
            osType = "Other";
        }

        Map<String, Object> data = new HashMap<>();
        data.put("osName", osName);
        data.put("osType", osType);
        data.put("logDir", configuredLogDir);// 日志存储目录
        return TcpResponse.success(data);
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

    /** 分页查询日志（支持多条件过滤和排序） */
    private TcpResponse pageList(String body) throws Exception {
        if (StringUtils.isEmpty(body)) {
            return TcpResponse.error("请求体不能为空");
        }

        // 解析请求参数
        @SuppressWarnings("unchecked")
        Map<String, Object> params = objectMapper.readValue(body, Map.class);

        // 构建查询条件
        SystemLog query = objectMapper.convertValue(params, SystemLog.class);

        // 构建分页参数
        PageQuery pageQuery = new PageQuery();
        if (params.containsKey("pageNum")) {
            pageQuery.setPageNum((Integer) params.get("pageNum"));
        }
        if (params.containsKey("pageSize")) {
            pageQuery.setPageSize((Integer) params.get("pageSize"));
        }
        if (params.containsKey("orderByColumn")) {
            pageQuery.setOrderByColumn((String) params.get("orderByColumn"));
        }
        if (params.containsKey("isAsc")) {
            pageQuery.setIsAsc((String) params.get("isAsc"));
        }
        if (params.containsKey("reasonable")) {
            pageQuery.setReasonable((Boolean) params.get("reasonable"));
        }

        // 执行分页查询
        PageResult<SystemLog> result = systemLogService.selectPageList(query, pageQuery);
        return TcpResponse.success(result);
    }

    /** 游标分页查询日志（高性能，适合大数据量） */
    private TcpResponse cursorPageList(String body) throws Exception {
        if (StringUtils.isEmpty(body)) {
            return TcpResponse.error("请求体不能为空");
        }

        // 解析请求参数
        @SuppressWarnings("unchecked")
        Map<String, Object> params = objectMapper.readValue(body, Map.class);
        
        // 构建查询条件
        SystemLog query = objectMapper.convertValue(params, SystemLog.class);
        
        // 构建游标分页参数
        CursorPageQuery cursorQuery = new CursorPageQuery();
        if (params.containsKey("pageSize")) {
            cursorQuery.setPageSize((Integer) params.get("pageSize"));
        }
        if (params.containsKey("lastId")) {
            cursorQuery.setLastId(((Number) params.get("lastId")).longValue());
        }
        if (params.containsKey("lastLogTime")) {
            cursorQuery.setLastLogTime((String) params.get("lastLogTime"));
        }
        if (params.containsKey("orderByColumn")) {
            cursorQuery.setOrderByColumn((String) params.get("orderByColumn"));
        }
        if (params.containsKey("isAsc")) {
            cursorQuery.setIsAsc((String) params.get("isAsc"));
        }

        // 执行游标分页查询
        CursorPageResult<SystemLog> result = systemLogService.selectCursorPageList(query, cursorQuery);
        return TcpResponse.success(result);
    }
}



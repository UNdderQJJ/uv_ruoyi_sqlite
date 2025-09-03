package com.ruoyi.tcp.business.Task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.domain.TaskInfo.TaskInfo;
import com.ruoyi.business.enums.TaskStatus;
import com.ruoyi.business.service.TaskInfo.ITaskInfoService;
import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.utils.StringUtils;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 任务中心 TaskInfo TCP 处理器
 * 负责处理 /business/taskInfo/* 路径的请求
 */
@Component
public class TaskInfoManagementHandler {

    private static final Logger log = LoggerFactory.getLogger(TaskInfoManagementHandler.class);

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ITaskInfoService taskInfoService;

    /**
     * 任务中心统一入口
     * 根据 path 分派到具体方法
     */
    public TcpResponse handleTaskInfoRequest(String path, String body) {
        try {
            if (path.endsWith("/create")) {
                return create(body);
            } else if (path.endsWith("/update")) {
                return update(body);
            } else if (path.endsWith("/delete")) {
                return delete(body);
            } else if (path.endsWith("/get")) {
                return get(body);
            } else if (path.endsWith("/list")) {
                return list(body);
            } else if (path.endsWith("/count")) {
                return count(body);
            } else if (path.endsWith("/start")) {
                return start(body);
            } else if (path.endsWith("/pause")) {
                return pause(body);
            } else if (path.endsWith("/resume")) {
                return resume(body);
            } else if (path.endsWith("/complete")) {
                return complete(body);
            } else if (path.endsWith("/fail")) {
                return fail(body);
            }
            return TcpResponse.error("未知的任务中心接口: " + path);
        } catch (Exception e) {
            log.error("处理任务中心请求发生异常", e);
            return TcpResponse.error(e.getMessage());
        }
    }

    /** 新增任务 */
    private TcpResponse create(String body) throws Exception {
        TaskInfo taskInfo = objectMapper.readValue(StringUtils.isEmpty(body) ? "{}" : body, TaskInfo.class);
        int rows = taskInfoService.insertTaskInfo(taskInfo);
        return rows > 0 ? TcpResponse.success(taskInfo) : TcpResponse.error("新增任务失败");
    }

    /** 更新任务 */
    private TcpResponse update(String body) throws Exception {
        TaskInfo taskInfo = objectMapper.readValue(body, TaskInfo.class);
        int rows = taskInfoService.updateTaskInfo(taskInfo);
        return rows > 0 ? TcpResponse.success(taskInfo) : TcpResponse.error("更新任务失败");
    }

    /** 批量删除任务（ids） */
    private TcpResponse delete(String body) throws Exception {
        // body: {"ids":[1,2,3]}
        Long[] ids = objectMapper.readValue(body, Long[].class);
        int rows = taskInfoService.deleteTaskInfoByIds(ids);
        return TcpResponse.success(rows);
    }

    /** 根据ID获取任务（body可为{"id":1}或直接传long） */
    private TcpResponse get(String body) throws Exception {
        Long id;
        if (StringUtils.isNumeric(body)) {
            id = Long.valueOf(body);
        } else {
            TaskInfo req = objectMapper.readValue(body, TaskInfo.class);
            id = req.getId();
        }
        TaskInfo data = taskInfoService.selectTaskInfoById(id);
        return TcpResponse.success(data);
    }

    /** 列表查询（支持 name/status/poolId 过滤） */
    private TcpResponse list(String body) throws Exception {
        TaskInfo query = StringUtils.isEmpty(body) ? new TaskInfo() : objectMapper.readValue(body, TaskInfo.class);
        List<TaskInfo> list = taskInfoService.selectTaskInfoList(query);
        return TcpResponse.success(list);
    }

    /** 统计数量（支持过滤） */
    private TcpResponse count(String body) throws Exception {
        TaskInfo query = StringUtils.isEmpty(body) ? new TaskInfo() : objectMapper.readValue(body, TaskInfo.class);
        Integer cnt = taskInfoService.countTaskInfo(query);
        return TcpResponse.success(cnt);
    }

    /** 启动任务 */
    private TcpResponse start(String body) throws Exception {
        Long id = parseId(body);
        int rows = taskInfoService.startTask(id);
        return TcpResponse.success(rows);
    }

    /** 暂停任务 */
    private TcpResponse pause(String body) throws Exception {
        Long id = parseId(body);
        int rows = taskInfoService.pauseTask(id);
        return TcpResponse.success(rows);
    }

    /** 恢复任务 */
    private TcpResponse resume(String body) throws Exception {
        Long id = parseId(body);
        int rows = taskInfoService.resumeTask(id);
        return TcpResponse.success(rows);
    }

    /** 完成任务 */
    private TcpResponse complete(String body) throws Exception {
        Long id = parseId(body);
        int rows = taskInfoService.completeTask(id);
        return TcpResponse.success(rows);
    }

    /** 失败任务 */
    private TcpResponse fail(String body) throws Exception {
        Long id = parseId(body);
        int rows = taskInfoService.failTask(id);
        return TcpResponse.success(rows);
    }

    /** 解析ID，支持{"id":1}或直接传long字符串 */
    private Long parseId(String body) throws Exception {
        if (StringUtils.isEmpty(body)) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (StringUtils.isNumeric(body)) {
            return Long.valueOf(body);
        }
        TaskInfo req = objectMapper.readValue(body, TaskInfo.class);
        return req.getId();
    }
}



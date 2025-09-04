package com.ruoyi.tcp.business.Task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.domain.TaskInfo.TaskDeviceLink;
import com.ruoyi.business.domain.TaskInfo.TaskInfo;
import com.ruoyi.business.enums.TaskDeviceStatus;
import com.ruoyi.business.service.DeviceInfo.IDeviceInfoService;
import com.ruoyi.business.service.TaskInfo.ITaskDeviceLinkService;
import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.utils.StringUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 任务设备关联 TaskDeviceLink TCP 处理器
 * 负责处理 /business/taskInfo/link* 路径的请求
 */
@Component
public class TaskDeviceLinkManagementHandler {


    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ITaskDeviceLinkService taskDeviceLinkService;


     /**
     * 任务设备关联表 CRUD 处理
     */
    public TcpResponse handleTaskDeviceLinkRequest(String path, String body) throws Exception {
        if (path.endsWith("/create")) {
            return createLink(body);
        } else if (path.endsWith("/update")) {
            return updateLink(body);
        } else if (path.endsWith("/delete")) {
            return deleteLink(body);
        } else if (path.endsWith("/get")) {
            return getLink(body);
        } else if (path.endsWith("/list")) {
            return listLinks(body);
        } else if (path.endsWith("/listByTask")) {
            return listLinksByTask(body);
        } else if (path.endsWith("/listByDevice")) {
            return listLinksByDevice(body);
        }
        return TcpResponse.error("未知的任务设备关联接口: " + path);
    }

    /** 创建任务设备关联 */
    private TcpResponse createLink(String body) throws Exception {
        TaskDeviceLink link = objectMapper.readValue(body, TaskDeviceLink.class);
        if (link.getStatus() == null) {
            link.setStatus(TaskDeviceStatus.WAITING.getCode());
        }
        if (link.getDelFlag() == null) {
            link.setDelFlag(0);
        }
        if (link.getCompletedQuantity() == null) {
            link.setCompletedQuantity(0);
        }

        List<TaskDeviceLink> links = new java.util.ArrayList<>();
        links.add(link);
        int rows = taskDeviceLinkService.batchCreateLinks(links);
        return rows > 0 ? TcpResponse.success(link) : TcpResponse.error("创建任务设备关联失败");
    }

    /** 更新任务设备关联 */
    private TcpResponse updateLink(String body) throws Exception {
        TaskDeviceLink link = objectMapper.readValue(body, TaskDeviceLink.class);
        int rows = taskDeviceLinkService.updateLink(link);
        return rows > 0 ? TcpResponse.success(link) : TcpResponse.error("更新任务设备关联失败");
    }

    /** 删除任务设备关联（按任务ID） */
    private TcpResponse deleteLink(String body) throws Exception {
        Long taskId = parseId(body);
        int rows = taskDeviceLinkService.deleteByTaskId(taskId);
        return TcpResponse.success(rows);
    }

    /** 获取任务设备关联详情 */
    private TcpResponse getLink(String body) throws Exception {
        Long id = parseId(body);
        TaskDeviceLink query = new TaskDeviceLink();
        query.setId(id);
        List<TaskDeviceLink> list = taskDeviceLinkService.list(query);
        TaskDeviceLink data = list.isEmpty() ? null : list.get(0);
        return TcpResponse.success(data);
    }

    /** 查询任务设备关联列表 */
    private TcpResponse listLinks(String body) throws Exception {
        TaskDeviceLink query = StringUtils.isEmpty(body) ? new TaskDeviceLink() : objectMapper.readValue(body, TaskDeviceLink.class);
        List<TaskDeviceLink> list = taskDeviceLinkService.list(query);
        return TcpResponse.success(list);
    }

    /** 按任务ID查询关联列表 */
    private TcpResponse listLinksByTask(String body) throws Exception {
        Long taskId = parseId(body);
        TaskDeviceLink query = new TaskDeviceLink();
        query.setTaskId(taskId);
        List<TaskDeviceLink> list = taskDeviceLinkService.list(query);
        return TcpResponse.success(list);
    }

    /** 按设备ID查询关联列表 */
    private TcpResponse listLinksByDevice(String body) throws Exception {
        Long deviceId = parseId(body);
        TaskDeviceLink query = new TaskDeviceLink();
        query.setDeviceId(deviceId);
        List<TaskDeviceLink> list = taskDeviceLinkService.list(query);
        return TcpResponse.success(list);
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

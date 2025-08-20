package com.ruoyi.tcp.business;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.domain.DataPool;
import com.ruoyi.business.service.IDataPoolService;

import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据池管理TCP处理器
 * 专门处理数据池相关的TCP请求
 */
@Component
public class DataPoolManagementHandler
{
    private static final Logger log = LoggerFactory.getLogger(DataPoolManagementHandler.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IDataPoolService dataPoolService;



    /**
     * 处理数据池相关的TCP请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @return TCP响应
     */
    public TcpResponse handleDataPoolRequest(String path, String body) {
        try {
            switch (path) {
                case "/business/dataPool/list":
                    return listDataPools(body);
                case "/business/dataPool/get":
                    return getDataPool(body);
                case "/business/dataPool/create":
                    return createDataPool(body);
                case "/business/dataPool/update":
                    return updateDataPool(body);
                case "/business/dataPool/delete":
                    return deleteDataPool(body);
                case "/business/dataPool/start":
                    return startDataPool(body);
                case "/business/dataPool/stop":
                    return stopDataPool(body);
                case "/business/dataPool/updateStatus":
                    return updateDataPoolStatus(body);
                case "/business/dataPool/updateCount":
                    return updateDataPoolCount(body);
                default:
                    log.warn("[DataPoolManagement] 未知的数据池操作路径: {}", path);
                    return TcpResponse.error("未知的数据池操作: " + path);
            }
        } catch (Exception e) {
            log.error("[DataPoolManagement] 处理数据池请求时发生异常: {}", path, e);
            return TcpResponse.error("数据池操作失败: " + e.getMessage());
        }
    }

    /**
     * 查询数据池列表
     */
    private TcpResponse listDataPools(String body) throws JsonProcessingException {
        DataPool queryParam = new DataPool();
        
        if (StringUtils.isNotEmpty(body)) {
            queryParam = objectMapper.readValue(body, DataPool.class);
        }

        List<DataPool> dataPools = dataPoolService.selectDataPoolList(queryParam);
        
        var result = new HashMap<String, Object>();
        result.put("dataPools", dataPools);
        result.put("total", dataPools.size());
        
        return TcpResponse.success("查询数据池列表成功", result);
    }

    /**
     * 获取数据池详情
     */
    private TcpResponse getDataPool(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});
        Long id = Long.valueOf(params.get("id").toString());
        
        DataPool dataPool = dataPoolService.selectDataPoolById(id);
        
        if (dataPool == null) {
            return TcpResponse.error("数据池不存在");
        }

        var result = new HashMap<String, Object>();
        result.put("dataPool", dataPool);
        
        return TcpResponse.success("获取数据池详情成功", result);
    }

    /**
     * 创建数据池
     */
    private TcpResponse createDataPool(String body) throws JsonProcessingException {
        DataPool dataPool = objectMapper.readValue(body, DataPool.class);
        
        // 验证必填参数
        if (StringUtils.isEmpty(dataPool.getPoolName()) || StringUtils.isEmpty(dataPool.getSourceType())) {
            return TcpResponse.error("缺少必要参数: poolName, sourceType");
        }

        int result = dataPoolService.insertDataPool(dataPool);
        
        if (result > 0) {
            var responseData = new HashMap<String, Object>();
            responseData.put("id", dataPool.getId());
            return TcpResponse.success("创建数据池成功", responseData);
        } else {
            return TcpResponse.error("创建数据池失败");
        }
    }

    /**
     * 更新数据池
     */
    private TcpResponse updateDataPool(String body) throws JsonProcessingException {
        DataPool dataPool = objectMapper.readValue(body, DataPool.class);
        
        if (dataPool.getId() == null) {
            return TcpResponse.error("缺少数据池ID参数");
        }

        int result = dataPoolService.updateDataPool(dataPool);
        
        if (result > 0) {
            return TcpResponse.success("更新数据池成功");
        } else {
            return TcpResponse.error("更新数据池失败");
        }
    }

    /**
     * 删除数据池
     */
    private TcpResponse deleteDataPool(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});
        Long id = Long.valueOf(params.get("id").toString());
        
        int result = dataPoolService.deleteDataPoolById(id);
        
        if (result > 0) {
            return TcpResponse.success("删除数据池成功");
        } else {
            return TcpResponse.error("删除数据池失败");
        }
    }

    /**
     * 启动数据池
     */
    private TcpResponse startDataPool(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});
        Long id = Long.valueOf(params.get("id").toString());
        
        int result = dataPoolService.startDataPool(id);
        
        if (result > 0) {
            return TcpResponse.success("启动数据池成功");
        } else {
            return TcpResponse.error("启动数据池失败");
        }
    }

    /**
     * 停止数据池
     */
    private TcpResponse stopDataPool(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});
        Long id = Long.valueOf(params.get("id").toString());
        
        int result = dataPoolService.stopDataPool(id);
        
        if (result > 0) {
            return TcpResponse.success("停止数据池成功");
        } else {
            return TcpResponse.error("停止数据池失败");
        }
    }

    /**
     * 更新数据池状态
     */
    private TcpResponse updateDataPoolStatus(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});
        Long id = Long.valueOf(params.get("id").toString());
        String status = (String) params.get("status");
        
        int result = dataPoolService.updateDataPoolStatus(id, status);
        
        if (result > 0) {
            return TcpResponse.success("更新数据池状态成功");
        } else {
            return TcpResponse.error("更新数据池状态失败");
        }
    }

    /**
     * 更新数据池计数
     */
    private TcpResponse updateDataPoolCount(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});
        Long id = Long.valueOf(params.get("id").toString());
        Long totalCount = params.containsKey("totalCount") ? Long.valueOf(params.get("totalCount").toString()) : null;
        Long pendingCount = params.containsKey("pendingCount") ? Long.valueOf(params.get("pendingCount").toString()) : null;
        
        int result = dataPoolService.updateDataPoolCount(id, totalCount, pendingCount);
        
        if (result > 0) {
            return TcpResponse.success("更新数据池计数成功");
        } else {
            return TcpResponse.error("更新数据池计数失败");
        }
    }
}

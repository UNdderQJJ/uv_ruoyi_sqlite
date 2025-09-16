package com.ruoyi.common.core.page;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 分页查询请求参数
 * 统一的分页查询组件，供其他查询调用
 * 
 * @author ruoyi
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageQuery {

    /** 当前页码，从1开始 */
    private Integer pageNum = 1;

    /** 每页显示记录数 */
    private Integer pageSize = 10;

    /** 排序列 */
    private String orderByColumn;

    /** 排序的方向 "desc" 或者 "asc" */
    private String isAsc = "asc";

    /** 分页参数合理化 */
    private Boolean reasonable = true;

    /**
     * 获取排序字符串
     * @return 排序字符串，如 "create_time desc"
     */
    public String getOrderBy() {
        if (orderByColumn == null || orderByColumn.trim().isEmpty()) {
            return "";
        }
        return orderByColumn + " " + isAsc;
    }

    /**
     * 设置排序字段（自动转换为下划线格式）
     * @param orderByColumn 排序字段
     */
    public void setOrderByColumn(String orderByColumn) {
        if (orderByColumn != null && !orderByColumn.trim().isEmpty()) {
            // 转换为下划线格式
            this.orderByColumn = com.ruoyi.common.utils.StringUtils.toUnderScoreCase(orderByColumn);
        }
    }

    /**
     * 设置排序方向（兼容前端排序类型）
     * @param isAsc 排序方向
     */
    public void setIsAsc(String isAsc) {
        if (isAsc != null && !isAsc.trim().isEmpty()) {
            // 兼容前端排序类型
            if ("ascending".equals(isAsc)) {
                this.isAsc = "asc";
            } else if ("descending".equals(isAsc)) {
                this.isAsc = "desc";
            } else {
                this.isAsc = isAsc;
            }
        }
    }

    /**
     * 验证分页参数
     */
    public void validate() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        // 限制最大页面大小，防止性能问题
        if (pageSize > 1000) {
            pageSize = 1000;
        }
//        // 限制最大页码，防止深度分页性能问题
//        if (pageNum > 10000) {
//            pageNum = 10000;
//        }
        if (isAsc == null || (!"asc".equals(isAsc) && !"desc".equals(isAsc))) {
            isAsc = "asc";
        }
    }
}

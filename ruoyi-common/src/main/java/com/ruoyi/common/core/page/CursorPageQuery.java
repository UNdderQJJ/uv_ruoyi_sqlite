package com.ruoyi.common.core.page;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 游标分页查询参数
 * 用于大数据量的高效分页查询，避免深度分页性能问题
 * 
 * @author ruoyi
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CursorPageQuery {

    /** 每页显示记录数 */
    private Integer pageSize = 10;

    /** 上一页最后一条记录的ID（用于游标分页） */
    private Long lastId;

    /** 上一页最后一条记录的时间（用于游标分页） */
    private String lastLogTime;

    /** 排序列 */
    private String orderByColumn = "log_time";

    /** 排序的方向 "desc" 或者 "asc" */
    private String isAsc = "desc";

    /**
     * 获取排序字符串
     * @return 排序字符串，如 "log_time desc"
     */
    public String getOrderBy() {
        if (orderByColumn == null || orderByColumn.trim().isEmpty()) {
            return "log_time desc";
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
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        // 限制最大页面大小，防止性能问题
        if (pageSize > 100) {
            pageSize = 100;
        }
        if (isAsc == null || (!"asc".equals(isAsc) && !"desc".equals(isAsc))) {
            isAsc = "desc";
        }
    }

    /**
     * 判断是否为第一页
     * @return true表示第一页
     */
    public boolean isFirstPage() {
        return lastId == null && (lastLogTime == null || lastLogTime.trim().isEmpty());
    }
}

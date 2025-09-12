package com.ruoyi.common.core.page;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 游标分页查询结果
 * 用于大数据量的高效分页查询结果
 * 
 * @author ruoyi
 */
@Data
public class CursorPageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 每页显示记录数 */
    private int pageSize;

    /** 是否有下一页 */
    private boolean hasNextPage;

    /** 下一页的游标ID */
    private Long nextCursorId;

    /** 下一页的游标时间 */
    private String nextCursorTime;

    /** 列表数据 */
    private List<T> list;

    /**
     * 默认构造函数
     */
    public CursorPageResult() {
    }

    /**
     * 构造函数
     * @param list 列表数据
     * @param pageSize 每页大小
     * @param hasNextPage 是否有下一页
     */
    public CursorPageResult(List<T> list, int pageSize, boolean hasNextPage) {
        this.list = list;
        this.pageSize = pageSize;
        this.hasNextPage = hasNextPage;
        
        // 设置下一页游标
        if (hasNextPage && !list.isEmpty()) {
            T lastItem = list.get(list.size() - 1);
            // 这里需要根据实际的数据结构来设置游标
            // 假设T有getId()和getLogTime()方法
            try {
                this.nextCursorId = (Long) lastItem.getClass().getMethod("getId").invoke(lastItem);
                this.nextCursorTime = (String) lastItem.getClass().getMethod("getLogTime").invoke(lastItem);
            } catch (Exception e) {
                // 如果反射失败，不设置游标
                this.nextCursorId = null;
                this.nextCursorTime = null;
            }
        }
    }

    /**
     * 创建游标分页结果
     * @param list 列表数据
     * @param pageSize 每页大小
     * @return 游标分页结果
     */
    public static <T> CursorPageResult<T> of(List<T> list, int pageSize) {
        boolean hasNextPage = list.size() == pageSize;
        return new CursorPageResult<>(list, pageSize, hasNextPage);
    }

    /**
     * 创建空的结果
     * @param pageSize 每页大小
     * @return 空的游标分页结果
     */
    public static <T> CursorPageResult<T> empty(int pageSize) {
        return new CursorPageResult<>(List.of(), pageSize, false);
    }
}

package com.ruoyi.common.core.page;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页查询结果
 * 统一的分页查询结果组件，供其他查询调用
 * 
 * @author ruoyi
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private int pageNum;

    /** 每页显示记录数 */
    private int pageSize;

    /** 总页数 */
    private int pages;

    /** 列表数据 */
    private List<T> list;

    /** 是否有下一页 */
    private boolean hasNextPage;

    /** 是否有上一页 */
    private boolean hasPreviousPage;

    /**
     * 默认构造函数
     */
    public PageResult() {
    }

    /**
     * 构造函数
     * @param list 列表数据
     * @param total 总记录数
     * @param pageNum 当前页码
     * @param pageSize 每页显示记录数
     */
    public PageResult(List<T> list, long total, int pageNum, int pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = (int) Math.ceil((double) total / pageSize);
        this.hasNextPage = pageNum < pages;
        this.hasPreviousPage = pageNum > 1;
    }

    /**
     * 创建分页结果
     * @param list 列表数据
     * @param total 总记录数
     * @param pageQuery 分页查询参数
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> list, long total, PageQuery pageQuery) {
        return new PageResult<>(list, total, pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    /**
     * 创建空的分页结果
     * @param pageQuery 分页查询参数
     * @return 空的分页结果
     */
    public static <T> PageResult<T> empty(PageQuery pageQuery) {
        return new PageResult<>(List.of(), 0, pageQuery.getPageNum(), pageQuery.getPageSize());
    }
}

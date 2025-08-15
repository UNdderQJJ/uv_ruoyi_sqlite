package com.ruoyi.common.core.page;

import com.ruoyi.common.utils.StringUtils;

/**
 * 表格数据处理
 * 
 * @author ruoyi
 */
public class TableSupport
{
    /** 当前记录起始索引 */
    public static final String PAGE_NUM = "pageNum";

    /** 每页显示记录数 */
    public static final String PAGE_SIZE = "pageSize";

    /** 排序列 */
    public static final String ORDER_BY_COLUMN = "orderByColumn";

    /** 排序的方向 "desc" 或者 "asc". */
    public static final String IS_ASC = "isAsc";

    /** 分页参数合理化 */
    public static final String REASONABLE = "reasonable";

    /**
     * 当前记录起始索引
     */
    public static Integer getPageNum()
    {
        return 1;
    }

    /**
     * 每页显示记录数
     */
    public static Integer getPageSize()
    {
        return 10;
    }

    /**
     * 排序列
     */
    public static String getOrderByColumn()
    {
        return "";
    }

    /**
     * 排序的方向 "desc" 或者 "asc".
     */
    public static String getIsAsc()
    {
        return "asc";
    }

    /**
     * 分页参数合理化
     */
    public static Boolean getReasonable()
    {
        return true;
    }

    /**
     * 构建分页请求
     */
    public static PageDomain buildPageRequest()
    {
        PageDomain pageDomain = new PageDomain();
        pageDomain.setPageNum(getPageNum());
        pageDomain.setPageSize(getPageSize());
        pageDomain.setOrderByColumn(getOrderByColumn());
        pageDomain.setIsAsc(getIsAsc());
        pageDomain.setReasonable(getReasonable());
        return pageDomain;
    }
}

package com.ruoyi.common.utils;

import com.github.pagehelper.PageHelper;
import com.ruoyi.common.core.page.PageQuery;
import com.ruoyi.common.utils.sql.SqlUtil;

/**
 * 分页查询工具类
 * 统一的分页查询工具，供其他查询调用
 * 
 * @author ruoyi
 */
public class PageQueryUtils {

    /**
     * 启动分页查询
     * @param pageQuery 分页查询参数
     */
    public static void startPage(PageQuery pageQuery) {
        if (pageQuery == null) {
            return;
        }
        
        // 验证分页参数
        pageQuery.validate();
        
        // 获取排序字符串
        String orderBy = pageQuery.getOrderBy();
        if (StringUtils.isNotEmpty(orderBy)) {
            // 转义排序字段，防止SQL注入
            orderBy = SqlUtil.escapeOrderBySql(orderBy);
        }
        
        // 启动分页
        PageHelper.startPage(pageQuery.getPageNum(), pageQuery.getPageSize(), orderBy)
                .setReasonable(pageQuery.getReasonable());
    }

    /**
     * 启动分页查询（无排序）
     * @param pageNum 页码
     * @param pageSize 每页大小
     */
    public static void startPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize).setReasonable(true);
    }

    /**
     * 启动分页查询（带排序）
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param orderBy 排序字段
     */
    public static void startPage(int pageNum, int pageSize, String orderBy) {
        if (StringUtils.isNotEmpty(orderBy)) {
            orderBy = SqlUtil.escapeOrderBySql(orderBy);
        }
        PageHelper.startPage(pageNum, pageSize, orderBy).setReasonable(true);
    }

    /**
     * 清理分页的线程变量
     */
    public static void clearPage() {
        PageHelper.clearPage();
    }
}

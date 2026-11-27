package com.study.platform.common;

/**
 * 列表分页。缺省 pageNum=1、pageSize=20，并限制过大的翻页参数。
 */
public final class PageQuery {

    public static final long DEFAULT_PAGE_NUM = 1L;
    public static final long DEFAULT_PAGE_SIZE = 20L;
    public static final long MAX_PAGE_SIZE = 100L;
    public static final long MAX_PAGE_NUM = 10000L;

    private PageQuery() {
    }

    public static long normalizePageNum(long pageNum) {
        if (pageNum < 1) {
            return DEFAULT_PAGE_NUM;
        }
        return Math.min(pageNum, MAX_PAGE_NUM);
    }

    public static long normalizePageSize(long pageSize) {
        if (pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}

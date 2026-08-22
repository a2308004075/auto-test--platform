/**
 * @author HXN
 * @date 2026-08-20 10:05
 * @description 分页响应封装
 */
package com.platform.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页响应格式
 *
 * <p>JSON 输出格式：{@code { "items": [], "total": N, "page": 1, "page_size": 20 }}
 *
 * @param <T> 列表元素类型
 */
@Data
public class PageResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页数据列表
     */
    private List<T> items;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码（从 1 开始）
     */
    private Long page;

    /**
     * 每页大小
     */
    @JsonProperty("page_size")
    private Long pageSize;

    public PageResponse() {
    }

    public PageResponse(List<T> items, Long total, Long page, Long pageSize) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    /**
     * 从列表和分页参数构造分页响应
     *
     * @param items    当前页数据
     * @param total    总记录数
     * @param page     当前页码
     * @param pageSize 每页大小
     */
    public static <T> PageResponse<T> of(List<T> items, long total, int page, int pageSize) {
        return new PageResponse<>(items, (long) total, (long) page, (long) pageSize);
    }

    /**
     * 构造空分页响应
     *
     * @param page     当前页码
     * @param pageSize 每页大小
     * @param <T>      列表元素类型
     * @return 空分页响应
     */
    public static <T> PageResponse<T> empty(Long page, Long pageSize) {
        return new PageResponse<>(Collections.emptyList(), 0L, page, pageSize);
    }
}

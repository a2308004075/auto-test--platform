/**
 * @author HXN
 * @date 2026-08-30 14:00
 * @description 界面元素文件树节点 DTO
 */
package com.platform.uielement.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 界面元素文件树节点
 *
 * <p>三级结构：仓库（REPO）→ 目录（DIR）→ 文件（FILE），文件节点携带元素数。
 */
@Data
public class UiElementFileNode {

    /**
     * 节点类型：REPO-仓库，DIR-目录，FILE-文件
     */
    private String nodeType;

    /**
     * 节点显示名（仓库为仓库名，目录/文件为自身名称）
     */
    private String name;

    /**
     * 节点路径（仓库为空串，目录/文件为相对仓库根路径）
     */
    private String path;

    private Long repositoryId;

    private String repositoryName;

    /**
     * 元素数（仅 FILE 节点有效）
     */
    private Integer elementCount;

    private List<UiElementFileNode> children = new ArrayList<>();
}

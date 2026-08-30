/**
 * @author HXN
 * @date 2026-08-30 10:00
 * @description 代码仓库拉取历史实体类
 */
package com.platform.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代码仓库拉取历史实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("code_repository_pull_log")
public class CodeRepositoryPullLog extends BaseEntity {

    /**
     * 所属仓库 ID
     */
    private Long repositoryId;

    /**
     * 拉取类型：CLONE-首次克隆，PULL-增量更新
     */
    private String pullType;

    /**
     * 拉取分支
     */
    private String branch;

    /**
     * 拉取状态：RUNNING-拉取中，SUCCESS-成功，FAILED-失败
     */
    private String status;

    /**
     * 拉取成功后的 HEAD commit ID
     */
    private String commitId;

    /**
     * 结果信息（成功为概要，失败为原因）
     */
    private String message;

    /**
     * 拉取耗时（毫秒）
     */
    private Long durationMs;
}

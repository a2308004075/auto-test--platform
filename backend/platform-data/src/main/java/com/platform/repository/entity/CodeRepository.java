/**
 * @author HXN
 * @date 2026-08-30 10:00
 * @description 测试代码仓库实体类
 */
package com.platform.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 测试代码仓库实体
 *
 * <p>authPassword 存储 AES 加密后的密文（enc: 前缀）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("code_repository")
public class CodeRepository extends BaseEntity {

    /**
     * 所属项目 ID
     */
    private Long projectId;

    /**
     * 仓库名称
     */
    private String name;

    /**
     * Git 仓库地址
     */
    private String gitUrl;

    /**
     * 拉取分支（NULL=仓库默认分支）
     */
    private String branch;

    /**
     * 仓库描述
     */
    private String description;

    /**
     * 认证用户名
     */
    private String authUsername;

    /**
     * 认证密码/Token（AES 密文）
     */
    private String authPassword;

    /**
     * 本地代码目录（相对存储根目录）
     */
    private String localPath;

    /**
     * 最近一次拉取时间
     */
    private LocalDateTime lastPullAt;

    /**
     * 最近一次拉取状态：RUNNING/SUCCESS/FAILED
     */
    private String lastPullStatus;

    /**
     * 最近一次拉取成功后的 HEAD commit ID
     */
    private String lastCommitId;
}

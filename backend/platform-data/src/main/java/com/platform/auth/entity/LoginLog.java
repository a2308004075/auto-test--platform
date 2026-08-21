package com.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体 - 记录用户登录成功/失败历史
 *
 * <p>纯日志表，不继承 BaseEntity（无 updated_at、无逻辑删除）。
 */
@Data
@TableName("login_log")
public class LoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID（登录失败且用户不存在时为 NULL） */
    private Long userId;

    /** 登录时输入的用户名 */
    private String username;

    /** 登录状态：SUCCESS / FAILED */
    private String status;

    /** 客户端 IP 地址 */
    private String ip;

    /** 完整 User-Agent */
    private String userAgent;

    /** 解析后的浏览器名称 */
    private String browser;

    /** 解析后的操作系统 */
    private String os;

    /** 附加信息（如失败原因） */
    private String message;

    /** 登录时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}

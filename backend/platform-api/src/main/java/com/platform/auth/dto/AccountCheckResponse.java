/**
 * @author HXN
 * @date 2026-08-22 15:34
 * @description 账号可用性校验响应 DTO
 */
package com.platform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 账号可用性校验响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountCheckResponse {

    /**
     * 账号是否可用
     */
    private boolean available;

    /**
     * 提示信息
     */
    private String message;
}

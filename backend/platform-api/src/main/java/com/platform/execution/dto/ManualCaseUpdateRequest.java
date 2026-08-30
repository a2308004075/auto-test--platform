/**
 * @author HXN
 * @date 2026-08-30
 * @description 手动用例更新请求 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 手动用例更新请求（支持部分更新）
 */
@Data
public class ManualCaseUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 200, message = "用例标题长度不能超过 200")
    private String title;

    private String preconditions;

    private String operationSteps;

    private String expectedResult;

    private String caseType;

    private String priority;

    private Long groupId;

    private Integer runInTestEnv;

    private Integer runInProdEnv;

    private Integer caseStatus;
}

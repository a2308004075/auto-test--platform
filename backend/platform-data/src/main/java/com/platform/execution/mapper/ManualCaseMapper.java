/**
 * @author HXN
 * @date 2026-08-30
 * @description 手动化用例数据访问接口
 */
package com.platform.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.execution.entity.ManualCase;
import org.apache.ibatis.annotations.Mapper;

/**
 * 手动化用例 Mapper
 */
@Mapper
public interface ManualCaseMapper extends BaseMapper<ManualCase> {
}
